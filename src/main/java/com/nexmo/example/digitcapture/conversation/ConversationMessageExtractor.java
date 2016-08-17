package com.nexmo.example.digitcapture.conversation;

import akka.cluster.sharding.ShardRegion.MessageExtractor;

import com.nexmo.example.digitcapture.conversation.message.ConversationEnvelope;

public class ConversationMessageExtractor implements MessageExtractor {
    @Override
    public String entityId(Object message) {
        if (message instanceof ConversationEnvelope) {
            return ((ConversationEnvelope) message).getConversationId();
        }

        throw new UnsupportedOperationException("could not route this message: " + message);
    }

    @Override
    public Object entityMessage(Object message) {
        if (message instanceof ConversationEnvelope) {
            return ((ConversationEnvelope) message).getPayload();
        }

        throw new UnsupportedOperationException("could not route this message: " + message);
    }

    @Override
    public String shardId(Object message) {
        if (message instanceof ConversationEnvelope) {
            int numberOfShards = 10;
            String conversationId = ((ConversationEnvelope) message).getConversationId();
            return String.valueOf(conversationId.codePointAt(conversationId.length() - 1) % numberOfShards);
        }

        throw new UnsupportedOperationException("could not shard this message: " + message);
    }
}
