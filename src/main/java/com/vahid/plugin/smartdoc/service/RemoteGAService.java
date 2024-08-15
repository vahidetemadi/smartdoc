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
import java.util.List;
import java.util.stream.Collectors;

@Service(Service.Level.APP)
public final class RemoteGAService {

    //private Project project;
    private static final String MODEL = "babbage-002";
    private OpenAiService openAiService;
    private MethodService methodService;
    public RemoteGAService() {
        //this.project = ProjectManager.getInstance().getOpenProjects()[0];
//        this.openAiService = new OpenAiService(SmartDocState.getInstance(project).apiKey);
        this.openAiService = new OpenAiService(ApplicationManager.getApplication().getService(SmartDocState.class).apiKey);
        this.methodService = ApplicationManager.getApplication().getService(MethodService.class);
    }

    public String getMethodComment(PsiMethod superMethod, List<PsiMethodCallExpression> psiMethodCallExpressions) {
        CompletionRequest completionRequest = CompletionRequest.builder()
                .prompt(createPrompt(superMethod, psiMethodCallExpressions))
                .echo(true)
                .build();
        return openAiService.createCompletion(completionRequest).getChoices()
                .stream()
                .findFirst()
                .map(CompletionChoice::getText)
                .orElse(null);
    }

    public String createPrompt(PsiMethod superMethod, List<PsiMethodCallExpression> psiMethodCallExpressions) {
        String template = "Produce comment for method {0} which inside it are method calls and their explanation are as" +
            " {1}";
        List<String> nestedMethodCallComment = psiMethodCallExpressions.stream()
                .map(methodCallExpression -> String.join(":", List.of(methodCallExpression.getMethodExpression().getText(),
                        methodService.findMethodComment(methodCallExpression.resolveMethod()).getText())))
                .collect(Collectors.toList());

        String joinedMethodCalls = String.join(",", nestedMethodCallComment);
        return MessageFormat.format(template, superMethod.getBody(), joinedMethodCalls);
    }



}
