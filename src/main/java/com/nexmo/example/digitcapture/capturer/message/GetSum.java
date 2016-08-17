package com.nexmo.example.digitcapture.capturer.message;

public final class GetSum {
    private static final GetSum instance = new GetSum();

    private GetSum() {

    }

    public static final GetSum getInstance() {
        return instance;
    }
}
