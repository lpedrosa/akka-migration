package com.nexmo.example.digitcapture.conversation.message;

import java.io.Serializable;

public final class ConversationEnvelope implements Serializable {

    private final String conversationId;
    private final Object payload;

    public ConversationEnvelope(String conversationId, Object payload) {
        this.conversationId = conversationId;
        this.payload = payload;
    }

    public String getConversationId() {
        return this.conversationId;
    }

    public Object getPayload() {
        return this.payload;
    }

}
