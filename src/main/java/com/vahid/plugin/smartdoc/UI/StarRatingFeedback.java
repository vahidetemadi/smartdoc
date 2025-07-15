package com.vahid.plugin.smartdoc.UI;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.JBUI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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

                    WebClient.create("http://localhost:8000")
                            .post()
                            .uri("/send-feedback")
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .bodyValue("""
                                    {
                                        "rate": "%s",
                                        "method_size": "%s"
                                    }""".formatted(starIndex, psiMethod.getName()))
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
                    }
                }
            });

            stars[i] = star;
            panel.add(star);
        }

        final int debounceDelayMs = 150;

        final javax.swing.Timer debounceTimer = new javax.swing.Timer(debounceDelayMs, null);
        debounceTimer.setRepeats(false);

        Runnable showBalloonAtOffset = () -> {
            if (balloonRef[0] != null && !balloonRef[0].isDisposed()) {
                balloonRef[0].hide();
            }

            int offset = psiMethod.getTextOffset();
            Point xy = editor.visualPositionToXY(editor.offsetToVisualPosition(offset));
            RelativePoint relativePoint = new RelativePoint(editor.getContentComponent(), xy);

            Balloon balloon = JBPopupFactory.getInstance()
                    .createBalloonBuilder(panel)
                    .setHideOnClickOutside(false)
                    .setHideOnKeyOutside(false)
                    .setCloseButtonEnabled(true)
                    .setFillColor(JBColor.PanelBackground)
                    .createBalloon();

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
        activeBalloons.forEach((id, balloon) -> {
            if (balloon != null && !balloon.isDisposed()) {
                balloon.hide();
                balloon.isDisposed();
            }
        });
        activeBalloons.clear();
    }


    public static void discardIsRated(String s) {
        ratedMethods.remove(s);
    }
}
