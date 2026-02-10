package com.hcs.weighbridge.ui;

import com.fazecast.jSerialComm.SerialPort;
import com.hcs.weighbridge.dao.ConfigDao;
import com.hcs.weighbridge.dao.UserDao;
import com.hcs.weighbridge.model.Role;
import com.hcs.weighbridge.model.SerialConfig;
import com.hcs.weighbridge.model.User;
import com.hcs.weighbridge.util.UiUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

import static com.hcs.weighbridge.util.UiUtils.showAlert;
import static com.hcs.weighbridge.util.UiUtils.showToast;

public class SettingsController implements Initializable {

    @FXML
    private ComboBox<String> portCombo;
    @FXML
    private ComboBox<Integer> baudCombo;
    @FXML
    private ComboBox<Integer> dataBitsCombo;
    @FXML
    private ComboBox<Integer> stopBitsCombo;
    @FXML
    private ComboBox<String> parityCombo;

    @FXML
    private Slider scaleSlider;
    @FXML
    private Label scaleLabel;

    @FXML
    private VBox serialSettingsContainer;
    @FXML
    private VBox userManagementContainer;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ComboBox<Role> roleCombo;
    @FXML
    private TableView<User> usersTable;

    private ConfigDao configDao;
    private UserDao userDao;
    private MainController mainController;
    private BorderPane mainControllerRootPane;
    private User currentUser;

    public void setDependencies(ConfigDao configDao, UserDao userDao, MainController mainController, User currentUser) {
        this.configDao = configDao;
        this.userDao = userDao;
        this.mainController = mainController;
        this.mainControllerRootPane = mainController.getRootPane();
        this.currentUser = currentUser;
        loadData();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            setupComboBoxes();
            setupScaleSlider();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void setupComboBoxes() {
        for (SerialPort p : SerialPort.getCommPorts()) {
            portCombo.getItems().add(p.getSystemPortName());
        }
        baudCombo.getItems().addAll(1200, 2400, 4800, 9600);
        dataBitsCombo.getItems().addAll(7, 8);
        stopBitsCombo.getItems().addAll(1, 2);
        parityCombo.getItems().addAll("NONE", "EVEN", "ODD");

        roleCombo.getItems().setAll(Role.values());
        roleCombo.setValue(Role.USER);
    }

    private void setupScaleSlider() {
        scaleSlider.setMin(1.0);
        scaleSlider.setMax(3.0);
        scaleSlider.setMajorTickUnit(0.5);
        scaleSlider.setMinorTickCount(0);
        scaleSlider.setShowTickLabels(true);
        scaleSlider.setShowTickMarks(true);
        scaleSlider.setSnapToTicks(true);

        scaleSlider.setValue(2.0);

        scaleSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateScaleLabel(newVal.doubleValue());
        });

        updateScaleLabel(scaleSlider.getValue());
    }

    private void updateScaleLabel(double value) {
        String label;
        if (value <= 1.0) {
            label = "Very Small";
        } else if (value <= 1.5) {
            label = "Small";
        } else if (value <= 2.0) {
            label = "Normal";
        } else if (value <= 2.5) {
            label = "Large";
        } else {
            label = "Very Large";
        }
        scaleLabel.setText(label);
    }

    private void loadData() {
        if (configDao == null)
            return;

        // Apply Access Control
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;
        if (serialSettingsContainer != null) {
            serialSettingsContainer.setVisible(isAdmin);
            serialSettingsContainer.setManaged(isAdmin);
        }
        if (userManagementContainer != null) {
            userManagementContainer.setVisible(isAdmin);
            userManagementContainer.setManaged(isAdmin);
        }

        // Load users list if admin
        if (isAdmin) {
            loadUsersList();
        }

        try {
            double currentScale = configDao.getUiScaleFactor();

            if (currentScale < 1.0 || currentScale > 3.0) {
                currentScale = 2.0;
            }

            scaleSlider.setValue(currentScale);
            updateScaleLabel(currentScale);

            SerialConfig cfg = configDao.loadSerialConfig();

            if (cfg.getPortName() != null && !cfg.getPortName().isEmpty()) {
                portCombo.setValue(cfg.getPortName());
            }

            if (cfg.getBaudRate() > 0) {
                baudCombo.setValue(cfg.getBaudRate());
            }

            if (cfg.getDataBits() > 0) {
                dataBitsCombo.setValue(cfg.getDataBits());
            }

            if (cfg.getStopBits() > 0) {
                stopBitsCombo.setValue(cfg.getStopBits());
            }

            String parityValue = cfg.getParity() == SerialPort.EVEN_PARITY ? "EVEN"
                    : cfg.getParity() == SerialPort.ODD_PARITY ? "ODD" : "NONE";
            parityCombo.setValue(parityValue);

        } catch (Exception e) {
            System.err.println("Failed to load settings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void addUser() {
        if (!isAdmin()) {
            showToast((Stage) mainControllerRootPane.getScene().getWindow(),
                    mainControllerRootPane,
                    "Access Denied: You do not have permission to add users.", 
                    false);
            return;
        }

        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        Role role = roleCombo.getValue();

        if (username.isEmpty() || password.isEmpty() || role == null) {
            showToast((Stage) mainControllerRootPane.getScene().getWindow(),
                    mainControllerRootPane,
                    "Please fill in all user fields.", 
                    false);
            return;
        }

        if (userDao.findByUsername(username) != null) {
            showToast((Stage) mainControllerRootPane.getScene().getWindow(),
                    mainControllerRootPane,
                    "User already exists!", 
                    false);
            return;
        }

        User newUser = new User(0, username, password, role);
        if (userDao.createUser(newUser)) {
            showToast((Stage) mainControllerRootPane.getScene().getWindow(),
                    mainControllerRootPane,
                    "User created successfully!", 
                    true);
            usernameField.clear();
            passwordField.clear();
            roleCombo.setValue(Role.USER);
            loadUsersList(); // Refresh the user list
        } else {
            showToast((Stage) mainControllerRootPane.getScene().getWindow(),
                    mainControllerRootPane,
                    "Failed to create user.", 
                    false);
        }
    }

    private void loadUsersList() {
        if (usersTable == null || userDao == null) {
            return;
        }

        java.util.List<User> users = userDao.getAllUsers();
        javafx.collections.ObservableList<User> usersList = javafx.collections.FXCollections.observableArrayList(users);
        usersTable.setItems(usersList);

        // Configure columns
        if (usersTable.getColumns().size() >= 3) {
            // Username column
            javafx.scene.control.TableColumn<User, String> usernameCol = (javafx.scene.control.TableColumn<User, String>) usersTable
                    .getColumns().get(0);
            usernameCol.setCellValueFactory(
                    cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getUsername()));

            // Role column
            javafx.scene.control.TableColumn<User, String> roleCol = (javafx.scene.control.TableColumn<User, String>) usersTable
                    .getColumns().get(1);
            roleCol.setCellValueFactory(
                    cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getRole().name()));

            // Actions column with delete button
            javafx.scene.control.TableColumn<User, Void> actionsCol = (javafx.scene.control.TableColumn<User, Void>) usersTable
                    .getColumns().get(2);
            actionsCol.setCellFactory(param -> new javafx.scene.control.TableCell<User, Void>() {
                private final javafx.scene.control.Button deleteButton = new javafx.scene.control.Button("Delete");

                {
                    deleteButton.setOnAction(event -> {
                        User user = getTableView().getItems().get(getIndex());
                        if (user != null) {
                            deleteUser(user);
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(deleteButton);
                    }
                }
            });
        }
    }

    private void deleteUser(User user) {
        // Prevent deleting current user
        if (currentUser != null && currentUser.getId() == user.getId()) {
            showToast((Stage) mainControllerRootPane.getScene().getWindow(),
                    mainControllerRootPane,
                    "Cannot delete the currently logged in user!",
                    false);
            return;
        }

        Stage stage = (Stage) mainControllerRootPane.getScene().getWindow();

        boolean confirmed = UiUtils.showConfirmation(
                stage,
                "Delete User",
                "Are you sure you want to delete user '" + user.getUsername() + "'?"
        );

        if (confirmed) {
            if (userDao.deleteUser(user.getId())) {
                showToast((Stage) mainControllerRootPane.getScene().getWindow(),
                    mainControllerRootPane,
                        "User deleted successfully!",
                        true);
                loadUsersList(); // Refresh the list
            } else {
                showToast((Stage) mainControllerRootPane.getScene().getWindow(),
                    mainControllerRootPane,
                        "Failed to delete user.",
                        false);
            }
        }
    }

    private boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == Role.ADMIN;
    }

    @FXML
    private void save() {
        if (configDao == null) {
            showToast((Stage) mainControllerRootPane.getScene().getWindow(),
                    mainControllerRootPane,
                    "Configuration DAO not initialized!",
                    false);
            return;
        }

        try {
            if (isAdmin()) {
                SerialConfig cfg = new SerialConfig();
                cfg.setPortName(portCombo.getValue());

                if (baudCombo.getValue() != null) {
                    cfg.setBaudRate(baudCombo.getValue());
                }

                if (dataBitsCombo.getValue() != null) {
                    cfg.setDataBits(dataBitsCombo.getValue());
                }

                if (stopBitsCombo.getValue() != null) {
                    cfg.setStopBits(stopBitsCombo.getValue());
                }

                String parity = parityCombo.getValue();
                if ("EVEN".equals(parity)) {
                    cfg.setParity(SerialPort.EVEN_PARITY);
                } else if ("ODD".equals(parity)) {
                    cfg.setParity(SerialPort.ODD_PARITY);
                } else {
                    cfg.setParity(SerialPort.NO_PARITY);
                }

                configDao.saveSerialConfig(cfg);
            }

            double scaleFactor = scaleSlider.getValue();
            configDao.saveUiScaleFactor(scaleFactor);

            if (mainController != null) {
                mainController.reloadWithScale(scaleFactor);
                showToast((Stage) mainControllerRootPane.getScene().getWindow(),
                    mainControllerRootPane,
                        "Settings saved successfully!",
                        true);
            } else {
                showAlert("Settings","Settings saved successfully!\nPlease restart the application for UI scale to take full effect.");
            }

            close();

        } catch (Exception e) {
            System.err.println("Failed to save settings: " + e.getMessage());
            e.printStackTrace();
            showToast((Stage) mainControllerRootPane.getScene().getWindow(),
                    mainControllerRootPane,
                    "Failed to save settings: " + e.getMessage(),
                    false);
        }
    }

    @FXML
    private void close() {
        Stage stage = (Stage) scaleSlider.getScene().getWindow();
        stage.close();
    }
}
