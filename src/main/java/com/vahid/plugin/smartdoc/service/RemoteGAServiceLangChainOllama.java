package com.vahid.plugin.smartdoc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.vahid.plugin.smartdoc.config.SmartDocState;
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

@Service(Service.Level.APP)
public final class RemoteGAServiceLangChainOllama extends RemoteGAService {
    private final ChatLanguageModel model;

    private static final Logger log = LoggerFactory.getLogger(RemoteGAServiceLangChainOllama.class);
    private final String apiKey;
    private static final String API_URL = "http://localhost:11434";

    public RemoteGAServiceLangChainOllama() {
        super();
        this.apiKey = ApplicationManager.getApplication().getService(SmartDocState.class).apiKey;
        this.model = OllamaChatModel.builder()
                .baseUrl(API_URL)
                .modelName("deepseek-r1:1.5b")
                .temperature(0.0)
                .timeout(Duration.ofSeconds(120))
                .logRequests(true)
                .build();
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
            You are a Java code assistant. When given a method and its context, generate only and only a concise method comment.
        """);


        ChatRequestParameters parameters = ChatRequestParameters.builder()
                .responseFormat(responseFormat)
                .build();

        ChatRequest chatRequest = ChatRequest.builder()
                .messages(userMessage, systemMessage)
                .parameters(parameters)
                .build();

        ChatResponse chatResponse = model.chat(chatRequest);
        try {
            ReturnCommentDto dto = new ObjectMapper().readValue(chatResponse.aiMessage().text(), ReturnCommentDto.class);
            return dto.javaDocStyleComment();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Something went wrong ");
        }
    }
}
