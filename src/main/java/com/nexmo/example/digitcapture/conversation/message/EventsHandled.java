package com.nexmo.example.digitcapture.conversation.message;

import java.io.Serializable;

public final class EventsHandled implements Serializable {

    private final long numberOfEvents;

    public EventsHandled(long numberOfEvents) {
        this.numberOfEvents = numberOfEvents;
    }

    public long getNumberOfEvents() {
        return numberOfEvents;
    }

}
