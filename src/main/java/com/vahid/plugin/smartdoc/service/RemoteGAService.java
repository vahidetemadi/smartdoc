package com.vahid.plugin.smartdoc.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.service.OpenAiService;
import com.vahid.plugin.smartdoc.config.SmartDocState;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

//@Service(Service.Level.APP)
public class RemoteGAService {

    //private Project project;
    private static final String MODEL = "gpt-3.5-turbo";
    private OpenAiService openAiService;
    private MethodService methodService;
    public RemoteGAService() {
        //this.project = ProjectManager.getInstance().getOpenProjects()[0];
//        this.openAiService = new OpenAiService(SmartDocState.getInstance(project).apiKey);
        //this.openAiService = new OpenAiService(ApplicationManager.getApplication().getService(SmartDocState.class).apiKey);
        this.methodService = ApplicationManager.getApplication().getService(MethodService.class);
    }

    public String getMethodComment(PsiMethod superMethod, List<PsiMethodCallExpression> psiMethodCallExpressions) {
        final String prompt = createPrompt(superMethod, psiMethodCallExpressions);
        CompletionRequest completionRequest = CompletionRequest.builder()
                
                .prompt(prompt)
                .model(MODEL)
                .echo(false)
                .maxTokens(500)
                .build();
        return openAiService.createCompletion(completionRequest).getChoices()
                .stream()
                .findFirst()
                .map(CompletionChoice::getText)
                .orElse(null);
    }
    
    public String createPrompt(PsiMethod superMethod, List<PsiMethodCallExpression> psiMethodCallExpressions) {
        String template = "Produce Java method conventional comment for method: {0}, given explanations for nested method calls as follow:\n" +
            " {1}";
        List<String> nestedMethodCallComment = psiMethodCallExpressions.stream()
                .filter(Objects::nonNull)
                .map(methodCallExpression -> String.join(" with method comment:", List.of(methodCallExpression.getMethodExpression().getReferenceName(),
                        methodService.getMethodComment(methodCallExpression.resolveMethod()))))
                .collect(Collectors.toList());

        String joinedMethodCalls = String.join(" ,and", nestedMethodCallComment);
        return MessageFormat.format(template, superMethod.getText(), joinedMethodCalls);
    }



}
