/*
 * Copyright (c) 2025 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartdoc.UI;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiMethod;
import com.vahid.plugin.smartdoc.dto.FeedbackCommentDto;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

public class FeedbackManager {

    private FeedbackManager() {
    }
    private static final ConcurrentHashMap<VirtualFile, List<FeedbackCommentDto>> pendingFeedback = new ConcurrentHashMap<>();
    private static final Set<Project> initializedProjects = ConcurrentHashMap.newKeySet();
    private static final BiPredicate<FeedbackCommentDto, FeedbackCommentDto> checkIfMethodsAreEquals = (dto1, dto2) -> dto1.psiMethod().getContainingFile().getVirtualFile().getPath().equals(dto2.psiMethod().getContainingFile().getVirtualFile().getPath())
            && dto1.psiMethod().getName().equals(dto1.psiMethod().getName());

    public static void queueFeedback(Project project, FeedbackCommentDto dto) {
        PsiMethod psiMethod = dto.psiMethod();
        VirtualFile file = psiMethod.getContainingFile().getVirtualFile();
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        VirtualFile currentFile = fileEditorManager.getSelectedFiles().length > 0
                ? fileEditorManager.getSelectedFiles()[0]
                : null;

        StarRatingFeedback.discardIsRated(psiMethod.getContainingFile().getVirtualFile().getPath() + "#" + psiMethod.getName());

        pendingFeedback.compute(file, (vf, list) -> {
            if (list == null) list = new ArrayList<>();
            list.removeIf(existingMethod -> checkIfMethodsAreEquals.test(existingMethod, dto));
            System.out.println("PsiMeethod size is" + list.size());
            list.add(dto);
            return list;
        });

        // Subscribe to file switch once (per project)
        if (initializedProjects.add(project)) {
            project.getMessageBus().connect().subscribe(
                    FileEditorManagerListener.FILE_EDITOR_MANAGER,
                    new FileEditorManagerListener() {
//                        @Override
//                        public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile selected) {
//                            System.out.println("Entered from open" + selected.getName());
//                            handleEvent(source.getProject(), selected);
//                        }

                        @Override
                        public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                            VirtualFile selected = event.getNewFile();
                            if (selected != null) {
                                System.out.println("Handled it!");
                                handleEvent(event.getManager().getProject(), selected);
                            }
                        }
                    });

            System.out.println("Done with all regs");
        }

        // File is already active → show immediately
        if (file.equals(currentFile)) {
            Editor editor = fileEditorManager.getSelectedTextEditor();
            if (editor != null) {
                StarRatingFeedback.show(editor, dto);
            }
        }

    }


    private static void handleEvent(@NotNull Project project, @NotNull VirtualFile selected) {
        StarRatingFeedback.dismissAllBalloons();
        System.out.println("Entered!" + selected.getName());
        if (pendingFeedback.containsKey(selected)) {
            System.out.println("Handleing..." + selected.getName());
            List<FeedbackCommentDto> psiMethods = pendingFeedback.get(selected);
            System.out.println("values are" + pendingFeedback.get(selected).size());
            if (psiMethods != null) {
                psiMethods.removeIf(dto -> StarRatingFeedback.isRated(
                        dto.psiMethod().getContainingFile().getVirtualFile().getPath() + "#" + dto.psiMethod().getName()));

                if (psiMethods.isEmpty()) {
                    System.out.println("Removing...");
                    pendingFeedback.remove(selected);
                } else {
                    pendingFeedback.put(selected, psiMethods);
                    System.out.println("Showing....");
                    showFeedbackList(project, psiMethods);
                }
            }
        }
    }

    private static void showFeedbackList(Project project, List<FeedbackCommentDto> feedbackCommentDtos) {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            FileEditorManager manager = FileEditorManager.getInstance(project);
            Editor editor = manager.getSelectedTextEditor();
            if (editor != null) {
                for (FeedbackCommentDto feedbackCommentDto : feedbackCommentDtos) {
                    // Ignore the following two lines, in case repetitive same method rating selected
                    String methodId = feedbackCommentDto.psiMethod().getContainingFile().getVirtualFile().getPath() + "#" + feedbackCommentDto.psiMethod().getName();
                    if (!StarRatingFeedback.isRated(methodId)) {
                        StarRatingFeedback.show(editor, feedbackCommentDto);
                    }
                }
            }
        });
    }

}
