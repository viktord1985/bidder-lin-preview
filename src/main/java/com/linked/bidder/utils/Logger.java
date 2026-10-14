package com.linked.bidder.utils;

import org.apache.commons.logging.LogFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

import static com.linked.bidder.MyApplicationKt.appConfig;
import static org.apache.commons.io.FileUtils.copyFile;

public class Logger {

    public static void init() {
        LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
        try {
            Files.createDirectories(Paths.get(appConfig.exportFolderPath));
            clearLogToFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final String logFileName = appConfig.exportFolderPath + appConfig.applicationLogPath; // Change the file name as needed

    public static void logInfo(String message) {
        log("INFO", message);
    }

    public static void logWarning(String message) {
        log("WARNING", message);
    }

    public static void logError(String message) {
        log("ERROR", message);
    }

    public static void logDebug(String message) {
        log("DEBUG", message);
    }

    private static void log(String level, String message) {
        String dateTime = getFormattedDateTime();
        String logEntry = "[" + dateTime + "] [" + level + "] " + message;

        // Print the log entry to the console
        System.out.println(logEntry);

        // Save the log entry to a file
        saveLogToFile(logEntry);
    }

    private static String getFormattedDateTime() {
        Date now = new Date();
        return dateFormat.format(now);
    }

    private static final int LOG_FILE_SIZE = 6; //MB

    // Note: clearing the file instead of limiting the line count, because {truncateLogFile} is very slow
    private static synchronized void clearLogToFile() {
        File logFile = new File(logFileName);
        boolean needClean = false;
        if (logFile.exists()) {
            long size = logFile.length() / (1024 * 1024);
            if (size >= LOG_FILE_SIZE) {
                needClean = true;
                // Create a timestamped backup
                String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date());
                File backupFile = new File(logFile.getParent(), logFile.getName() + "." + timestamp + ".bak");

                try {
                    copyFile(logFile, backupFile);
                } catch (IOException e) {
                    System.err.println("Failed to create a backup of the log file:");
                    e.printStackTrace();
                }
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFileName, !needClean))) {
            writer.write("[New log file]");
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static synchronized void saveLogToFile(String logEntry) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFileName, true))) {
            writer.write(logEntry);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
