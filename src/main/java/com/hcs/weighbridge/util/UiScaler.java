package com.hcs.weighbridge.util;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import java.util.*;

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
                "scale-sm", "scale-md", "scale-lg", "scale-xl"
        );

        String scaleClass = getScaleClass();
        if (!scaleClass.isEmpty()) {
            root.getStyleClass().add(scaleClass);
        }

        // Apply CSS ONCE
        root.applyCss();
        root.requestLayout();
    }

    private String getScaleClass() {
        if (scaleFactor <= 1.0) return "";
        if (scaleFactor <= 1.25) return "scale-md";
        if (scaleFactor <= 1.5) return "scale-lg";
        return "scale-xl";
    }
}
