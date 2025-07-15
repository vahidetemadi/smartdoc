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
