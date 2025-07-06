package com.vahid.plugin.smartdoc.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.testFramework.TestApplicationManager;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.vahid.plugin.smartdoc.UI.FeedbackManager;
import com.vahid.plugin.smartdoc.service.MethodService;
import com.vahid.plugin.smartdoc.service.RemoteGAService;
import com.vahid.plugin.smartdoc.service.RemoteGAServiceOkHttp;
import org.intellij.lang.annotations.Language;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

public class SmartDocActionTest extends LightJavaCodeInsightFixtureTestCase {
    MethodService methodService;
    RemoteGAService remoteGAService;
    UpdateAction updateAction;
    private static final String API_KEY = "sk-b1565f080a1748bdb5b1453235a12019";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MethodService realMethodService = new MethodService();
        methodService = Mockito.spy(realMethodService);
        remoteGAService = new RemoteGAServiceOkHttp(API_KEY);
        UpdateAction realUpdateAction = new DeepSeekUpdateAction(remoteGAService, methodService);
        updateAction = Mockito.spy(realUpdateAction);
    }

    @Captor
    ArgumentCaptor<String> commentCaptor = ArgumentCaptor.forClass(String.class);
    public void test_givenAMethodWithBodyAndNestedCalls_thenReturnEventualGeneratedComment() {

        @Language("JAVA") String code = """
                public class Sample {
                    public void methodA() {
                        System.out.println("MethodA");
                        methodB();
                        methodC();
                    }
                    
                    /**
                     * Prints "MethodB" to the console, and compute a multiplication.
                     */
                    public void methodB() {
                        System.out.println("MethodB");
                    }
                    
                    public int methodC() {
                        final int a = 10;
                        final int b = 12;
                        return a + b;
                    }
                }
                """;

        AnActionEvent e = Mockito.mock(AnActionEvent.class);
        Project project = Mockito.mock(Project.class);
        Editor editor = Mockito.mock(Editor.class);
        PsiClass psiClass = myFixture.addClass(code);
        PsiFile psiFile = psiClass.getContainingFile();

        WriteCommandAction.runWriteCommandAction(myFixture.getProject(), () -> {
            for (PsiMethod method : psiClass.getMethods()) {
                PsiComment docComment = method.getDocComment();
                if (docComment != null) {
                    docComment.delete();
                }
            }
        });

        when(e.getProject()).thenReturn(project);
        when(e.getData(CommonDataKeys.EDITOR)).thenReturn(editor);
        when(e.getData(CommonDataKeys.PSI_FILE)).thenReturn(psiFile);

        Map<PsiMethod, String> map = new HashMap<>();
        for (PsiMethod psiMethod : psiClass.getMethods()) {
            doNothing().when(methodService).replaceMethodComment(any(), any(), any());
            doReturn(psiMethod)
                    .when(updateAction)
                    .getMethod(editor, psiFile);
            try (MockedStatic<FeedbackManager> mockedStatic = Mockito.mockStatic(FeedbackManager.class)) {
                mockedStatic.when(() -> FeedbackManager.queueFeedback(any(), any())).thenAnswer(invocationOnMock -> null);
                updateAction.actionPerformed(e);
                doNothing().when(methodService).replaceMethodComment(any(PsiMethod.class), commentCaptor.capture(), any(Project.class));
                verify(methodService).replaceMethodComment(eq(psiMethod), commentCaptor.capture(), eq(project));
                map.put(psiMethod, commentCaptor.getValue());
            }
        }

        map.forEach((psiMethod, s) -> System.out.println(psiMethod.getName() + ":::" + s));
    }
}
