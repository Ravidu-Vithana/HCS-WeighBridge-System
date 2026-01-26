package com.hcs.weighbridge.util;

import javafx.scene.Parent;

public class UiScaler {

    private double scaleFactor;

    public UiScaler(double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    public void setScaleFactor(double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    public void applyScaling(Parent root) {
        if (root == null) return;

        root.getStyleClass().removeAll(
                "scale-xs", "scale-sm", "scale-md", "scale-lg", "scale-xl"
        );

        String scaleClass = getScaleClass();
        if (!scaleClass.isEmpty()) {
            root.getStyleClass().add(scaleClass);
        }

        root.applyCss();
        root.requestLayout();
    }

    private String getScaleClass() {
        if (scaleFactor <= 1.0) return "scale-xs";
        if (scaleFactor <= 1.5) return "scale-sm";
        if (scaleFactor <= 2.0) return "scale-md";
        if (scaleFactor <= 2.5) return "scale-lg";
        return "scale-xl";
    }
}