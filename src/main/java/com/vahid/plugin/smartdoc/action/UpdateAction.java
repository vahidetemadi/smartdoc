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
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.vahid.plugin.smartdoc.service.MethodService;
import com.vahid.plugin.smartdoc.service.RemoteGAService;
import com.vahid.plugin.smartdoc.service.RemoteGAServiceLangChainOllama;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class UpdateAction extends AnAction {
    ThreadLocal<Stack<PsiMethod>> stackThreadLocal = ThreadLocal.withInitial(Stack::new);

    private MethodService methodService;
    private RemoteGAService remoteGAService;


    public UpdateAction() {
        this.methodService = ApplicationManager.getApplication().getService(MethodService.class);
        this.remoteGAService = ApplicationManager.getApplication().getService(RemoteGAServiceLangChainOllama.class);
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

        new Task.Backgroundable(project, "Updating Comment", true) {

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                Stack<PsiMethod> methodStack = stackThreadLocal.get();
                try {
                    PsiMethod method = getMethod(editor, psiFile);

                    // Adding the root method (the current one)
                    methodStack.add(method);

                    // Starting from the current method to iterate in a DFS manner
                    iterateOverMethods(method, methodStack);

                    // Iterate over the thread scoped stack collection and apply method update in case the method does not have a comment yet!
                    while (!methodStack.isEmpty()) {
                        PsiMethod stackMethod = methodStack.pop();
                        List<PsiMethodCallExpression> firstLevelMethodCalls = methodService.findMethodCalls(stackMethod);
                        Optional<PsiComment> methodCommentOptional = methodService.findMethodComment(stackMethod);
                        String methodComment = methodCommentOptional
                                .map(PsiComment::getText)
                                .orElseGet(() -> remoteGAService.getMethodComment(stackMethod, firstLevelMethodCalls));
                        if (methodStack.isEmpty()) {
                            methodService.replaceMethodComment(stackMethod, remoteGAService.getMethodComment(stackMethod, firstLevelMethodCalls), e.getProject());
                        } else {
                            methodService.updateMethodCommentMap(stackMethod, methodComment);
                        }
                    }
                } finally {
                    stackThreadLocal.remove();
                }
            }
        }.queue();

//        new Task.Backgroundable(project, "Updating Comment", false) {
//
//            @Override
//            public void run(@NotNull ProgressIndicator progressIndicator) {
//                ReadAction.run(() -> {
//                    int offset = editor.getCaretModel().getOffset();
//                    PsiElement elementAtCaret = psiFile.findElementAt(offset);
//                    PsiMethod method = PsiTreeUtil.getParentOfType(elementAtCaret, PsiMethod.class);
//                    Stack<PsiMethod> methodStack = stackThreadLocal.get();
//                    try {
//                        // Adding the root method (the current one)
//                        methodStack.add(method);
//                        // Starting from the current method to iterate in a DFS manner
//                        iterateOverMethods(method, methodStack);
//                        // Iterate over stack collection and apply method update in case the method does not have a comment yet!
//                        while (!methodStack.isEmpty()) {
//                            PsiMethod stackMethod = methodStack.pop();
//                            List<PsiMethodCallExpression> firstLevelMethodCalls = methodService.findMethodCalls(stackMethod);
//                            Optional<PsiComment> methodCommentOptional = methodService.findMethodComment(stackMethod);
//                            String methodComment = methodCommentOptional
//                                    .map(PsiComment::getText)
//                                    .orElseGet(() -> remoteGAService.getMethodComment(stackMethod, firstLevelMethodCalls));
//                            if (methodStack.isEmpty()) {
//                                methodService.replaceMethodComment(stackMethod, remoteGAService.getMethodComment(stackMethod, firstLevelMethodCalls), e.getProject());
//                            } else {
//                                methodService.updateMethodCommentMap(stackMethod, methodComment);
//                            }
//                        }
//                    } finally {
//                        stackThreadLocal.remove();
//                    }
//                });
//            }
//        }.queue();

    }

    private PsiMethod getMethod(Editor editor, PsiFile psiFile) {
        return ReadAction.compute(() -> {
            int offset = editor.getCaretModel().getOffset();
            PsiElement elementAtCaret = psiFile.findElementAt(offset);
            return  PsiTreeUtil.getParentOfType(elementAtCaret, PsiMethod.class);
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
