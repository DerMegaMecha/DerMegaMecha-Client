package com.dermegamecha.launcher.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;

import java.util.Random;

public final class BlockWorldArtwork extends Region {
    private final Canvas canvas = new Canvas();

    public BlockWorldArtwork() {
        getChildren().add(canvas);
        canvas.setMouseTransparent(true);
        Rectangle clip = new Rectangle();
        clip.setArcWidth(36);
        clip.setArcHeight(36);
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);
        setMinHeight(250);
        setPrefHeight(360);
    }

    @Override protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0 || height <= 0) return;
        canvas.setWidth(width);
        canvas.setHeight(height);
        draw(width, height);
    }

    @Override protected double computePrefWidth(double height) {
        return 820;
    }

    private void draw(double width, double height) {
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.setEffect(null);
        graphics.setGlobalAlpha(1);
        graphics.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#101d31")),
                new Stop(0.52, Color.web("#0b1728")),
                new Stop(1, Color.web("#07111d"))));
        graphics.fillRect(0, 0, width, height);

        graphics.setFill(new RadialGradient(0, 0, 0.79, 0.36, 0.48, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(0, 217, 255, 0.30)),
                new Stop(0.48, Color.rgb(22, 119, 255, 0.12)),
                new Stop(1, Color.rgb(7, 11, 20, 0))));
        graphics.fillRect(0, 0, width, height);

        Random stars = new Random(271828);
        for (int i = 0; i < 46; i++) {
            double x = stars.nextDouble() * width;
            double y = stars.nextDouble() * height * 0.62;
            double size = i % 9 == 0 ? 2.2 : 1.1;
            graphics.setFill(i % 4 == 0 ? Color.rgb(137, 232, 255, 0.78) : Color.rgb(169, 202, 231, 0.42));
            graphics.fillOval(x, y, size, size);
        }

        double orb = Math.min(86, height * 0.24);
        graphics.setFill(Color.rgb(0, 217, 255, 0.09));
        graphics.fillOval(width * 0.79 - orb * 0.26, height * 0.13 - orb * 0.26, orb * 1.52, orb * 1.52);
        graphics.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#d8f8ff")),
                new Stop(0.64, Color.web("#69dff5")),
                new Stop(1, Color.web("#1677ff"))));
        graphics.fillOval(width * 0.79, height * 0.13, orb, orb);

        polygon(graphics, Color.web("#152b3e"),
                new double[] {0, width * .26, width * .39, width * .51, width * .62, width * .74, width, width},
                new double[] {height * .73, height * .42, height * .63, height * .36, height * .60, height * .45, height * .67, height});
        polygon(graphics, Color.web("#102236"),
                new double[] {0, width * .17, width * .30, width * .43, width * .56, width * .70, width, width},
                new double[] {height * .84, height * .57, height * .77, height * .53, height * .80, height * .59, height * .82, height});
        polygon(graphics, Color.web("#0a1a29"),
                new double[] {0, width * .23, width * .38, width * .53, width * .68, width * .84, width, width},
                new double[] {height * .91, height * .73, height * .89, height * .69, height * .90, height * .75, height * .91, height});

        graphics.setFill(Color.rgb(7, 15, 25, 0.82));
        for (int i = 0; i < 9; i++) {
            double x = width * (0.50 + i * 0.057);
            double blockWidth = width * (0.022 + (i % 3) * 0.006);
            double buildingHeight = height * (0.11 + (i * 13 % 7) * 0.026);
            graphics.fillRect(x, height * .77 - buildingHeight, blockWidth, buildingHeight);
            graphics.setFill(Color.rgb(0, 217, 255, 0.36));
            for (int row = 0; row < Math.floor(buildingHeight / 20); row++) {
                if ((i + row) % 2 == 0) graphics.fillRect(x + blockWidth * .28, height * .77 - buildingHeight + row * 20 + 6,
                        Math.max(2, blockWidth * .18), 3);
            }
            graphics.setFill(Color.rgb(7, 15, 25, 0.82));
        }

        graphics.setEffect(new DropShadow(12, Color.rgb(0, 217, 255, 0.46)));
        graphics.setStroke(Color.rgb(35, 205, 244, 0.60));
        graphics.setLineWidth(1.15);
        double[][] traces = {
                {width * .67, height * .73, width * .72, height * .73, width * .72, height * .66, width * .79, height * .66},
                {width * .81, height * .82, width * .85, height * .82, width * .85, height * .74, width * .91, height * .74},
                {width * .58, height * .83, width * .62, height * .83, width * .62, height * .78, width * .66, height * .78}
        };
        for (double[] trace : traces) {
            graphics.beginPath();
            graphics.moveTo(trace[0], trace[1]);
            for (int i = 2; i < trace.length; i += 2) graphics.lineTo(trace[i], trace[i + 1]);
            graphics.stroke();
        }
        graphics.setEffect(null);
        graphics.setFill(Color.rgb(0, 217, 255, 0.82));
        for (double[] trace : traces) graphics.fillOval(trace[trace.length - 2] - 2, trace[trace.length - 1] - 2, 4, 4);

        graphics.setStroke(Color.rgb(77, 186, 218, 0.14));
        graphics.setLineWidth(1);
        for (int i = 0; i < 7; i++) {
            double x = width * (.64 + i * .06);
            graphics.strokeLine(x, height * .78, x + width * .08, height);
        }
        graphics.strokeLine(width * .55, height * .90, width, height * .90);
    }

    private static void polygon(GraphicsContext graphics, Color fill, double[] x, double[] y) {
        graphics.setFill(fill);
        graphics.fillPolygon(x, y, x.length);
        graphics.setStroke(Color.rgb(0, 217, 255, 0.16));
        graphics.setLineWidth(1);
        graphics.strokePolyline(x, y, x.length);
    }
}
