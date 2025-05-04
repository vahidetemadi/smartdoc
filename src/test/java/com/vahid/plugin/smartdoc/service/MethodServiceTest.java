package com.vahid.plugin.smartdoc.service;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MethodServiceTest {

    @InjectMocks
    private MethodService methodService;

    @Test
    void testIsInProject_ReturnsTrue_WhenInSourceContent() {
        PsiClass psiClass = mock(PsiClass.class);
        Project project = mock(Project.class);
        PsiFile psiFile = mock(PsiFile.class);
        VirtualFile virtualFile = mock(VirtualFile.class);
        ProjectFileIndex fileIndex = mock(ProjectFileIndex.class);
        ProjectRootManager projectRootManager = mock(ProjectRootManager.class);

        when(psiClass.getProject()).thenReturn(project);
        when(psiClass.getContainingFile()).thenReturn(psiFile);
        when(psiFile.getVirtualFile()).thenReturn(virtualFile);

        try (MockedStatic<ProjectRootManager> mockedStatic = Mockito.mockStatic(ProjectRootManager.class)) {
            mockedStatic.when(() -> ProjectRootManager.getInstance(project)).thenReturn(projectRootManager);
            when(projectRootManager.getFileIndex()).thenReturn(fileIndex);
            when(fileIndex.isInSourceContent(virtualFile)).thenReturn(true);

            boolean result = methodService.isInProject(psiClass);
            assertTrue(result);
        }
    }
}
