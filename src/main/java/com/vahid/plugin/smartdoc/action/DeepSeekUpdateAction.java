package com.vahid.plugin.smartdoc.action;

import com.intellij.openapi.application.ApplicationManager;
import com.vahid.plugin.smartdoc.service.RemoteGAServiceOkHttp;

public class DeepSeekUpdateAction extends UpdateAction{
    protected DeepSeekUpdateAction() {
        super(ApplicationManager.getApplication().getService(RemoteGAServiceOkHttp.class));
    }
}
