/*
 * Copyright (c) 2025 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartdoc.action;

import com.intellij.openapi.application.ApplicationManager;
import com.vahid.plugin.smartdoc.service.RemoteGAService;
import com.vahid.plugin.smartdoc.service.RemoteGAServiceLangChainOllama;

public class OllamaUpdateAction extends UpdateAction{

    protected OllamaUpdateAction() {
        super(ApplicationManager.getApplication().getService(RemoteGAServiceLangChainOllama.class));
    }

    protected OllamaUpdateAction(RemoteGAService remoteGAService) {
        super(remoteGAService);
    }
}
