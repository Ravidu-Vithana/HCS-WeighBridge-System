package com.hcs.weighbridge.ui;

import com.fazecast.jSerialComm.SerialPort;
import com.hcs.weighbridge.config.DatabaseConfig;
import com.hcs.weighbridge.dao.ConfigDao;
import com.hcs.weighbridge.dao.UserDao;
import com.hcs.weighbridge.model.Role;
import com.hcs.weighbridge.model.SerialConfig;
import com.hcs.weighbridge.model.User;
import com.hcs.weighbridge.util.LogUtil;
import com.hcs.weighbridge.util.UiUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.concurrent.Task;
import com.hcs.weighbridge.MainApp;
import com.hcs.weighbridge.util.SystemUtils;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ResourceBundle;

import static com.hcs.weighbridge.util.UiUtils.showAlert;
import static com.hcs.weighbridge.util.UiUtils.showToast;

public class SettingsController implements Initializable {

    private static final Logger logger = LogUtil.getLogger(DatabaseConfig.class);
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
    private CheckBox startupCheckBox;

    @FXML
    private VBox serialSettingsContainer;
    @FXML
    private VBox systemSettingsContainer;
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
            logger.error("Failed to initialize settings: {}", e.getMessage(), e);
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
        if (systemSettingsContainer != null) {
            systemSettingsContainer.setVisible(isAdmin);
            systemSettingsContainer.setManaged(isAdmin);
        }

        // Load users list if admin
        if (isAdmin) {
            loadUsersList();
        }

        Task<Void> loadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                double currentScale = configDao.getUiScaleFactor();
                SerialConfig cfg = configDao.loadSerialConfig();

                Platform.runLater(() -> {
                    double finalScale = (currentScale < 1.0 || currentScale > 3.0) ? 2.0 : currentScale;
                    scaleSlider.setValue(finalScale);
                    updateScaleLabel(finalScale);

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

                    if (startupCheckBox != null) {
                        startupCheckBox.setSelected(SystemUtils.isRunOnStartupEnabled());
                    }
                });
                return null;
            }
        };

        MainApp.getExecutorService().submit(loadTask);
    }

    @FXML
    private void addUser() {
        if (!isAdmin()) {
            showToast((Stage) portCombo.getScene().getWindow(),
                    portCombo,
                    "Access Denied: You do not have permission to add users.",
                    false);
            return;
        }

        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        Role role = roleCombo.getValue();

        if (username.isEmpty() || password.isEmpty() || role == null) {
            showToast((Stage) portCombo.getScene().getWindow(),
                    portCombo,
                    "Please fill in all user fields.",
                    false);
            return;
        }

        if (userDao.findByUsername(username) != null) {
            showToast((Stage) portCombo.getScene().getWindow(),
                    portCombo,
                    "User already exists!",
                    false);
            return;
        }

        User newUser = new User(0, username, password, role);
        Task<Boolean> addTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return userDao.createUser(newUser);
            }
        };

        addTask.setOnSucceeded(e -> {
            showToast((Stage) portCombo.getScene().getWindow(),
                    portCombo,
                    "User created successfully!",
                    true);
            usernameField.clear();
            passwordField.clear();
            roleCombo.setValue(Role.USER);
            loadUsersList(); // Refresh the user list
        });

        addTask.setOnFailed(e -> {
            Throwable ex = addTask.getException();
            showToast((Stage) portCombo.getScene().getWindow(),
                    portCombo,
                    "Failed to create user: " + ex.getMessage(),
                    false);
        });

        MainApp.getExecutorService().submit(addTask);
    }

    private void loadUsersList() {
        if (usersTable == null || userDao == null) {
            return;
        }

        Task<java.util.List<User>> loadUsersTask = new Task<java.util.List<User>>() {
            @Override
            protected java.util.List<User> call() throws Exception {
                return userDao.getAllUsers();
            }
        };

        loadUsersTask.setOnSucceeded(e -> {
            java.util.List<User> users = loadUsersTask.getValue();
            javafx.collections.ObservableList<User> usersList = javafx.collections.FXCollections
                    .observableArrayList(users);
            usersTable.setItems(usersList);
        });

        loadUsersTask.setOnFailed(e -> {
            Throwable ex = loadUsersTask.getException();
            logger.error("Failed to load users", ex);
            showToast((Stage) usersTable.getScene().getWindow(),
                    usersTable,
                    "Failed to load users: " + ex.getMessage(),
                    false);
        });

        MainApp.getExecutorService().submit(loadUsersTask);

        // Configure columns (usually done once, but keeping it here for simplicity as
        // per original)
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
            showToast((Stage) portCombo.getScene().getWindow(),
                    portCombo,
                    "Cannot delete the currently logged in user!",
                    false);
            return;
        }

        Stage stage = (Stage) mainControllerRootPane.getScene().getWindow();

        boolean confirmed = UiUtils.showConfirmation(
                stage,
                "Delete User",
                "Are you sure you want to delete user '" + user.getUsername() + "'?");

        if (confirmed) {
            Task<Boolean> deleteTask = new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    return userDao.deleteUser(user.getId());
                }
            };

            deleteTask.setOnSucceeded(e -> {
                showToast((Stage) portCombo.getScene().getWindow(),
                        portCombo,
                        "User deleted successfully!",
                        true);
                loadUsersList(); // Refresh the list
            });

            deleteTask.setOnFailed(e -> {
                Throwable ex = deleteTask.getException();
                showToast((Stage) portCombo.getScene().getWindow(),
                        portCombo,
                        "Failed to delete user: " + ex.getMessage(),
                        false);
            });

            MainApp.getExecutorService().submit(deleteTask);
        }
    }

    private boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == Role.ADMIN;
    }

    @FXML
    private void save() {
        if (configDao == null) {
            showToast((Stage) scaleSlider.getScene().getWindow(),
                    scaleSlider,
                    "Configuration DAO not initialized!",
                    false);
            return;
        }

        try {
            SerialConfig cfg = new SerialConfig();
            if (isAdmin()) {
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
            }

            double scaleFactor = scaleSlider.getValue();

            Task<Void> saveTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    if (isAdmin()) {
                        configDao.saveSerialConfig(cfg);
                    }
                    configDao.saveUiScaleFactor(scaleFactor);
                    if (startupCheckBox != null) {
                        SystemUtils.setRunOnStartup(startupCheckBox.isSelected());
                    }
                    return null;
                }
            };

            saveTask.setOnSucceeded(e -> {
                if (mainController != null) {
                    mainController.reloadWithScale(scaleFactor);
                    showToast((Stage) scaleSlider.getScene().getWindow(),
                            scaleSlider,
                            "Settings saved successfully!",
                            true);
                } else {
                    showAlert("Settings",
                            "Settings saved successfully!\nPlease restart the application for UI scale to take full effect.");
                }
                close();
            });

            saveTask.setOnFailed(e -> {
                Throwable ex = saveTask.getException();
                ex.printStackTrace();
                showToast((Stage) scaleSlider.getScene().getWindow(),
                        scaleSlider,
                        "Failed to save settings: " + ex.getMessage(),
                        false);
            });

            MainApp.getExecutorService().submit(saveTask);

        } catch (Exception e) {
            System.err.println("Failed to save settings: " + e.getMessage());
            e.printStackTrace();
            showToast((Stage) scaleSlider.getScene().getWindow(),
                    scaleSlider,
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
