package com.vahid.plugin.smartdoc.value;

import com.vahid.plugin.smartdoc.action.DeepSeekUpdateAction;
import com.vahid.plugin.smartdoc.action.OllamaUpdateAction;
import com.vahid.plugin.smartdoc.action.UpdateAction;

public enum RemoteLLM {
    DEEP_SEEK,
    OLLAMA;

    public static RemoteLLM getLLM(UpdateAction updateAction) {
        switch (updateAction) {
            case DeepSeekUpdateAction deepSeekUpdateAction -> {
                return DEEP_SEEK;
            }
            case OllamaUpdateAction ollamaUpdateAction -> {
                return OLLAMA;
            }
            default -> {
                return DEEP_SEEK;
            }
        }
    }
}
