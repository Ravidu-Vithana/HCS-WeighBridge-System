package com.hcs.weighbridge.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;

public final class LogUtil {

    private LogUtil() {}

    static {
        createLogsDirectory();
    }

    private static void createLogsDirectory() {
        File logsDir = new File("./logs");
        if (!logsDir.exists()) {
            if (logsDir.mkdirs()) {
                getLogger(LogUtil.class).info("Logs directory created: " + logsDir.getAbsolutePath());
            } else {
                System.err.println("Failed to create logs directory at: " + logsDir.getAbsolutePath());
            }
        }
    }

    public static Logger getLogger(Class<?> clazz) {
        return LogManager.getLogger(clazz);
    }

    public static void logError(Logger logger, String message, Throwable throwable, Object... params) {
        logger.error(message, params, throwable);

        if (throwable instanceof java.sql.SQLException) {
            Logger dbLogger = LogManager.getLogger("DatabaseError");
            dbLogger.error("DATABASE ERROR - " + message, params, throwable);
        }
    }

    public static void logError(Logger logger, String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    public static void logError(Logger logger, Throwable throwable) {
        logger.error("Exception occurred: ", throwable);
    }

    public static void logAndThrow(Logger logger, String message, Throwable throwable) {
        logger.error(message, throwable);
        throw new RuntimeException(message, throwable);
    }
}