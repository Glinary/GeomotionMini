package com.mobdeve.s11.mco3.mco3javaversion;

public class RecordingModel {
    String date;
    String timestamp;

    public RecordingModel(String date, String timestamp) {
        this.date = date;
        this.timestamp = timestamp;
    }

    public String getDate() {
        return date;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
