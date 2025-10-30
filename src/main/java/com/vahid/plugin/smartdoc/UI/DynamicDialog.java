/*
 * Copyright (c) 2025 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartdoc.UI;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class DynamicDialog extends DialogWrapper {
    private final String message;
    private final String title;

    public DynamicDialog(String title, String message) {
        super(true); // modal
        this.title = title;
        this.message = message;
        init();
        setTitle(title);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel();
        panel.add(new JLabel(message));
        return panel;
    }
}
