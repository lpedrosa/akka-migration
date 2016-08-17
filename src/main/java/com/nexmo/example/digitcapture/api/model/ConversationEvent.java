package com.nexmo.example.digitcapture.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ConversationEvent {

    private final String id;
    private final String conversationId;
    private final String type;

    @JsonCreator
    public ConversationEvent(@JsonProperty("id") String id,
                             @JsonProperty("conversationId") String conversationId,
                             @JsonProperty("type") String type) {
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
