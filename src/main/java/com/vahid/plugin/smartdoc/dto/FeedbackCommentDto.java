package com.vahid.plugin.smartdoc.dto;

import com.intellij.psi.PsiMethod;
import com.vahid.plugin.smartdoc.value.RemoteLLM;

public record FeedbackCommentDto(RemoteLLM remoteLLM, PsiMethod psiMethod) {
}
