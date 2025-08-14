package com.vahid.plugin.smartdoc.service;

import com.intellij.openapi.application.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service(Service.Level.APP)
public final class MethodService {

    private static final Logger logger = LoggerFactory.getLogger(MethodService.class);
    private static final Map<String, String> methodComments = new ConcurrentHashMap<>();
    private static final Pattern JAVADOC_METHOD_PATTERN = Pattern.compile(
            "/\\*\\*\\s*" +                                      // /** start
                    "(?:\\*\\s*.*\\s*)*" +                               // optional description lines
                    "(?:\\*\\s*@param\\s+\\w+\\s+.*\\s*)*" +             // zero or more @param
                    "(?:\\*\\s*@return\\s+.*\\s*)?" +                    // optional @return
                    "(?:\\*\\s*@(throws|exception)\\s+\\w+\\s+.*\\s*)*" + // optional @throws or @exception
                    "\\*/", Pattern.DOTALL);

    public List<PsiMethodCallExpression> findMethodCalls(PsiMethod method) {
        List<PsiMethodCallExpression> methodCalls = new ArrayList<>();
        ReadAction.run(() -> method.accept(new JavaRecursiveElementVisitor() {
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
        }));
        return methodCalls;
    }

    public boolean isInProject(PsiClass psiClass) {
        Project project = psiClass.getProject();
        VirtualFile classFile = psiClass.getContainingFile().getVirtualFile();
        return classFile != null && ProjectRootManager.getInstance(project).getFileIndex().isInSourceContent(classFile);
    }

    public Optional<PsiComment> findMethodComment(PsiMethod method) {
        return ReadAction.compute(() -> {
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
        });
    }

    public String getMethodComment(PsiMethod psiMethod) {
        return methodComments.getOrDefault(getMethodUniqueKey(psiMethod), psiMethod.getText());
    }

    public void replaceMethodComment(PsiMethod method, String newCommentText, Project project) {
        // Read PSI safely in a background thread
        Optional<PsiComment> oldCommentOptional = findMethodComment(method);

        // Switch to UI Thread for PSI modification
        ApplicationManager.getApplication().invokeLater(() -> {
            WriteAction.run(() -> WriteCommandAction.runWriteCommandAction(project, () -> {
                PsiDocumentManager.getInstance(project).commitAllDocuments();

                PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
                PsiComment newComment = elementFactory.createCommentFromText(newCommentText, null);

                oldCommentOptional.ifPresentOrElse(
                        oldComment -> oldComment.replace(newComment),
                        () -> method.getParent().addBefore(newComment, method)
                );
            }));
        }, ModalityState.defaultModalityState());
    }

//    public void replaceMethodComment(PsiMethod method, String newCommentText, Project project) {
//        ApplicationManager.getApplication().executeOnPooledThread(() -> { // Run in background thread
//            PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
//
//            Optional<PsiComment> oldCommentOptional = ReadAction.compute(() -> findMethodComment(method)); // Read safely
//
//            ApplicationManager.getApplication().invokeLater(() -> { // Switch to EDT for write action
//                WriteAction.run(() -> { // Ensure writing is done correctly
//                    WriteCommandAction.runWriteCommandAction(project, () -> {
//                        PsiDocumentManager.getInstance(project).commitAllDocuments(); // Ensure PSI and document are synced
//
//                        PsiComment newComment = elementFactory.createCommentFromText(newCommentText, null);
//                        oldCommentOptional.ifPresentOrElse(
//                                oldComment -> oldComment.replace(newComment),
//                                () -> method.getParent().addBefore(newComment, method)
//                        );
//                    });
//                });
//            }, ModalityState.defaultModalityState());
//        });
//    }



//    public void replaceMethodComment(PsiMethod method, String newCommentText, Project project) {
//        ApplicationManager.getApplication().invokeLater(() -> {
//            WriteAction.run(() -> {
//                PsiDocumentManager.getInstance(project).commitAllDocuments();
//                WriteCommandAction.runWriteCommandAction(project, () -> {
//                    PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
//                    PsiComment newComment = elementFactory.createCommentFromText(newCommentText, null);
//
//                    Optional<PsiComment> oldCommentOptional = findMethodComment(method);
//                    oldCommentOptional.ifPresentOrElse(
//                            oldComment -> oldComment.replace(newComment),
//                            () -> method.getParent().addBefore(newComment, method));
//                });
//            });
//        }, ModalityState.defaultModalityState());
//    }



//    public void replaceMethodComment(PsiMethod method, String newCommentText, Project project) {
//        ApplicationManager.getApplication().invokeLater(() -> WriteCommandAction.runWriteCommandAction(project, () -> {
//            PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
//            PsiComment newComment = elementFactory.createCommentFromText(newCommentText, null);
//
//            Optional<PsiComment> oldCommentOptional = findMethodComment(method);
//            oldCommentOptional.ifPresentOrElse(
//                    oldComment -> oldComment.replace(newComment),
//                    () -> method.getParent().addBefore(newComment, method));
//        }), ModalityState.defaultModalityState());
//    }

    public void updateMethodCommentMap(PsiMethod stackMethod, String methodComment) {
        methodComments.computeIfAbsent(getMethodUniqueKey(stackMethod), k -> methodComment);
    }

//    public static String getMethodUniqueKey(PsiMethod psiMethod) {
//        return ReadAction.compute(() -> {
//            PsiClass psiClass = psiMethod.getContainingClass();
//            String qualifiedClassName = psiClass != null ? psiClass.getQualifiedName() : "";
//            return qualifiedClassName + "#" + psiMethod.getSignature(PsiSubstitutor.UNKNOWN);
//        });
//    }

    public static String getMethodUniqueKey(PsiMethod psiMethod) {
        return ReadAction.compute(() -> {
            PsiClass psiClass = psiMethod.getContainingClass();
            String qualifiedClassName = psiClass != null ? psiClass.getQualifiedName() : "";
            String methodName = psiMethod.getName();
            String parameterTypes = Arrays.stream(psiMethod.getParameterList().getParameters())
                    .map(p -> p.getType().getCanonicalText())
                    .collect(Collectors.joining(", "));
            return String.format("%s#%s(%s)", qualifiedClassName, methodName, parameterTypes);
        });
    }

    @Nullable
    public String getMethodFullQualifiedName(@NotNull PsiMethod psiMethod) {
        PsiClass containingClass = psiMethod.getContainingClass();
        if (containingClass == null || containingClass.getQualifiedName() == null) {
            return null; // Cannot determine
        }

        String classFqn = containingClass.getQualifiedName();
        String kind;

        if (containingClass.isEnum()) {
            kind = "enum";
        } else if (containingClass.isRecord()) {
            kind = "record";
        } else if (containingClass.isInterface()) {
            kind = "interface";
        } else if (containingClass.isAnnotationType()) {
            kind = "annotation";
        } else {
            kind = "class";
        }

        return String.format("%s() from %s %s", psiMethod.getName(), kind, classFqn);
    }

    public static boolean matchesJavaDocFormat(String comment) {
        return JAVADOC_METHOD_PATTERN.matcher(comment.trim()).matches();
    }

    public static Optional<String> getMatchedComment(String comment) {
         Matcher matcher = JAVADOC_METHOD_PATTERN.matcher(comment);
         if (matcher.find()) {
             return Optional.of(matcher.group());
         } else {
             return Optional.empty();
         }
    }
}
