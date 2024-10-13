package com.vahid.plugin.smartdoc.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiTreeUtil;
import com.vahid.plugin.smartdoc.service.MethodService;
import com.vahid.plugin.smartdoc.service.RemoteGAService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class UpdateAction extends AnAction {
    private Stack<PsiMethod> methodStack = new Stack<>();

    private MethodService methodService;
    private RemoteGAService remoteGAService;

    public UpdateAction() {
        this.methodService = ApplicationManager.getApplication().getService(MethodService.class);
        this.remoteGAService = ApplicationManager.getApplication().getService(RemoteGAService.class);
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

        int offset = editor.getCaretModel().getOffset();
        PsiElement elementAtCaret = psiFile.findElementAt(offset);
        PsiMethod method = PsiTreeUtil.getParentOfType(elementAtCaret, PsiMethod.class);
        List<String> comments = new ArrayList<>();
        if (method != null) {
            PsiComment methodComment = methodService.findMethodComment(method);
            if (methodComment != null) {
                comments.add(methodComment.getText());
            }
        }

        methodStack.add(method);
        // Starting from the current method to iterate in a DFS manner
        iterateOverMethods(method);

        // Iterate over stack collection and apply method update in case the method does not have a comment yet!
//        for (PsiMethod stackMethod : methodStack) {
          while (!methodStack.isEmpty()) {
            PsiMethod stackMethod = methodStack.pop();
            List<PsiMethodCallExpression> firstLevelMethodCalls = methodService.findMethodCalls(stackMethod);
            String methodComment = remoteGAService.getMethodComment(stackMethod, firstLevelMethodCalls);
            methodService.replaceMethodComment(stackMethod, methodComment, e.getProject());
        }
    }

    private void iterateOverMethods(PsiMethod method) {
        List<PsiMethodCallExpression> expressions = methodService.findMethodCalls(method);
        if (expressions.isEmpty()) {
            return;
        }
        for (PsiMethodCallExpression expression : expressions) {
            methodStack.add(expression.resolveMethod());
            iterateOverMethods(expression.resolveMethod());
        }
    }
}
