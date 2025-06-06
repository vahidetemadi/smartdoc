package com.vahid.plugin.smartdoc.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.intellij.lang.annotations.Language;

import java.util.List;

public class MethodServicePsiVisitMethodTest extends LightJavaCodeInsightFixtureTestCase {

    private MethodService methodService;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.methodService = ApplicationManager.getApplication().getService(MethodService.class);
    }

    public void test_given_aMethodWithNestedCalls_then_visitAllOfThem() {
        @Language("JAVA") String code = """
                public class Sample {
                    public void methodA() {
                        methodB();
                    }
                    public void methodB() {
                    }
                }
                """;

        assertNotNull(myFixture);
        PsiClass psiClass = myFixture.addClass(code);
        PsiMethod psiMethod = psiClass.findMethodsByName("methodA", false)[0];
        List<PsiMethodCallExpression> psiCallExpressions = methodService.findMethodCalls(psiMethod);
        assertNotEmpty(psiCallExpressions);
    }

    @Override
    protected void tearDown() throws Exception{
        super.tearDown();
    }
}
