package com.nexmo.example.digitcapture.conversation;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import com.nexmo.example.digitcapture.conversation.message.ConversationMessages;
import com.nexmo.example.digitcapture.conversation.message.Event;
import com.nexmo.example.digitcapture.conversation.message.EventsHandled;
import com.nexmo.example.digitcapture.conversation.message.SetupConversation;
import com.nexmo.example.digitcapture.formatter.LoggerFormatter;
import com.nexmo.example.digitcapture.migrate.MigrationMessage;

public class Conversation extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private final LoggerFormatter formatter;

    public static Props props(LoggerFormatter formatter) {
        return Props.create(Conversation.class, () -> new Conversation(formatter));
    }

    public Conversation(LoggerFormatter formatter) {
        this.formatter = formatter;
        this.eventsHandled = 0;
    }

    private int eventsHandled;
    private String conversationId;

    @Override
    public void preStart() throws Exception {
        this.conversationId = getSelf().path().name();

        // reload state
        Path path = Paths.get("./conversation-mem-"+this.conversationId);
        if (Files.exists(path)) {
            List<String> state = Files.readAllLines(path);
            state.forEach(line -> this.eventsHandled = Integer.valueOf(line));
        }

        super.preStart();
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof SetupConversation) {
            SetupConversation setupInfo = ((SetupConversation)message);
            //this.conversationId = setupInfo.getConversationId();
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
        log.info(formatter.format("Handling event: {}" + event));
        this.eventsHandled++;
    }

    private void handleMigration() {
        Path path = Paths.get("./conversation-mem-"+this.conversationId);
        try {
            Files.deleteIfExists(path);
            Files.write(path, String.valueOf(this.eventsHandled).getBytes(), StandardOpenOption.CREATE);
        } catch (IOException ex) {
            log.error("Failed to persist state [cid:{}]", this.conversationId);
        }

        getContext().stop(getSelf());
    }
}
