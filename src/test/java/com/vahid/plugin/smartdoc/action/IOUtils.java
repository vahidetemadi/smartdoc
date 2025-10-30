/*
 * Copyright (c) 2025 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartdoc.action;

import com.intellij.psi.PsiMethod;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class IOUtils {
    public static void persistChanges(String modelName, Map<PsiMethod, CommentPairHolderDto> theMap, String projectName) {
        Path testResultPath = Paths.get(STR."src/test/resources/testResults/\{modelName}/\{projectName}.txt");
        try {
            Files.createDirectories(testResultPath.getParent());
            if (Files.notExists(testResultPath)) {
                Files.createFile(testResultPath);
            }
            try(FileWriter writer = new FileWriter(testResultPath.toFile(), true)) {
                if (Files.size(testResultPath) == 0) {
                    writer.append("method, actual, expected\n");
                }
                for (Map.Entry<PsiMethod, CommentPairHolderDto> entry : theMap.entrySet()) {
                    CommentPairHolderDto value = entry.getValue();
                    writer
                            .append(entry.getKey().getName())
                            .append(",")
                            .append(Optional.ofNullable(value.getActual()).map(a -> a.lines().collect(Collectors.joining())).orElse(""))
                            .append(",")
                            .append(Optional.ofNullable(value.getExpected()).map(e -> e.lines().collect(Collectors.joining())).orElse(""))
                            .append("\n");
                }
            }
        } catch (IOException io) {
            io.printStackTrace();
        }
    }
}
