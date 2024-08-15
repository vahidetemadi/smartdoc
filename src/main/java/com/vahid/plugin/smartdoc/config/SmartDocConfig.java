package com.vahid.plugin.smartdoc.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SmartDocConfig implements Configurable {
    private JPanel jPanel;
    private JTextField jTextField;

    private Project project;


    private SmartDocState smartDocState;

    public SmartDocConfig() {
        //this.project = ProjectManager.getInstance().getOpenProjects()[0];
        smartDocState = ApplicationManager.getApplication().getService(SmartDocState.class);
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "SmardDoc App Settings";
    }

    @Override
    public @Nullable JComponent createComponent() {
        jPanel = new JPanel();
//        jTextField = new JTextField(SmartDocState.getInstance(project).apiKey, 20);
        jTextField = new JTextField(smartDocState.apiKey, 20);
        jPanel.add(new JLabel("API Key:"));
        jPanel.add(jTextField);
        return jPanel;
    }

    @Override
    public boolean isModified() {
//        return !jTextField.getText().equals(SmartDocState.getInstance(project).apiKey);
        return !jTextField.getText().equals(smartDocState.apiKey);
    }

    @Override
    public void apply() throws ConfigurationException {
//        SmartDocState.getInstance(project).apiKey = jTextField.getText();
        smartDocState.apiKey = jTextField.getText();
    }

    @Override
    public void reset() {
//        jTextField.setText(SmartDocState.getInstance(project).apiKey);
        jTextField.setText(smartDocState.apiKey);
    }
}
