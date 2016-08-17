package com.nexmo.example.digitcapture.conversation.message;

import java.io.Serializable;

public final class SetupConversation implements Serializable {
    private final String conversationId;

    public SetupConversation(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getConversationId() {
        return conversationId;
    }
}
