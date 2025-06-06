package com.vahid.plugin.smartdoc.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SmartDocConfig implements Configurable {
    private JPanel jPanel;
    private JTextField jTextFieldDeepSeek;
    private JTextField jTextFieldOther;
    private final SmartDocState smartDocState;

    
    public SmartDocConfig() {
        smartDocState = ApplicationManager.getApplication().getService(SmartDocState.class);
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "SmartDoc App Settings";
    }

    @Override
    public @Nullable JComponent createComponent() {
        jPanel = new JPanel();

        jPanel.add(new JLabel("DeepSeek API Key:"));
        jTextFieldDeepSeek = new JTextField(smartDocState.DeepSeekAPIKey, 20);
        jPanel.add(jTextFieldDeepSeek);

        jPanel.add(Box.createVerticalStrut(8));

        jPanel.add(new JLabel("Other API Key:"));
        jTextFieldOther = new JTextField(smartDocState.OtherAPIKey, 20);
        jPanel.add(jTextFieldOther);

        return jPanel;
    }

    @Override
    public boolean isModified() {
        return !jTextFieldDeepSeek.getText().equals(smartDocState.DeepSeekAPIKey)
                || !jTextFieldOther.getText().equals(smartDocState.OtherAPIKey);
    }

    @Override
    public void apply() throws ConfigurationException {
        smartDocState.DeepSeekAPIKey = jTextFieldDeepSeek.getText();
        smartDocState.OtherAPIKey = jTextFieldOther.getText();
    }

    @Override
    public void reset() {
        jTextFieldDeepSeek.setText(smartDocState.DeepSeekAPIKey);
        jTextFieldOther.setText(smartDocState.OtherAPIKey);
    }
}
