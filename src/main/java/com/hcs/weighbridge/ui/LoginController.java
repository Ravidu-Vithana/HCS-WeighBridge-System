package com.hcs.weighbridge.ui;

import com.hcs.weighbridge.MainApp;
import com.hcs.weighbridge.config.DatabaseConfig;
import com.hcs.weighbridge.dao.UserDao;
import com.hcs.weighbridge.util.UiScaler;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button exitButton;

    @FXML
    private Label errorLabel;

    @FXML
    private Parent rootPane;

    private UiScaler uiScaler;

    @FXML
    public void initialize() {
        loginButton.setOnAction(event -> handleLogin());
        exitButton.setOnAction(event -> Platform.exit());
        usernameField.setOnAction(event -> passwordField.requestFocus());
        passwordField.setOnAction(event -> handleLogin());

        // Initialize scaler with default 2.0 or load from config
        uiScaler = new UiScaler(2.0);
        uiScaler.applyScaling(rootPane);
    }

    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter username and password.");
            errorLabel.setVisible(true);
            return;
        }

        try {
            Connection connection = DatabaseConfig.getConnection();
            UserDao userDao = new UserDao(connection);

            if (userDao.validateUser(username, password)) {
                loadMainApp();
            } else {
                errorLabel.setText("Invalid username or password.");
                errorLabel.setVisible(true);
            }
        } catch (Exception e) {
            errorLabel.setText("Database error: " + e.getMessage());
            errorLabel.setVisible(true);
            e.printStackTrace();
        }
    }

    private void loadMainApp() {
        try {
            if (MainApp.getInstance() != null) {
                MainApp.getInstance().showMainView();
                // Close login window
                Stage stage = (Stage) loginButton.getScene().getWindow();
                stage.close();
            } else {
                errorLabel.setText("Application error: MainApp instance not found.");
                errorLabel.setVisible(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Error loading main application.");
            errorLabel.setVisible(true);
        }
    }
}
