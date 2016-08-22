package com.nexmo.example.digitcapture.formatter;

public class AppInfoLoggerFormatter implements LoggerFormatter {

    private final String baseMessage;

    public AppInfoLoggerFormatter(String host, String port, String systemName) {
        this.baseMessage = String.format("[Host: %s, Port: %s][System: %s]", host, port, systemName);
    }

    @Override
    public String format(String logMessage) {
        return String.format("%s => %s", this.baseMessage, logMessage);
    }
}
