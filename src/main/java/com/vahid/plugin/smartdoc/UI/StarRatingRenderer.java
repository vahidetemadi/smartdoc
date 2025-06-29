package com.vahid.plugin.smartdoc.UI;

import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.concurrent.CompletableFuture;

class StarRatingRenderer implements EditorCustomElementRenderer {
    private final PsiMethod method;

    public StarRatingRenderer(PsiMethod method) {
        this.method = method;
    }

    @Override
    public int calcWidthInPixels(@NotNull Inlay inlay) {
        return 5 * 16; // rough width for 5 stars
    }

    @Override
    public void paint(@NotNull Inlay inlay, @NotNull Graphics g, @NotNull Rectangle targetRegion, @NotNull TextAttributes textAttributes) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.YELLOW);
        g2.setFont(new Font("Dialog", Font.PLAIN, 16));
        for (int i = 0; i < 5; i++) {
            g2.drawString("⭐", targetRegion.x + i * 16, targetRegion.y + targetRegion.height - 4);
        }
    }

//    @Override
//    public void onClick(@NotNull MouseEvent event, @NotNull Point translated) {
//        int starIndex = translated.x / 16 + 1;
//        if (starIndex >= 1 && starIndex <= 5) {
//            //sendRatingToServer(method, starIndex);
//            Inlay<?> inlay = (Inlay<?>) event.getSource();
//            inlay.dispose();
//        }
//    }

//    private void sendRatingToServer(PsiMethod method, int rating) {
//        CompletableFuture.runAsync(() -> {
//            try {
//                HttpClient client = HttpClient.newHttpClient();
//                HttpRequest request = HttpRequest.newBuilder()
//                        .uri(new URI("https://yourserver.example/api/rate"))
//                        .header("Content-Type", "application/json")
//                        .POST(HttpRequest.BodyPublishers.ofString(
//                                "{\"method\":\"" + method.getName() + "\", \"rating\":" + rating + "}"
//                        ))
//                        .build();
//                client.send(request, HttpResponse.BodyHandlers.ofString());
//            } catch (Exception e) {
//                Logger.getInstance(getClass()).warn("Failed to send rating", e);
//            }
//        });
//    }
}

