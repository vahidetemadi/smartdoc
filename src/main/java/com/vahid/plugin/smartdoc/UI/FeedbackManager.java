package com.vahid.plugin.smartdoc.UI;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class FeedbackManager {

    private FeedbackManager() {
    }
    private static final ConcurrentHashMap<VirtualFile, List<PsiMethod>> pendingFeedback = new ConcurrentHashMap<>();

    public static void queueFeedback(Project project, PsiMethod psiMethod) {
        VirtualFile file = psiMethod.getContainingFile().getVirtualFile();
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        VirtualFile currentFile = fileEditorManager.getSelectedFiles().length > 0
                ? fileEditorManager.getSelectedFiles()[0]
                : null;

        // Case 1: File is already active → show immediately
        if (file.equals(currentFile)) {
            Editor editor = fileEditorManager.getSelectedTextEditor();
            if (editor != null) {
                StarRatingFeedback.show(editor, psiMethod);
                return;
            }
        }

        // Case 2: File not visible → queue feedback
        pendingFeedback.compute(file, (vf, list) -> {
            if (list == null) list = new ArrayList<>();
            list.add(psiMethod);
            return list;
        });

        // Subscribe to file switch once (per project)
        project.getMessageBus().connect().subscribe(
                FileEditorManagerListener.FILE_EDITOR_MANAGER,
                new FileEditorManagerListener() {
                    @Override
                    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                        VirtualFile selected = event.getNewFile();
                        if (selected != null && pendingFeedback.containsKey(selected)) {
                            List<PsiMethod> psiMethods = pendingFeedback.remove(selected);
                            if (psiMethods != null) {
                                showFeedbackList(project, psiMethods);
                            }
                        }
                    }
                });
    }

    private static void showFeedbackList(Project project, List<PsiMethod> psiMethods) {
        ApplicationManager.getApplication().invokeLater(() -> {
            FileEditorManager manager = FileEditorManager.getInstance(project);
            Editor editor = manager.getSelectedTextEditor();
            if (editor != null) {
                for (PsiMethod psiMethod : psiMethods) {
                    StarRatingFeedback.show(editor, psiMethod);
                }
            }
        });
    }
}
