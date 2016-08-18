package com.nexmo.example.digitcapture.conversation;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.nexmo.example.digitcapture.conversation.message.ConversationMessages;
import com.nexmo.example.digitcapture.conversation.message.Event;
import com.nexmo.example.digitcapture.conversation.message.EventsHandled;
import com.nexmo.example.digitcapture.conversation.message.SetupConversation;
import com.nexmo.example.digitcapture.migrate.MigrationMessage;

public class Conversation extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public static Props props() {
        return Props.create(Conversation.class, Conversation::new);
    }

    private int eventsHandled = 0;

    private String conversationId;

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof SetupConversation) {
            SetupConversation setupInfo = ((SetupConversation)message);
            this.conversationId = setupInfo.getConversationId();
        } else if (message instanceof Event) {
            handleEvent((Event) message);
        } else if (message == ConversationMessages.GetEventsHandled) {
            replyWithEventsHandled();
        } else if (message == MigrationMessage.Migrate) {
            handleMigration();
        } else {
            unhandled(message);
        }
    }

    private void replyWithEventsHandled() {
        getSender().tell(new EventsHandled(this.eventsHandled), getSelf());
    }

    private void handleEvent(Event event) {
        if (this.conversationId == null) {
            this.conversationId = event.getConversationId();
        }
        log.info("Handling event: {}", event);
        this.eventsHandled++;
    }

    private void handleMigration() {
        // TODO(lpedrosa): store state into a file
        // sending normal PoisonPill for now
        getSelf().tell(PoisonPill.getInstance(), ActorRef.noSender());
    }
}
