package com.vahid.plugin.smartdoc.action;

import com.intellij.openapi.application.ApplicationManager;
import com.vahid.plugin.smartdoc.service.MethodService;
import com.vahid.plugin.smartdoc.service.RemoteGAService;
import com.vahid.plugin.smartdoc.service.RemoteGAServiceOkHttp;

public class DeepSeekUpdateAction extends UpdateAction{
    protected DeepSeekUpdateAction() {
        super(ApplicationManager.getApplication().getService(RemoteGAServiceOkHttp.class));
    }

    protected DeepSeekUpdateAction(RemoteGAService remoteGAService, MethodService methodService) {
        super(remoteGAService, methodService);
    }
}
