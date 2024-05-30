package org.example;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

public class TimeRuler extends Pane {
    public TimeRuler() {
        setPrefHeight(20);
        setStyle("-fx-background-color: white;");

        int totalSeconds = 10;
        int segments = 10;

        for (int i = 0; i <= totalSeconds; i++) {
            double x = (double) i / totalSeconds * 2000;
            Line line = new Line(x, 0, x, 50);
            line.setStroke(Color.GRAY);
            getChildren().add(line);

            Text text = new Text(x + 2, 12, i + "s");
            text.setFill(Color.BLACK);
            getChildren().add(text);
        }

        for (int i = 1; i < segments * totalSeconds; i++) {
            double x = (double) i / (segments * totalSeconds) * 2000;
            Line line = new Line(x, 0, x, 100);
            line.setStroke(Color.LIGHTGRAY);
            getChildren().add(line);
        }
    }
}