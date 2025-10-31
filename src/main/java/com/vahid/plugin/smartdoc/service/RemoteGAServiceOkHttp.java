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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.vahid.plugin.smartdoc.UI.DynamicDialog;
import com.vahid.plugin.smartdoc.config.SmartDocState;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service(Service.Level.APP)
public final class RemoteGAServiceOkHttp extends RemoteGAService{
    private static final Logger logger = LoggerFactory.getLogger(RemoteGAServiceOkHttp.class);
    private static final String API_URL = "https://api.deepseek.com/chat/completions";


    public RemoteGAServiceOkHttp() {
        super();
    }

    public String getApiKey() {
        return ApplicationManager.getApplication().getService(SmartDocState.class).DeepSeekAPIKey;
    }
    @Override
    public String getMethodComment(PsiMethod superMethod, List<PsiMethodCallExpression> psiMethodCallExpressions) {

        if (Strings.isNullOrEmpty(getApiKey())) {
            ApplicationManager.getApplication().invokeAndWait(() -> {
                DynamicDialog dialog = new DynamicDialog("Empty API Key ERROR", "Before proceeding, please introduce API key from File -> Settings -> SmartDoc App Settings menu, and try again!");
                dialog.showAndGet();
            });

            throw new ProcessCanceledException();
        }

        final String prompt = createPrompt(superMethod, psiMethodCallExpressions);
        ObjectMapper objectMapper = new ObjectMapper();
        String formattedPrompt;
        try {
            formattedPrompt = objectMapper.writeValueAsString(prompt);
        } catch (JsonProcessingException e) {
            throw new ProcessCanceledException(e);
        }
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        String json = String.format("""
                {
                    "model": "%s",
                    "messages": [
                        {"role": "system", "content": "Be very concise and only return method comment in JavaDoc style including the opening and closing comment tag, given following explanation of nested method, if exist"},
                        {"role": "user", "content": %s}
                    ],
                    "stream": false
                }
                """,
                Optional.ofNullable(System.getenv("DEEPSEEK_MODEL_NAME"))
                        .orElse("deepseek-coder"),
                formattedPrompt);
        RequestBody requestBody = RequestBody.create(json, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", STR."Bearer \{getApiKey()}")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code: " + response.code());
            }

            assert response.body() != null;
            String responseBodyStr = response.body().string();
            logger.info("Here is raw res from deepseek: {}", responseBodyStr);

            return parseResponse(responseBodyStr);
        } catch (IOException e) {
            logger.error("Error occurred when making call to remote DeepSeek: {}", e.getMessage());
            ApplicationManager.getApplication().invokeAndWait(() -> {
                DynamicDialog dialog = new DynamicDialog("Failed Remote LLM Call", "Check info of your remote LLM API server (for more details, seethe logs)!");
                dialog.showAndGet();
            });
            throw new ProcessCanceledException();
        }
    }

    private String parseResponse(String jsonRes) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonRes);
            return rootNode.path("choices").get(0).path("message").path("content").asText();
        } catch (JsonProcessingException e) {
            throw new ProcessCanceledException(e);
        }
    }
}
