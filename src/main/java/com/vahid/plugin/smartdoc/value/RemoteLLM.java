/*
 * Copyright (c) 2025 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
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
