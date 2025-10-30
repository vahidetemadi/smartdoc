/*
 * Copyright (c) 2025 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartdoc.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "SmartDocState",
        storages = {@Storage("SmartDocState.xml")}
)
@Service(Service.Level.APP)
public final class SmartDocState implements PersistentStateComponent<SmartDocState> {
    public String DeepSeekAPIKey = "";
    public String OtherAPIKey = "";

    @Override
    public @Nullable SmartDocState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull SmartDocState smartDocState) {
        XmlSerializerUtil.copyBean(smartDocState, this);
    }
}
