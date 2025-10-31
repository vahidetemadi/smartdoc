/*
 * Copyright (c) 2025 Vahid Etemadi
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0 International License.
 * You may share this work (copy and redistribute) under the terms of the license,
 * but you may not remix, transform, or build upon it.
 * To view a copy of the license, visit https://creativecommons.org/licenses/by-nd/4.0/
 */
package com.vahid.plugin.smartdoc.UI;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.VisibleAreaListener;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.JBUI;
import com.vahid.plugin.smartdoc.action.UpdateAction;
import com.vahid.plugin.smartdoc.dto.FeedbackCommentDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class StarRatingFeedback {

    private static Logger logger = LoggerFactory.getLogger(StarRatingFeedback.class);
    private static final ConcurrentHashMap<String, Balloon> activeBalloons = new ConcurrentHashMap<>();

    private StarRatingFeedback() {
    }

    private static final Set<String> ratedMethods = ConcurrentHashMap.newKeySet();

    public static void show(Editor editor, FeedbackCommentDto dto) {
        PsiMethod psiMethod = dto.psiMethod();
        String methodId = psiMethod.getContainingFile().getVirtualFile().getPath() + "#" + psiMethod.getName();
        if (ratedMethods.contains(methodId)) {
            return;
        }
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        panel.setBorder(JBUI.Borders.empty(5));
        panel.add(new JLabel("Rate this comment: "));
        JLabel[] stars = new JLabel[5];
        final Balloon[] balloonRef = new Balloon[1]; // Mutable holder

        for (int i = 0; i < 5; i++) {
            final int starIndex = i + 1;
            JLabel star = new JLabel("☆");
            star.setFont(star.getFont().deriveFont(20f));
            star.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            star.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    updateStars(stars, starIndex);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    updateStars(stars, 0);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    updateStars(stars, starIndex);

                    panel.removeAll();
                    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                    JLabel commentLabel = new JLabel("Optional comment (or leave blank):");
                    commentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JTextArea commentArea = new JTextArea(4, 30);
                    commentArea.setLineWrap(true);
                    commentArea.setWrapStyleWord(true);

                    JScrollPane scrollPane = new JScrollPane(commentArea);
                    scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
                    scrollPane.setPreferredSize(new Dimension(400, 80));

                    JButton submitButton = new JButton("Submit");
                    submitButton.setAlignmentX(Component.LEFT_ALIGNMENT);
                    submitButton.addActionListener(ev -> {
                        String userFeedback = commentArea.getText().trim();

                        WebClient.create("http://localhost:8000")
                                .post()
                                .uri("/send-feedback")
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .bodyValue("""
                                            {
                                                "rate": "%s",
                                                "method_size": "%s",
                                                "comment": "%s",
                                                "LLM type: "%s""
                                            }""".formatted(starIndex, psiMethod.getName(), escapeJson(userFeedback), dto.remoteLLM()))
                                .retrieve()
                                .bodyToMono(Void.class)
                                .subscribe(
                                        unsend -> {},
                                        error -> {},
                                        () -> logger.info("Post completed successfully"));

                        logger.info("User rated: {} stars, method: {}, llm: {}",
                                starIndex,
                                psiMethod.getName(),
                                dto.remoteLLM());
                        ratedMethods.add(methodId);
                        if (balloonRef[0] != null) {
                            balloonRef[0].hide();
                            activeBalloons.remove(methodId);
                        }
                    });

                    panel.add(commentLabel);
                    panel.add(Box.createVerticalStrut(5));
                    panel.add(scrollPane);
                    panel.add(Box.createVerticalStrut(10));
                    panel.add(submitButton);

                    panel.revalidate();
                    panel.repaint();

                    // Optional: force balloon re-pack if using IntelliJ’s Balloon API
                    if (balloonRef[0] != null) {
                        balloonRef[0].revalidate(); // or hide+show if not auto-updating size
                    }
                }

            });

            stars[i] = star;
            panel.add(star);
        }

        JButton closeButton = new JButton("Not Interested");
        closeButton.setMargin(new Insets(0, 0, 0, 0));
        closeButton.setFocusable(false);
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        panel.add(closeButton);


        final int debounceDelayMs = 150;

        final javax.swing.Timer debounceTimer = new javax.swing.Timer(debounceDelayMs, null);
        debounceTimer.setRepeats(false);

        Runnable showBalloonAtOffset = () -> {
            if (balloonRef[0] != null && !balloonRef[0].isDisposed()) {
                balloonRef[0].hide(true);
                balloonRef[0].dispose();
            }

            activeBalloons.remove(methodId);

            int offset = psiMethod.getTextOffset();
            Point xy = editor.visualPositionToXY(editor.offsetToVisualPosition(offset));
            RelativePoint relativePoint = new RelativePoint(editor.getContentComponent(), xy);

            Balloon balloon = JBPopupFactory.getInstance()
                    .createBalloonBuilder(panel)
                    .setHideOnClickOutside(false)
                    .setHideOnKeyOutside(false)
                    .setCloseButtonEnabled(false)
                    .setRequestFocus(false)
                    .setBlockClicksThroughBalloon(true)
                    .setFillColor(JBColor.PanelBackground)
                    .createBalloon();

            balloonRef[0] = balloon;
            activeBalloons.put(methodId, balloon);
            balloon.show(relativePoint, Balloon.Position.below);
        };

        showBalloonAtOffset.run();

        VisibleAreaListener[] listenerRef = new VisibleAreaListener[1];

        listenerRef[0] = e -> {
            Rectangle oldArea = e.getOldRectangle();
            Rectangle newArea = e.getNewRectangle();

            // Only act if vertical scroll (Y position changed)
            if (oldArea.y != newArea.y) {
                if (balloonRef[0] != null && !balloonRef[0].isDisposed()) {
                    balloonRef[0].hide();
                    balloonRef[0].dispose();
                    activeBalloons.remove(methodId);
                    editor.getScrollingModel().removeVisibleAreaListener(listenerRef[0]);
                }
            }
        };

        editor.getScrollingModel().addVisibleAreaListener(listenerRef[0]);

        closeButton.addActionListener(e -> {
            ratedMethods.add(methodId);
            if (balloonRef[0] != null) {
                balloonRef[0].hide();
                balloonRef[0].dispose();
                activeBalloons.remove(methodId);
            }

            editor.getScrollingModel().removeVisibleAreaListener(listenerRef[0]);
        });
    }

    private static void updateStars(JLabel[] stars, int count) {
        for (int i = 0; i < stars.length; i++) {
            stars[i].setText(i < count ? "★" : "☆");
        }
    }

    public static boolean isRated(String methodId) {
        return ratedMethods.contains(methodId);
    }

    public static void dismissAllBalloons() {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            for (Balloon balloon : activeBalloons.values()) {
                if (balloon != null) {
                    balloon.hide(true);
                    balloon.dispose();
                }
            }
            activeBalloons.clear();
        });
    }

    public static void discardIsRated(String s) {
        ratedMethods.remove(s);
    }

    private static String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}