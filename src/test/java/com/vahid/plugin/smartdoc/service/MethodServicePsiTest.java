package com.vahid.plugin.smartdoc.service;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.fixtures.*;
import org.intellij.lang.annotations.Language;

public class MethodServicePsiTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void test_given_methodWithSignature_when_askForItsKey_then_returnAsExpected() {
        @Language("JAVA") String code = """
                package test;
                public class Sample {
                    public void myMethod(int a) {
                    }
                }
                """;
        assertNotNull(myFixture);
        PsiClass psiClass = myFixture.addClass(code);
        PsiMethod psiMethod = psiClass.findMethodsByName("myMethod", false)[0];
        String result = MethodService.getMethodUniqueKey(psiMethod);
        assertEquals("test.Sample#myMethod(int)", result);
    }

    @Override
    protected void tearDown() throws Exception {
        myFixture.tearDown();
    }
}