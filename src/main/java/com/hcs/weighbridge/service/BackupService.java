package com.hcs.weighbridge.service;

import com.hcs.weighbridge.config.DatabaseConfig;
import com.hcs.weighbridge.dao.ConfigDao;
import com.hcs.weighbridge.util.AppException;
import com.hcs.weighbridge.util.LogUtil;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

public class BackupService {

    private static final Logger logger = LogUtil.getLogger(BackupService.class);
    private static final String BACKUP_DIR = "backups";
    private final ConfigDao configDao;
    private final Connection connection;

    public BackupService(Connection connection, ConfigDao configDao) {
        this.connection = connection;
        this.configDao = configDao;
        ensureBackupDirExists();
    }

    private void ensureBackupDirExists() {
        File dir = new File(BACKUP_DIR);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                logger.info("Created backup directory: {}", dir.getAbsolutePath());
            } else {
                logger.error("Failed to create backup directory: {}", dir.getAbsolutePath());
            }
        }
    }

    public void performBackup() throws Exception {
        logger.info("Starting database backup...");
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = "backup_" + timestamp + ".sql";
        File backupFile = new File(BACKUP_DIR, filename);

        try (FileWriter writer = new FileWriter(backupFile)) {
            exportTable(writer, "users");
            exportTable(writer, "app_config");
            exportTable(writer, "weigh_data");

            logger.info("Backup completed successfully: {}", backupFile.getAbsolutePath());
            configDao.setLastBackupDate(LocalDate.now().toString());
        } catch (IOException e) {
            throw new AppException("Database backup failed: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new AppException("Unexpected error during backup: " + e.getMessage(), e);
        }
    }

    private void exportTable(FileWriter writer, String tableName) throws Exception {
        writer.write("-- Table: " + tableName + "\n");
        String query = "SELECT * FROM " + tableName;

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            int columnCount = rs.getMetaData().getColumnCount();

            while (rs.next()) {
                StringBuilder sb = new StringBuilder();
                sb.append("INSERT IGNORE INTO ").append(tableName).append(" VALUES (");

                for (int i = 1; i <= columnCount; i++) {
                    if (i > 1)
                        sb.append(", ");

                    Object value = rs.getObject(i);
                    if (value == null) {
                        sb.append("NULL");
                    } else if (value instanceof Number) {
                        sb.append(value);
                    } else {
                        sb.append("'").append(value.toString().replace("'", "\\'")).append("'");
                    }
                }
                sb.append(");\n");
                writer.write(sb.toString());
            }
        }
        writer.write("\n");
    }

    public void restoreFromBackup(File backupFile) throws Exception {
        logger.info("Restoring from backup: {}", backupFile.getName());

        if (DatabaseConfig.isTableEmpty("weigh_data")) {
            logger.info("weigh_data table is empty. Proceeding with full restore.");
            DatabaseConfig.executeScript(backupFile.getAbsolutePath());
        } else {
            logger.warn("weigh_data is NOT empty. Skipping restore to prevent data overwrite.");

            restoreSelectively(backupFile);
        }
    }

    private void restoreSelectively(File backupFile) {
        boolean skipWeighData = !DatabaseConfig.isTableEmpty("weigh_data");
        logger.info("Selective restore started. Skipping weigh_data: {}", skipWeighData);

        try (java.util.Scanner scanner = new java.util.Scanner(backupFile)) {
            try (Statement stmt = connection.createStatement()) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if (line.isEmpty() || line.startsWith("--"))
                        continue;

                    if (skipWeighData && line.toUpperCase().startsWith("INSERT IGNORE INTO WEIGH_DATA")) {
                        continue;
                    }

                    try {
                        stmt.execute(line);
                    } catch (Exception e) {
                        logger.warn("Failed to execute line: {}", line, e);
                    }
                }
            } catch (SQLException e) {
                throw new AppException("SQL error during selective restore", e);
            }
        } catch (IOException e) {
            throw new AppException("Failed to read backup file", e);
        }
    }

    public void autoRestoreIfEnabled() {
        if (!configDao.isAutoRestoreEnabled()) {
            logger.info("Auto-restore is disabled.");
            return;
        }

        if (!DatabaseConfig.isTableEmpty("weigh_data")) {
            logger.info("Database is not empty. Auto-restore skipped.");
            return;
        }

        logger.info("Database (weigh_data) is empty. Attempting auto-restore...");
        File latestBackup = getLatestBackup();
        if (latestBackup != null) {
            try {
                restoreFromBackup(latestBackup);
                logger.info("Auto-restore completed from {}", latestBackup.getName());
            } catch (Exception e) {
                logger.error("Auto-restore failed", e);
            }
        } else {
            logger.info("No backup files found for auto-restore.");
        }
    }

    public void checkAndRunScheduledBackup() {
        String frequency = configDao.getBackupFrequency();
        String lastBackupDateStr = configDao.getLastBackupDate();

        if (lastBackupDateStr == null) {
            try {
                performBackup();
            } catch (Exception e) {
                logger.error("Initial backup failed", e);
            }
            return;
        }

        LocalDate lastBackup = LocalDate.parse(lastBackupDateStr);
        LocalDate today = LocalDate.now();
        long daysSince = ChronoUnit.DAYS.between(lastBackup, today);

        boolean shouldBackup = false;
        switch (frequency) {
            case "Daily":
                shouldBackup = daysSince >= 1;
                break;
            case "Weekly":
                shouldBackup = daysSince >= 7;
                break;
            case "Monthly":
                shouldBackup = daysSince >= 30;
                break;
        }

        if (shouldBackup) {
            logger.info("Scheduled backup due (Frequency: {}, Last: {}). Executing...", frequency, lastBackup);
            try {
                performBackup();
            } catch (Exception e) {
                logger.error("Scheduled backup failed", e);
            }
        }
    }

    public File getLatestBackup() {
        File dir = new File(BACKUP_DIR);
        if (!dir.exists() || !dir.isDirectory())
            return null;

        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".sql"));
        if (files == null || files.length == 0)
            return null;

        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        return files[0];
    }
}
