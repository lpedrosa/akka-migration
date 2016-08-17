package com.nexmo.example.digitcapture.conversation.message;

import java.io.Serializable;

public final class Event implements Serializable {

    private final String id;
    private final String conversationId;
    private final String type;

    public Event(String id, String conversationId, String type) {
        this.id = id;
        this.conversationId = conversationId;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getType() {
        return type;
    }

}
