package com.vahid.plugin.smartdoc.UI;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.event.VisibleAreaEvent;
import com.intellij.openapi.editor.event.VisibleAreaListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FeedbackManager {

    private FeedbackManager() {
    }
    private static final ConcurrentHashMap<VirtualFile, List<PsiMethod>> pendingFeedback = new ConcurrentHashMap<>();
    private static final Set<Project> initializedProjects = ConcurrentHashMap.newKeySet();

    public static void queueFeedback(Project project, PsiMethod psiMethod) {
        VirtualFile file = psiMethod.getContainingFile().getVirtualFile();
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        VirtualFile currentFile = fileEditorManager.getSelectedFiles().length > 0
                ? fileEditorManager.getSelectedFiles()[0]
                : null;

        StarRatingFeedback.discardIsRated(psiMethod.getContainingFile().getVirtualFile().getPath() + "#" + psiMethod.getName());

        // File may not be visible → queue feedback
        pendingFeedback.compute(file, (vf, list) -> {
            if (list == null) list = new ArrayList<>();
            list.add(psiMethod);
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

//            EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryListener() {
//                @Override
//                public void editorCreated(@NotNull EditorFactoryEvent event) {
//                    System.out.println("Edidor is active");
//                    Editor editor = event.getEditor();
//                    VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
//                    Project editorProject = editor.getProject();
//
//                    if (file != null && editorProject != null && editorProject.equals(project)) {
//                        editor.getScrollingModel().addVisibleAreaListener(new VisibleAreaListener() {
//                            private boolean handled = false;
//
//                            @Override
//                            public void visibleAreaChanged(@NotNull VisibleAreaEvent e) {
//                                if (!handled) {
//                                    handled = true;
//                                    handleEvent(editorProject, file); // your re-open logic
//                                }
//                            }
//                        }, project);
//                    }
//                }
//            }, project);

            System.out.println("Done with all regs");
        }

        // File is already active → show immediately
        if (file.equals(currentFile)) {
            Editor editor = fileEditorManager.getSelectedTextEditor();
            if (editor != null) {
                StarRatingFeedback.show(editor, psiMethod);
            }
        }

    }


    private static void handleEvent(@NotNull Project project, @NotNull VirtualFile selected) {
        StarRatingFeedback.dismissAllBalloons();
        System.out.println("Entered!" + selected.getName());
        if (pendingFeedback.containsKey(selected)) {
            System.out.println("Handleing..." + selected.getName());
            // In case a method can be repeated in terms of rating. Replace other similar blocks as well.
//                                List<PsiMethod> psiMethods = pendingFeedback.remove(selected);
//                                if (psiMethods != null) {
//                                    for (PsiMethod psiMethod : psiMethods) {
//                                        showFeedbackList(project, psiMethods);
//                                    }
//                                }
            List<PsiMethod> psiMethods = pendingFeedback.get(selected);
            System.out.println("values are" + pendingFeedback.get(selected).size());
            if (psiMethods != null) {
                psiMethods.removeIf(psiMethod -> StarRatingFeedback.isRated(
                        psiMethod.getContainingFile().getVirtualFile().getPath() + "#" + psiMethod.getName()));

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

    private static void showFeedbackList(Project project, List<PsiMethod> psiMethods) {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            FileEditorManager manager = FileEditorManager.getInstance(project);
            Editor editor = manager.getSelectedTextEditor();
            if (editor != null) {
                for (PsiMethod psiMethod : psiMethods) {
                    // Ignore the following two lines, in case repetitive same method rating selected
                    String methodId = psiMethod.getContainingFile().getVirtualFile().getPath() + "#" + psiMethod.getName();
                    if (!StarRatingFeedback.isRated(methodId)) {
                        StarRatingFeedback.show(editor, psiMethod);
                    }
                }
            }
        });
    }

}
