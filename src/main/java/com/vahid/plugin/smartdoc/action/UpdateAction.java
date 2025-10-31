/*
 * Copyright (c) 2025 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartdoc.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.vahid.plugin.smartdoc.UI.DynamicDialog;
import com.vahid.plugin.smartdoc.UI.FeedbackManager;
import com.vahid.plugin.smartdoc.dto.FeedbackCommentDto;
import com.vahid.plugin.smartdoc.exception.StructuredOutputMaxRetryException;
import com.vahid.plugin.smartdoc.service.MethodService;
import com.vahid.plugin.smartdoc.service.RemoteGAService;
import com.vahid.plugin.smartdoc.value.RemoteLLM;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public abstract class UpdateAction extends AnAction implements DumbAware {
    Logger logger = LoggerFactory.getLogger(UpdateAction.class);
    ThreadLocal<Stack<PsiMethod>> stackThreadLocal = ThreadLocal.withInitial(Stack::new);
    ThreadLocal<Set<PsiMethod>> setThreadLocal = ThreadLocal.withInitial(HashSet::new);
    private static final ScopedValue<Integer> RETRY_COUNT = ScopedValue.newInstance();
    private static final Integer MAX_RETRY_COUNT = 3;
    private final MethodService methodService;
    private final RemoteGAService remoteGAService;

    protected UpdateAction(RemoteGAService remoteGAService) {
        this.methodService = ApplicationManager.getApplication().getService(MethodService.class);
        this.remoteGAService = remoteGAService;
    }

    protected UpdateAction(RemoteGAService remoteGAService, MethodService methodService) {
        this.methodService = methodService;
        this.remoteGAService = remoteGAService;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(false);

        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (project == null || editor == null || psiFile == null) return;

        int offset = editor.getCaretModel().getOffset();
        PsiElement element = psiFile.findElementAt(offset);

        PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
        if (method != null) {
            e.getPresentation().setEnabledAndVisible(true);
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        if (project == null || editor == null) {
            return;
        }

        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (psiFile == null) {
            return;
        }

        new Task.Backgroundable(project, "Updating comment", true) {
            private PsiMethod rootMethod;
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                Stack<PsiMethod> methodStack = stackThreadLocal.get();
                Set<PsiMethod> methodSet = setThreadLocal.get();
                try {
                    rootMethod = getMethod(editor, psiFile);

                    // Adding the root method (the current one)
                    methodStack.add(rootMethod);

                    // Starting from the current method to iterate in a DFS manner
                    iterateOverMethods(rootMethod, methodStack, methodSet);

                    // Iterate over the thread scoped stack collection and apply method update in case the method does not have a comment yet!
                    while (!methodStack.isEmpty()) {
                        PsiMethod stackMethod = methodStack.pop();
                        List<PsiMethodCallExpression> firstLevelMethodCalls = methodService.findMethodCalls(stackMethod);
                        Optional<PsiComment> methodCommentOptional = methodService.findMethodComment(stackMethod);
                        if (methodStack.isEmpty()) {
                            ScopedValue.where(RETRY_COUNT, 0)
                                            .run(() -> {
                                                try {
                                                    String comment = getMethodCommentWithRetry(stackMethod, firstLevelMethodCalls);
                                                    methodService.replaceMethodComment(stackMethod, comment, e.getProject());
                                                } catch (Exception ex) {
                                                    throw new RuntimeException(ex);
                                                }
                                            });
                        } else {
                            String methodComment = methodCommentOptional
                                    .map(PsiComment::getText)
                                    .orElseGet(() -> remoteGAService.getMethodComment(stackMethod, firstLevelMethodCalls));
                            methodService.updateMethodCommentMap(stackMethod, methodComment);
                        }
                    }
                } catch (ProcessCanceledException e) {
                    logger.error("Task canceled by the user or unexpected event");
                    throw e;
                }
                finally {
                    stackThreadLocal.remove();
                    setThreadLocal.remove();
                }
            }
            @Override
            public void onCancel() {
                logger.info("Canceled by user on thread: {}", Thread.currentThread());
            }

            @Override
            public void onSuccess() {
                if (rootMethod == null) return;
                FeedbackManager.queueFeedback(project, new FeedbackCommentDto(RemoteLLM.getLLM(UpdateAction.this),
                         rootMethod));
            }
        }.queue();
    }

    private String getMethodCommentWithRetry(PsiMethod stackMethod, List<PsiMethodCallExpression> firstLevelMethodCalls) throws Exception {
        int attempt = RETRY_COUNT.get();
        while (attempt < MAX_RETRY_COUNT) {
            String newComment = remoteGAService.getMethodComment(stackMethod, firstLevelMethodCalls);
            Optional<String> extractedComment =  MethodService.getMatchedComment(newComment);
            if (extractedComment.isPresent()) {
                return extractedComment.get();
            }
            attempt++;
        }
        ApplicationManager.getApplication().invokeAndWait(() -> {
            DynamicDialog dialog = new DynamicDialog("Failed Re-Try Calls", "Reached max try count. Please initiate another request!");
            dialog.showAndGet();
        });
        throw new StructuredOutputMaxRetryException("Max retries (" + MAX_RETRY_COUNT + ") exceeded for: " + stackMethod);
    }

    public PsiMethod getMethod(Editor editor, PsiFile psiFile) {
        return ReadAction.compute(() -> {
            int offset = editor.getCaretModel().getOffset();
            PsiElement elementAtCaret = psiFile.findElementAt(offset);
            return PsiTreeUtil.getParentOfType(elementAtCaret, PsiMethod.class);
        });
    }

    private void iterateOverMethods(PsiMethod method, Stack<PsiMethod> methodStack, Set<PsiMethod> visited) {
        List<PsiMethodCallExpression> expressions = methodService.findMethodCalls(method);
        if (expressions.isEmpty()) {
            return;
        }
        for (PsiMethodCallExpression expression : expressions) {
            AtomicReference<PsiMethod> psiMethodRef = new AtomicReference<>();
            ReadAction.run(() -> psiMethodRef.set(expression.resolveMethod()));
            PsiMethod psiMethod = psiMethodRef.get();
            methodStack.add(psiMethod);
            iterateOverMethods(psiMethod, methodStack, visited);
        }
    }
}