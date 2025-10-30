package com.vahid.plugin.smartdoc.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.vahid.plugin.smartdoc.UI.FeedbackManager;
import com.vahid.plugin.smartdoc.service.MethodService;
import com.vahid.plugin.smartdoc.service.RemoteGAService;
import com.vahid.plugin.smartdoc.service.RemoteGAServiceOkHttp;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

public class SmartDocActionTest extends LightJavaCodeInsightFixtureTestCase {

    public static String projectName;
    public static String modelName = "DEEPSEEK_REMOTE_CODER";

    Map<PsiMethod, CommentPairHolderDto> map = new HashMap<>();

    MethodService methodService;
    RemoteGAService remoteGAService;
    UpdateAction updateAction;
    private static final String API_KEY = System.getenv("DEEPSEEK_KEY");

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MethodService realMethodService = new MethodService();
        methodService = Mockito.spy(realMethodService);
        RemoteGAService remoteGAServiceReal = new RemoteGAServiceOkHttp();
        remoteGAService = Mockito.spy(remoteGAServiceReal);
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

        // Deleting method comments to make it fully dependent to SmartDoc
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

        Map<PsiMethod, String> psiMethodMap = new HashMap<>();

        for (PsiMethod psiMethod : psiClass.getMethods()) {
            doNothing().when(methodService).replaceMethodComment(any(), any(), any());
            doReturn(psiMethod)
                    .when(updateAction)
                    .getMethod(editor, psiFile);
            doReturn(API_KEY)
                    .when((RemoteGAServiceOkHttp) remoteGAService)
                    .getApiKey();
            try (MockedStatic<FeedbackManager> mockedStatic = Mockito.mockStatic(FeedbackManager.class)) {
                mockedStatic.when(() -> FeedbackManager.queueFeedback(any(), any())).thenAnswer(invocationOnMock -> null);
                updateAction.actionPerformed(e);
                doNothing().when(methodService).replaceMethodComment(any(PsiMethod.class), commentCaptor.capture(), any(Project.class));
                verify(methodService).replaceMethodComment(eq(psiMethod), commentCaptor.capture(), eq(project));
                psiMethodMap.put(psiMethod, commentCaptor.getValue());
            }
        }

        psiMethodMap.forEach((psiMethod, s) -> System.out.println(psiMethod.getName() + ":::" + s));
    }

    @ParameterizedTest
    @MethodSource("moduleNames")
    public void test_givenMultipleClassesProject_whenRequestForCommentGen_produceAndRecordCommentPairs(String moduleName) throws IOException{
        map.clear();
        Path testResourcePath = Paths.get(STR."src/test/resources/testClasses/\{moduleName}");
        projectName = testResourcePath.getName(testResourcePath.getNameCount() - 1).toString();

        try (Stream<Path> paths = Files.walk(testResourcePath, Integer.MAX_VALUE)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            String relPath = testResourcePath.relativize(path).toString();
                            String fileText = Files.readString(path);

                            myFixture.addFileToProject(relPath.replace(File.separatorChar, '/'), fileText);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }

        GlobalSearchScope scope = GlobalSearchScope.allScope(myFixture.getProject());
        PsiShortNamesCache cache = PsiShortNamesCache.getInstance(myFixture.getProject());

        List<PsiClass> allClasses = Arrays.stream(cache.getAllClassNames())
                .flatMap(className -> Arrays.stream(cache.getClassesByName(className, scope)))
                .toList();

        AnActionEvent e = Mockito.mock(AnActionEvent.class);
        Project project = Mockito.mock(Project.class);
        Editor editor = Mockito.mock(Editor.class);

        when(e.getProject()).thenReturn(project);
        when(e.getData(CommonDataKeys.EDITOR)).thenReturn(editor);

        int i = 0;
        int end = 10;
        for (PsiClass psiClass : allClasses) {
            if (i > end)
                break;
            PsiFile psiFile = psiClass.getContainingFile();
            when(e.getData(CommonDataKeys.PSI_FILE)).thenReturn(psiFile);

            WriteCommandAction.runWriteCommandAction(myFixture.getProject(), () -> {
                for (PsiMethod psiMethod : psiClass.getMethods()) {
                    PsiComment psiComment = psiMethod.getDocComment();
                    String methodComment = Optional.ofNullable(psiComment).map(PsiComment::getText).orElse(null);
                    if (psiComment != null)  {
                        map.computeIfAbsent(psiMethod, m -> CommentPairHolderDto.Builder().expected(methodComment).build());
                        psiComment.delete();
                    }
                }
            });

            for (PsiMethod psiMethod : psiClass.getMethods()) {
                if (i > end)
                    break;

                if (!map.containsKey(psiMethod))
                    continue;

                doNothing().when(methodService).replaceMethodComment(any(), any(), any());
                doReturn(psiMethod).when(updateAction).getMethod(editor, psiFile);
                doReturn(API_KEY).when((RemoteGAServiceOkHttp) remoteGAService).getApiKey();

                try (MockedStatic<FeedbackManager> mockedStatic = Mockito.mockStatic(FeedbackManager.class)) {
                    mockedStatic.when(() -> FeedbackManager.queueFeedback(any(), any())).thenAnswer(invocation -> null);
                    updateAction.actionPerformed(e);
                    verify(methodService).replaceMethodComment(eq(psiMethod), commentCaptor.capture(), eq(project));
                    map.computeIfPresent(psiMethod, (m, commentPairHolderDto) -> commentPairHolderDto.setActual(commentCaptor.getValue()));
                    i++;
                }
            }
        }
    }

    static List<String> moduleNames() {
        return List.of("edge", "gateway", "json", "things", "thingsearch");
    }

    @Override
    protected void tearDown() throws Exception {
        IOUtils.persistChanges(modelName, map, projectName);
        super.tearDown();
    }
}
