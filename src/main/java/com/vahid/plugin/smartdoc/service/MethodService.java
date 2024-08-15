package com.vahid.plugin.smartdoc.service;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

@Service(Service.Level.APP)
public final class MethodService {
    public List<PsiMethodCallExpression> findMethodCalls(PsiMethod method) {
        List<PsiMethodCallExpression> methodCalls = new ArrayList<>();
        method.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);
                methodCalls.add(expression);
            }
        });
        return methodCalls;
    }

    public PsiComment findMethodComment(PsiMethod method) {
        // Check for a PsiDocComment directly attached to the method
        PsiDocComment docComment = method.getDocComment();
        if (docComment != null) {
            return docComment;
        }

        // Look for other comments (e.g., single line or block comments) preceding the method
        PsiElement element = method.getPrevSibling();
        while (element != null) {
            if (element instanceof PsiComment) {
                return (PsiComment) element;
            }
            // Skip whitespace and move to the previous sibling
            if (!(element instanceof PsiWhiteSpace)) {
                break;
            }
            element = element.getPrevSibling();
        }

        return null;
    }

    public void replaceMethodComment(PsiMethod method, String newCommentText, Project project) {
        SwingUtilities.invokeLater(() -> {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
                PsiComment newComment = elementFactory.createCommentFromText(newCommentText, null);

                PsiComment oldComment = findMethodComment(method);
                if (oldComment != null) {
                    oldComment.replace(newComment);
                } else {
                    // If there's no existing comment, add the new comment before the method
                    method.getParent().addBefore(newComment, method);
                }
            });
        });
    }
}
