package com.vahid.plugin.smartdoc.UI;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.JBUI;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class StarRatingFeedback {

    public static void show(Editor editor, PsiMethod psiMethod) {
        int offset = psiMethod.getTextOffset();
        Point xy = editor.visualPositionToXY(editor.offsetToVisualPosition(offset));
        RelativePoint relativePoint = new RelativePoint(editor.getContentComponent(), xy);

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

                    WebClient.create("http://localhost:8000").post()
                            .uri("/send-feedback")
                            .bodyValue("""
                                    {
                                        "rate": %s
                                    }""".formatted(starIndex))
                            .retrieve()
                            .bodyToMono(Void.class)
                                    .subscribe(unsend -> {},
                                            error -> {},
                                            () -> System.out.println("Post completed successfully"));

                    System.out.println("User rated: " + starIndex + " stars, for method: " + psiMethod.getName());
                    if (balloonRef[0] != null) {
                        balloonRef[0].hide(); // Dismiss the balloon
                    }
                }
            });

            stars[i] = star;
            panel.add(star);
        }

        Balloon balloon = JBPopupFactory.getInstance()
                .createBalloonBuilder(panel)
                .setHideOnClickOutside(false)
                .setHideOnKeyOutside(false)
                .setCloseButtonEnabled(true)
                .setFillColor(JBColor.PanelBackground)
                .createBalloon();

        balloonRef[0] = balloon;

        // Proper use of RelativePoint:
        balloon.show(relativePoint, Balloon.Position.below);
    }

    private static void updateStars(JLabel[] stars, int count) {
        for (int i = 0; i < stars.length; i++) {
            stars[i].setText(i < count ? "★" : "☆");
        }
    }
}
