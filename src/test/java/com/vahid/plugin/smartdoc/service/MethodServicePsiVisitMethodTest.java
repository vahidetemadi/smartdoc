/*
 * Copyright (c) 2025 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
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
                        methodC();
                    }
                    public void methodB() {
                    }
                    public void methodC() {
                    }
                }
                """;

        assertNotNull(myFixture);
        PsiClass psiClass = myFixture.addClass(code);
        PsiMethod psiMethod = psiClass.findMethodsByName("methodA", false)[0];
        List<PsiMethodCallExpression> psiCallExpressions = methodService.findMethodCalls(psiMethod);
        assertNotEmpty(psiCallExpressions);
        assertSize(2, psiCallExpressions);
    }

    @Override
    protected void tearDown() throws Exception{
        super.tearDown();
    }
}
