package com.hcs.weighbridge.util;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Optional;

public final class UiUtils {

    private UiUtils() {
        // Prevent instantiation
    }

    public static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static boolean showConfirmation(Stage owner,
                                           String title,
                                           String message) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        Button cancelButton = (Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL);

        cancelButton.setDefaultButton(true);
        okButton.setDefaultButton(false);

        alert.getDialogPane().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                Node focused = alert.getDialogPane().getScene().getFocusOwner();
                if (focused instanceof Button) {
                    ((Button) focused).fire();
                    event.consume();
                }
            }
        });

        Optional<ButtonType> result = alert.showAndWait();
        return result.orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    public static void showToast(Stage owner,
                                 Node rootNode,
                                 String message,
                                 boolean isSuccess) {

        if (owner == null || rootNode == null) return;

        Platform.runLater(() -> {

            Popup popup = new Popup();

            FontIcon icon = new FontIcon();
            icon.setIconLiteral(isSuccess ? "mdi-check-circle" : "mdi-alert-circle");
            icon.setIconSize(22);
            icon.setIconColor(Color.valueOf(isSuccess ? "#4CAF50" : "#F44336"));

            Label messageLabel = new Label(message);
            messageLabel.setStyle(
                    "-fx-text-fill: white; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: 600; " +
                            "-fx-font-family: 'Segoe UI', system-ui;");
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(320);

            HBox content = new HBox(12, icon, messageLabel);
            content.setAlignment(Pos.CENTER_LEFT);

            content.setStyle(
                    "-fx-background-color: rgba(35, 35, 35, 0.9); " +
                            "-fx-background-radius: 10; " +
                            "-fx-padding: 12 20; " +
                            "-fx-border-color: rgba(255, 255, 255, 0.15); " +
                            "-fx-border-width: 1; " +
                            "-fx-border-radius: 10; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 15, 0, 0, 8);");

            popup.getContent().add(content);
            popup.setAutoHide(true);

            content.setOpacity(0);

            popup.setOnShown(e -> {

                double x = owner.getX() + owner.getWidth() - content.getWidth() - 30;
                double y = owner.getY() + owner.getHeight() - content.getHeight() - 60;

                popup.setX(x);
                popup.setY(y);

                TranslateTransition slide = new TranslateTransition(Duration.millis(350), content);
                slide.setFromY(20);
                slide.setToY(0);

                FadeTransition fade = new FadeTransition(Duration.millis(350), content);
                fade.setFromValue(0);
                fade.setToValue(1);

                new ParallelTransition(slide, fade).play();
            });

            popup.show(owner);

            PauseTransition pause = new PauseTransition(Duration.seconds(3.5));
            pause.setOnFinished(e -> {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(400), content);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(ev -> popup.hide());
                fadeOut.play();
            });
            pause.play();
        });
    }

    public static void showSuccessToast(Stage owner, Node root, String message) {
        showToast(owner, root, message, true);
    }

    public static void showErrorToast(Stage owner, Node root, String message) {
        showToast(owner, root, message, false);
    }
}
