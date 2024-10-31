package com.vahid.plugin.smartdoc.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service(Service.Level.APP)
public final class MethodService {
    private static final Map<String, String> methodComments = new ConcurrentHashMap<>();

    public List<PsiMethodCallExpression> findMethodCalls(PsiMethod method) {
        List<PsiMethodCallExpression> methodCalls = new ArrayList<>();
        method.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);
                PsiMethod calledMethod = expression.resolveMethod();
                if (calledMethod != null) {
                    PsiClass calledClass = calledMethod.getContainingClass();
                    if (calledClass != null && isInProject(calledClass)) {
                        methodCalls.add(expression);
                    }
                }
            }
        });
        return methodCalls;
    }

    private boolean isInProject(PsiClass psiClass) {
        Project project = psiClass.getProject();
        VirtualFile classFile = psiClass.getContainingFile().getVirtualFile();
        return classFile != null && ProjectRootManager.getInstance(project).getFileIndex().isInSourceContent(classFile);
    }

    public Optional<PsiComment> findMethodComment(PsiMethod method) {
        // Check for a PsiDocComment directly attached to the method
        PsiDocComment docComment = method.getDocComment();
        if (docComment != null) {
            return Optional.of(docComment);
        }

        // Look for other comments (e.g., single line or block comments) preceding the method
        PsiElement element = method.getPrevSibling();
        while (element != null) {
            if (element instanceof PsiComment) {
                return Optional.of((PsiComment) element);
            }
            // Skip whitespace and move to the previous sibling
            if (!(element instanceof PsiWhiteSpace)) {
                break;
            }
            element = element.getPrevSibling();
        }

        return Optional.empty();
    }

    public String getMethodComment(PsiMethod psiMethod) {
        return methodComments.getOrDefault(getMethodUniqueKey(psiMethod), psiMethod.getText());
    }

    public void replaceMethodComment(PsiMethod method, String newCommentText, Project project) {
        ApplicationManager.getApplication().invokeLater(() -> WriteCommandAction.runWriteCommandAction(project, () -> {
            PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
            PsiComment newComment = elementFactory.createCommentFromText(newCommentText, null);

            Optional<PsiComment> oldCommentOptional = findMethodComment(method);
            oldCommentOptional.ifPresentOrElse(
                    oldComment -> oldComment.replace(newComment),
                    () -> method.getParent().addBefore(newComment, method));
        }), ModalityState.defaultModalityState());
    }

    public void updateMethodCommentMap(PsiMethod stackMethod, String methodComment) {
        methodComments.computeIfAbsent(getMethodUniqueKey(stackMethod), k -> methodComment);
    }

    public static String getMethodUniqueKey(PsiMethod psiMethod) {
        PsiClass psiClass = psiMethod.getContainingClass();
        String qualifiedClassName = psiClass != null ? psiClass.getQualifiedName() : "";
        return qualifiedClassName + "#" + psiMethod.getSignature(PsiSubstitutor.EMPTY);
    }
}
