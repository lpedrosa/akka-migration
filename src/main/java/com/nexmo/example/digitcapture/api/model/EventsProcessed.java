package com.nexmo.example.digitcapture.api.model;

public class EventsProcessed {

    private final long eventsProcessed;

    public EventsProcessed(long eventsProcessed) {
        this.eventsProcessed = eventsProcessed;
    }

    public long getEventsProcessed() {
        return eventsProcessed;
    }

}
