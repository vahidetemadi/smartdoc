package com.vahid.plugin.smartdoc.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

public abstract class RemoteGAService {

    private final MethodService methodService;
    protected RemoteGAService() {
        this.methodService = ApplicationManager.getApplication().getService(MethodService.class);
    }

    public abstract String getMethodComment(PsiMethod superMethod, List<PsiMethodCallExpression> psiMethodCallExpressions);

    /**
     * Engineers the prompt in order for remote call
     * @param superMethod root method to request method summary
     * @param psiMethodCallExpressions all superMethod's children methods that already have their summary
     * @return engineered prompt
     */
    public String createPrompt(PsiMethod superMethod, List<PsiMethodCallExpression> psiMethodCallExpressions) {
        return ReadAction.compute(() -> {
            String template = """
                    Produce Java-Doc style method comment for method: {0},
                    given explanations for nested method calls as follow: {1}""";
            List<String> nestedMethodCallComment = psiMethodCallExpressions.stream()
                    .filter(Objects::nonNull)
                    .map(methodCallExpression -> String.join(" with method comment:",
                            List.of(Objects.requireNonNull(methodCallExpression.getMethodExpression().getReferenceName()),
                                    methodService.getMethodComment(methodCallExpression.resolveMethod()))))
                    .toList();

            String joinedMethodCalls = String.join(" , and", nestedMethodCallComment);
            return MessageFormat.format(template, superMethod.getText(), joinedMethodCalls);
        });
    }



}
