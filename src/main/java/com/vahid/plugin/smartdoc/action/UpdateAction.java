package com.vahid.plugin.smartdoc.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.vahid.plugin.smartdoc.UI.FeedbackManager;
import com.vahid.plugin.smartdoc.UI.StarRatingFeedback;
import com.vahid.plugin.smartdoc.exception.StructuredOutputMaxRetryException;
import com.vahid.plugin.smartdoc.service.MethodService;
import com.vahid.plugin.smartdoc.service.RemoteGAService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public abstract class UpdateAction extends AnAction {
    Logger logger = LoggerFactory.getLogger(UpdateAction.class);
    ThreadLocal<Stack<PsiMethod>> stackThreadLocal = ThreadLocal.withInitial(Stack::new);
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
                try {
                    rootMethod = getMethod(editor, psiFile);

                    // Adding the root method (the current one)
                    methodStack.add(rootMethod);

                    // Starting from the current method to iterate in a DFS manner
                    iterateOverMethods(rootMethod, methodStack);

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
                            //methodService.replaceMethodComment(stackMethod, remoteGAService.getMethodComment(stackMethod, firstLevelMethodCalls), e.getProject());
                        } else {
                            String methodComment = methodCommentOptional
                                    .map(PsiComment::getText)
                                    .orElseGet(() -> remoteGAService.getMethodComment(stackMethod, firstLevelMethodCalls));
                            methodService.updateMethodCommentMap(stackMethod, methodComment);
                        }
                    }
                } finally {
                    stackThreadLocal.remove();
                }
            }
            @Override
            public void onCancel() {
                logger.info("Canceled by user on thread: {}", Thread.currentThread());
            }

            @Override
            public void onSuccess() {
                    if (rootMethod == null) return;
                FeedbackManager.queueFeedback(project, rootMethod);
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
        throw new StructuredOutputMaxRetryException("Max retries (" + MAX_RETRY_COUNT + ") exceeded for: " + stackMethod);
    }

    public PsiMethod getMethod(Editor editor, PsiFile psiFile) {
        return ReadAction.compute(() -> {
            int offset = editor.getCaretModel().getOffset();
            PsiElement elementAtCaret = psiFile.findElementAt(offset);
            return PsiTreeUtil.getParentOfType(elementAtCaret, PsiMethod.class);
        });
    }

    private void iterateOverMethods(PsiMethod method, Stack<PsiMethod> methodStack) {
        List<PsiMethodCallExpression> expressions = methodService.findMethodCalls(method);
        if (expressions.isEmpty()) {
            return;
        }
        for (PsiMethodCallExpression expression : expressions) {
            AtomicReference<PsiMethod> psiMethodRef = new AtomicReference<>();
            ReadAction.run(() -> psiMethodRef.set(expression.resolveMethod()));
            PsiMethod psiMethod = psiMethodRef.get();
            methodStack.add(psiMethod);
            iterateOverMethods(psiMethod, methodStack);
        }
    }
}
