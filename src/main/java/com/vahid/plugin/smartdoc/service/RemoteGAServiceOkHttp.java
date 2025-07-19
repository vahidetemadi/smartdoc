package com.vahid.plugin.smartdoc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service(Service.Level.APP)
public final class RemoteGAServiceOkHttp extends RemoteGAService{
    private static final Logger logger = LoggerFactory.getLogger(RemoteGAServiceOkHttp.class);
    private String apiKey;
    private static final String API_URL = "https://api.deepseek.com/chat/completions";


    public RemoteGAServiceOkHttp() {
        super();
        this.apiKey = ApplicationManager.getApplication().getService(SmartDocState.class).DeepSeekAPIKey;
    }

    public RemoteGAServiceOkHttp(String apiKey) {
        super();
        this.apiKey = apiKey;
    }

    private String getApiKey() {
        if (Strings.isNullOrEmpty(this.apiKey)) {
            this.apiKey = ApplicationManager.getApplication().getService(SmartDocState.class).DeepSeekAPIKey;
        }
        return this.apiKey;
    }
    @Override
    public String getMethodComment(PsiMethod superMethod, List<PsiMethodCallExpression> psiMethodCallExpressions) {
        if (Strings.isNullOrEmpty(this.apiKey)
                || Strings.isNullOrEmpty(ApplicationManager.getApplication().getService(SmartDocState.class).DeepSeekAPIKey)) {
            ApplicationManager.getApplication().invokeAndWait(() -> {
                DynamicDialog dialog = new DynamicDialog("Empty API Key ERROR", "Before proceeding, please introduce API key from Setting menu.");
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
            throw new RuntimeException(e);
        }
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        String json = String.format("""
                {
                    "model": "deepseek-chat",
                    "messages": [
                        {"role": "system", "content": "Be very concise and only return method comment, in JavaDoc style (include what nested methods do all as single explain, but do not name them  or do not say method to) and not the method itself and without java tag and only comment"},
                        {"role": "user", "content": %s}
                    ],
                    "stream": false
                }
                """, formattedPrompt);
        logger.info("Here is the  request: {}", json);
        RequestBody requestBody = RequestBody.create(json, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", STR."Bearer \{getApiKey()}")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            assert response.body() != null;
            String responseBodyStr = response.body().string();
            System.out.println("Raw response: " + responseBodyStr);

            return parseResponse(responseBodyStr);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    private String parseResponse(String jsonRes) {
        try {
            System.out.println("Start extracting...");
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonRes);
            String assistantReply = rootNode.path("choices").get(0).path("message").path("content").asText();
            System.out.println(String.format("Here is the response: %s", assistantReply));
            return assistantReply;
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
