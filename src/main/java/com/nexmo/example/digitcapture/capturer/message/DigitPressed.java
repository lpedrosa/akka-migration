package com.nexmo.example.digitcapture.capturer.message;

public final class DigitPressed {
    private final String digit;

    public DigitPressed(String digit) {
        this.digit = digit;
    }

    public String getDigit() {
        return digit;
    }
}
