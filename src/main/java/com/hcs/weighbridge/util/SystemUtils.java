package com.hcs.weighbridge.util;

import org.apache.logging.log4j.Logger;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import javafx.application.Platform;

public class SystemUtils {
    private static final Logger logger = LogUtil.getLogger(SystemUtils.class);
    private static final String REG_KEY = "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String APP_NAME = "HCSWeighbridge";

    /**
     * Set whether the application should run on Windows startup.
     * Uses 'reg add' or 'reg delete' commands.
     */
    public static void setRunOnStartup(boolean enabled) {
        try {
            if (enabled) {
                String jarPath = new File(SystemUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                        .getPath();
                String command = String.format("reg add \"%s\" /v \"%s\" /t REG_SZ /d \"\\\"%s\\\"\" /f", REG_KEY,
                        APP_NAME, jarPath);
                executeCommand(command);
                logger.info("Enabled run on startup with JAR path: {}", jarPath);
            } else {
                String command = String.format("reg delete \"%s\" /v \"%s\" /f", REG_KEY, APP_NAME);
                executeCommand(command);
                logger.info("Disabled run on startup");
            }
        } catch (Exception e) {
            logger.error("Failed to set run on startup: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if the application is set to run on startup.
     */
    public static boolean isRunOnStartupEnabled() {
        try {
            String command = String.format("reg query \"%s\" /v \"%s\"", REG_KEY, APP_NAME);
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            logger.error("Failed to check run on startup status: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Shut down the computer with a delay of 5 seconds.
     * @throws Exception
     */
    public static void shutdownPC() throws Exception {
        logger.info("Triggering system shutdown...");
        executeCommand("shutdown /s /t 5");
    }

    private static void executeCommand(String command) throws Exception {
        Process process = Runtime.getRuntime().exec(command);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.debug(line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                StringBuilder errorMsg = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    errorMsg.append(line).append("\n");
                }
                logger.error("Command failed with exit code {}: {}", exitCode, errorMsg.toString());
            }
        }
    }
}
