package com.vahid.plugin.smartdoc.UI;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
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

    private static final ConcurrentHashMap<String, Balloon> activeBalloons = new ConcurrentHashMap<>();
    private StarRatingFeedback() {
    }

    private static final Set<String> ratedMethods = ConcurrentHashMap.newKeySet();

    public static void show(Editor editor, PsiMethod psiMethod) {
        String methodId = psiMethod.getContainingFile().getVirtualFile().getPath() + "#" + psiMethod.getName();
        if (ratedMethods.contains(methodId)) {
            return; // Already rated, skip showing balloon
        }
        System.out.println("Showing from balloon...");
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

                    String userFeedback = Messages.showInputDialog(
                            "Add optional feedback (or leave blank):",
                            "Submit Rating",
                            Messages.getQuestionIcon()
                    );

                    WebClient.create("http://localhost:8000")
                            .post()
                            .uri("/send-feedback")
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .bodyValue("""
                                    {
                                        "rate": "%s",
                                        "method_size": "%s",
                                        "comment": "%s"
                                    }""".formatted(starIndex, psiMethod.getName(), userFeedback))
                            .retrieve()
                            .bodyToMono(Void.class)
                                    .subscribe(unsend -> {},
                                            error -> {},
                                            () -> System.out.println("Post completed successfully"));

                    System.out.println("User rated: " + starIndex + " stars, for method: " + psiMethod.getName());
                    ratedMethods.add(methodId);
                    if (balloonRef[0] != null) {
                        balloonRef[0].hide(); // Dismiss the balloon
                        balloonRef[0].isDisposed();
                        activeBalloons.remove(methodId);
                        System.out.println("Exit balloon");
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

        closeButton.addActionListener(e -> {
            System.out.println("Closed via custom X");
            ratedMethods.add(methodId);
            if (balloonRef[0] != null) {
                balloonRef[0].hide();
                activeBalloons.remove(methodId);
            }
        });

        panel.add(closeButton); // Add it somewhere on the top/right


        final int debounceDelayMs = 150;

        final javax.swing.Timer debounceTimer = new javax.swing.Timer(debounceDelayMs, null);
        debounceTimer.setRepeats(false);

        Runnable showBalloonAtOffset = () -> {
            if (balloonRef[0] != null && !balloonRef[0].isDisposed()) {
                balloonRef[0].hide();
                balloonRef[0].dispose();
            }

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

//            balloon.addListener(new JBPopupListener() {
//                @Override
//                public void onClosed(@NotNull LightweightWindowEvent event) {
//                    System.out.println("event ok" + event.asBalloon().isDisposed());
//                    System.out.println("contains" + ratedMethods.contains(methodId));
//                    if (event.asBalloon().isDisposed() && !ratedMethods.contains(methodId)) {
//                        System.out.println("Balloon closed by user (X or ESC), marking as rated.");
//                        ratedMethods.add(methodId);
//                    }
//                    activeBalloons.remove(methodId);
//                }
//            });

            balloonRef[0] = balloon;
            activeBalloons.put(methodId, balloon);
            balloon.show(relativePoint, Balloon.Position.below);
        };

        showBalloonAtOffset.run();

        editor.getScrollingModel().addVisibleAreaListener(e -> {
            if (balloonRef[0] == null || balloonRef[0].isDisposed()) {
                debounceTimer.stop();
                return;
            }
            debounceTimer.stop();
            debounceTimer.addActionListener(ev -> showBalloonAtOffset.run());
            debounceTimer.start();
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
        ApplicationManager.getApplication().invokeLater(() -> {
            activeBalloons.forEach((id, balloon) -> {
                if (balloon != null) {
                    balloon.hide();
                    balloon.dispose();
                    System.out.println("Hided balloon X");
                }
            });
            activeBalloons.clear();
        });
    }


    public static void discardIsRated(String s) {
        ratedMethods.remove(s);
    }
}








//package com.vahid.plugin.smartdoc.UI;
//
//import com.intellij.openapi.editor.Editor;
//import com.intellij.openapi.ui.popup.Balloon;
//import com.intellij.openapi.ui.popup.JBPopupFactory;
//import com.intellij.openapi.ui.popup.JBPopupListener;
//import com.intellij.openapi.ui.popup.LightweightWindowEvent;
//import com.intellij.psi.PsiMethod;
//import com.intellij.ui.JBColor;
//import com.intellij.ui.awt.RelativePoint;
//import com.intellij.util.ui.JBUI;
//import org.jetbrains.annotations.NotNull;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.web.reactive.function.client.WebClient;
//
//import javax.swing.*;
//import java.awt.*;
//import java.awt.event.MouseAdapter;
//import java.awt.event.MouseEvent;
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//
//public class StarRatingFeedback {
//
//    private static final ConcurrentHashMap<String, Balloon> activeBalloons = new ConcurrentHashMap<>();
//    private static final Set<String> ratedMethods = ConcurrentHashMap.newKeySet();
//
//    private StarRatingFeedback() {}
//
//    public static void show(Editor editor, PsiMethod psiMethod) {
//        String methodId = psiMethod.getContainingFile().getVirtualFile().getPath() + "#" + psiMethod.getName();
//        if (ratedMethods.contains(methodId)) {
//            return;
//        }
//
//        JPanel panel = new JPanel();
//        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
//        panel.setBorder(JBUI.Borders.empty(5));
//
//        JPanel starsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
//        starsPanel.add(new JLabel("Rate this comment: "));
//
//        JLabel[] stars = new JLabel[5];
//        final Balloon[] balloonRef = new Balloon[1];
//
//        for (int i = 0; i < 5; i++) {
//            final int starIndex = i + 1;
//            JLabel star = new JLabel("☆");
//            star.setFont(star.getFont().deriveFont(20f));
//            star.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//
//            star.addMouseListener(new MouseAdapter() {
//                @Override
//                public void mouseEntered(MouseEvent e) {
//                    updateStars(stars, starIndex);
//                }
//
//                @Override
//                public void mouseExited(MouseEvent e) {
//                    updateStars(stars, 0);
//                }
//
//                @Override
//                public void mouseClicked(MouseEvent e) {
//                    updateStars(stars, starIndex);
//
//                    // Replace stars with comment box
//                    panel.removeAll();
//
//                    JLabel commentLabel = new JLabel("Optional comment:");
//                    commentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
//
//                    JTextArea commentArea = new JTextArea(4, 30);
//                    commentArea.setLineWrap(true);
//                    commentArea.setWrapStyleWord(true);
//                    JScrollPane scrollPane = new JScrollPane(commentArea);
//                    scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
//
//                    JButton submitButton = new JButton("Submit");
//                    submitButton.setAlignmentX(Component.LEFT_ALIGNMENT);
//                    submitButton.addActionListener(ev -> {
//                        String userFeedback = commentArea.getText().trim();
//
//                        WebClient.create("http://localhost:8000")
//                                .post()
//                                .uri("/send-feedback")
//                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
//                                .bodyValue("""
//                                        {
//                                            "rate": "%s",
//                                            "method_size": "%s",
//                                            "comment": "%s"
//                                        }""".formatted(starIndex, psiMethod.getName(), escapeJson(userFeedback)))
//                                .retrieve()
//                                .bodyToMono(Void.class)
//                                .subscribe(
//                                        unsend -> {},
//                                        error -> {},
//                                        () -> System.out.println("Post completed successfully"));
//
//                        System.out.printf("User rated: %d stars, method: %s%n", starIndex, psiMethod.getName());
//                        ratedMethods.add(methodId);
//                        if (balloonRef[0] != null) {
//                            balloonRef[0].hide();
//                            activeBalloons.remove(methodId);
//                        }
//                    });
//
//                    panel.add(commentLabel);
//                    panel.add(scrollPane);
//                    panel.add(Box.createVerticalStrut(5));
//                    panel.add(submitButton);
//                    panel.revalidate();
//                    panel.repaint();
//                }
//            });
//
//            stars[i] = star;
//            starsPanel.add(star);
//        }
//
//        panel.add(starsPanel);
//
//        Runnable showBalloonAtOffset = () -> {
//            if (balloonRef[0] != null && !balloonRef[0].isDisposed()) {
//                balloonRef[0].hide();
//            }
//
//            int offset = psiMethod.getTextOffset();
//            Point xy = editor.visualPositionToXY(editor.offsetToVisualPosition(offset));
//            RelativePoint relativePoint = new RelativePoint(editor.getContentComponent(), xy);
//
//            Balloon balloon = JBPopupFactory.getInstance()
//                    .createBalloonBuilder(panel)
//                    .setHideOnClickOutside(false)
//                    .setHideOnKeyOutside(false)
//                    .setCloseButtonEnabled(true)
//                    .setRequestFocus(false)
//                    .setBlockClicksThroughBalloon(true)
//                    .setFillColor(JBColor.PanelBackground)
//                    .createBalloon();
//
//            balloon.addListener(new JBPopupListener() {
//                @Override
//                public void onClosed(@NotNull LightweightWindowEvent event) {
//                    if (!ratedMethods.contains(methodId)) {
//                        System.out.println("Balloon closed by user, marking as rated.");
//                        ratedMethods.add(methodId);
//                    }
//                    activeBalloons.remove(methodId);
//                }
//            });
//
//            balloonRef[0] = balloon;
//            activeBalloons.put(methodId, balloon);
//            balloon.show(relativePoint, Balloon.Position.below);
//        };
//
//        showBalloonAtOffset.run();
//
//        // Optional: debounce re-show if user scrolls
//        final int debounceDelayMs = 150;
//        final javax.swing.Timer debounceTimer = new javax.swing.Timer(debounceDelayMs, null);
//        debounceTimer.setRepeats(false);
//
//        editor.getScrollingModel().addVisibleAreaListener(e -> {
//            if (balloonRef[0] == null || balloonRef[0].isDisposed()) {
//                debounceTimer.stop();
//                return;
//            }
//            debounceTimer.stop();
//            debounceTimer.addActionListener(ev -> showBalloonAtOffset.run());
//            debounceTimer.start();
//        });
//    }
//
//    private static void updateStars(JLabel[] stars, int count) {
//        for (int i = 0; i < stars.length; i++) {
//            stars[i].setText(i < count ? "★" : "☆");
//        }
//    }
//
//    private static String escapeJson(String text) {
//        return text
//                .replace("\\", "\\\\")
//                .replace("\"", "\\\"")
//                .replace("\n", "\\n")
//                .replace("\r", "");
//    }
//
//    public static boolean isRated(String methodId) {
//        return ratedMethods.contains(methodId);
//    }
//
//    public static void dismissAllBalloons() {
//        activeBalloons.forEach((id, balloon) -> {
//            if (balloon != null && !balloon.isDisposed()) {
//                balloon.hide();
//            }
//        });
//        activeBalloons.clear();
//    }
//
//    public static void discardIsRated(String s) {
//        ratedMethods.remove(s);
//    }
//}
