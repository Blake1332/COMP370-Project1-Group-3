package raft_demo;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logs events with timestamps to both console and file.
 */
public class Logger {
    private final String serverId;
    private final PrintWriter fileWriter;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    public Logger(String serverId, String logFilePath) throws IOException {
        this.serverId = serverId;
        this.fileWriter = new PrintWriter(new FileWriter(logFilePath, true), true);
    }
    
    /**
     * Logs a message with timestamp and server ID.
     */
    public synchronized void log(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logEntry = String.format("[%s] [%s] %s", timestamp, serverId, message);
        
        // Write to console
        System.out.println(logEntry);
        
        // Write to file
        fileWriter.println(logEntry);
    }
    
    public synchronized void close() {
    if (fileWriter != null) {
        fileWriter.close();
    }
}
}