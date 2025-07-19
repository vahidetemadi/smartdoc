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
