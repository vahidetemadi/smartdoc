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
