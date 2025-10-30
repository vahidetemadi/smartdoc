/*
 * Copyright (c) 2025 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartdoc.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class RemoteGAService {

    private final MethodService methodService;
    protected RemoteGAService() {
        this.methodService = ApplicationManager.getApplication().getService(MethodService.class);
    }

    public abstract String getMethodComment(PsiMethod superMethod, List<PsiMethodCallExpression> psiMethodCallExpressions);

    /**
     * Engineers the prompt in order for making remote call
     * @param superMethod root method to request method summary
     * @param psiMethodCallExpressions all superMethod's children methods that already have their summary
     * @return engineered prompt
     */
    public String createPrompt(PsiMethod superMethod, List<PsiMethodCallExpression> psiMethodCallExpressions) {
        return ReadAction.compute(() -> {
            String rootTemplate = """
                    Produce maximum of three lines JavaDoc style method comment (including all relevant marks such as param, return or throws if required based on the context) for method: {0}""";

            Optional<String> superMethodTextOptional = Optional.ofNullable(superMethod.getText());
            String superMethodText = superMethodTextOptional.orElse(methodService.getMethodFullQualifiedName(superMethod));

            if (psiMethodCallExpressions.isEmpty()) {
                return MessageFormat.format(rootTemplate, superMethodText);
            }

            String followingTemplate = """
                    ,given explanations for nested method calls as follow: {1}""";
            List<String> nestedMethodCallComment = psiMethodCallExpressions.stream()
                    .filter(Objects::nonNull)
                    .map(methodCallExpression -> String.join(" with method comment:",
                            List.of(Objects.requireNonNull(methodCallExpression.getMethodExpression().getReferenceName()),
                                    methodService.getMethodComment(methodCallExpression.resolveMethod()))))
                    .toList();

            String joinedMethodCalls = String.join(" , and ", nestedMethodCallComment);

            return MessageFormat.format(rootTemplate + followingTemplate,
                    superMethodText,
                    joinedMethodCalls);
        });
    }



}
