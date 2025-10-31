/*
 * Copyright (c) 2025 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartdoc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.vahid.plugin.smartdoc.UI.DynamicDialog;
import com.vahid.plugin.smartdoc.dto.ReturnCommentDto;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service(Service.Level.APP)
public final class RemoteGAServiceLangChainOllama extends RemoteGAService {
    private volatile ChatLanguageModel model;

    private static final Logger logger = LoggerFactory.getLogger(RemoteGAServiceLangChainOllama.class);
    private static final String API_URL = "http://localhost:11434";

    public RemoteGAServiceLangChainOllama() {
        super();
    }

    private ChatLanguageModel getOrCreateModel() {
        if (model == null) {
            synchronized (this) {
                if (model == null) {
                    model = OllamaChatModel.builder()
                            .baseUrl(API_URL)
                            .modelName(Optional.of(System.getenv("OLLAMA_MODEL_NAME"))
                                    .orElseThrow(() -> {
                                        ApplicationManager.getApplication().invokeAndWait(() -> {
                                            DynamicDialog dialog = new DynamicDialog("EMPTY OLLAMA MODEL NAME", "Before proceeding, please set \"OLLAMA_MODEL_NAME\" env!");
                                            dialog.showAndGet();
                                        });
                                        return new ProcessCanceledException();
                                    }))
                            .temperature(0.0)
                            .timeout(Duration.ofSeconds(120))
                            .logRequests(true)
                            .build();
                }
            }
        }
        return model;
    }
    @Override
    public String getMethodComment(PsiMethod superMethod, List<PsiMethodCallExpression> psiMethodCallExpressions) {
        final String prompt = createPrompt(superMethod, psiMethodCallExpressions);
        ResponseFormat responseFormat = ResponseFormat.builder()
                .type(ResponseFormatType.JSON)
                .jsonSchema(JsonSchema.builder()
                        .name("ReturnCommentDto")
                        .rootElement(JsonObjectSchema.builder()
                                .addStringProperty("javaDocStyleComment")
                                .required("javaDocStyleComment")
                                .build())
                        .build())
                .build();

        UserMessage userMessage = UserMessage.from(prompt);
        SystemMessage systemMessage = new SystemMessage("""
            You are a Java code assistant. When given a method and its context, generate only and only Java-doc style method comment.
        """);


        ChatRequestParameters parameters = ChatRequestParameters.builder()
                .responseFormat(responseFormat)
                .build();

        ChatRequest chatRequest = ChatRequest.builder()
                .messages(userMessage, systemMessage)
                .parameters(parameters)
                .build();

        ChatResponse chatResponse = getOrCreateModel().chat(chatRequest);
        try {
            ReturnCommentDto dto = new ObjectMapper().readValue(chatResponse.aiMessage().text(), ReturnCommentDto.class);
            return dto.javaDocStyleComment();
        } catch (JsonProcessingException e) {
            logger.error("Parsing error: ", e);
            throw new ProcessCanceledException(e);
        }
    }
}
