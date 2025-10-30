package com.vahid.plugin.smartdoc.service;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;

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

    @Test
    void givenJavaMethodCommentPattern_whenSendATestWithAMatch_returnOnlyMatchedComment() {
        String inputComment = """
                /**
                 * Returns a list of all Type enum values with HTTP status OK. Internally checks if input is null and handles nested checks. This is new commentmethod111.
                 */
                """;

        Optional<String> polishedMethodComment = MethodService.getMatchedComment(inputComment);

        assertThat(polishedMethodComment).isNotEmpty();
    }

    @Test
    void givenPsiMethod_whenAskForMethodUniqueKey_itReturnsTheKey() {
        PsiClass psiClass = mock(PsiClass.class);
        when(psiClass.getQualifiedName()).thenReturn("ClassA");
        PsiMethod psiMethod = mock(PsiMethod.class);
        when(psiMethod.getContainingClass()).thenReturn(psiClass);
        when(psiMethod.getName()).thenReturn("methodA01");
        PsiParameterList parameterList = mock(PsiParameterList.class);
        PsiParameter psiParameter = mock(PsiParameter.class);
        PsiType psiType = mock(PsiType.class);
        when(psiType.getCanonicalText()).thenReturn("java.lang.String");
        when(psiParameter.getType()).thenReturn(psiType);
        when(parameterList.getParameters()).thenReturn(new PsiParameter[]{psiParameter});

        when(psiMethod.getParameterList()).thenReturn(null);
        try (MockedStatic<ApplicationManager> applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class)) {
            Application application = mock(Application.class);
            applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);

            when(application.runReadAction(any(ThrowableComputable.class)))
                    .thenAnswer(invocation -> {
                        ThrowableComputable<?, ?> computable = invocation.getArgument(0);
                        return computable.compute();
                    });

            when(psiMethod.getParameterList()).thenReturn(parameterList);
            String methodUniqueKey = MethodService.getMethodUniqueKey(psiMethod);

            assertThat(methodUniqueKey).isNotEmpty();
            assertThat(methodUniqueKey).isEqualTo("ClassA#methodA01(java.lang.String)");
        }
    }
}
