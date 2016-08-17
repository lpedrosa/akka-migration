package com.nexmo.example.digitcapture.api;

import akka.actor.ActorRef;
import akka.pattern.PatternsCS;
import akka.util.Timeout;

import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nexmo.example.digitcapture.api.model.Conversation;
import com.nexmo.example.digitcapture.api.model.ConversationEvent;
import com.nexmo.example.digitcapture.api.model.EventsProcessed;
import com.nexmo.example.digitcapture.conversation.message.ConversationEnvelope;
import com.nexmo.example.digitcapture.conversation.message.ConversationMessages;
import com.nexmo.example.digitcapture.conversation.message.Event;
import com.nexmo.example.digitcapture.conversation.message.EventsHandled;
import com.nexmo.example.digitcapture.conversation.message.SetupConversation;

@Path("/conversation")
public class ConversationResource {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationResource.class);

    private final ActorRef conversationSharder;

    public ConversationResource(ActorRef conversationSharder) {
        this.conversationSharder = conversationSharder;
    }

    @POST
    @Path("/event")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response publishEvent(ConversationEvent conversationEvent) {
        final String conversationId = conversationEvent.getConversationId();
        LOG.info("Pushing events to actor [cid:{}]", conversationId);
        final Event event = new Event(conversationEvent.getId(), conversationEvent.getConversationId(), conversationEvent.getType());

        final ConversationEnvelope conversationEnvelope = new ConversationEnvelope(conversationId, event);

        this.conversationSharder.tell(conversationEnvelope, ActorRef.noSender());

        return Response.ok().build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Conversation create() {
        final String conversationId = UUID.randomUUID().toString();
        final SetupConversation setupConversation = new SetupConversation(conversationId);

        final ConversationEnvelope envelope = new ConversationEnvelope(conversationId, setupConversation);

        this.conversationSharder.tell(envelope, ActorRef.noSender());

        return new Conversation(conversationId);
    }

    @GET
    @Path("/{id}/events")
    @Produces(MediaType.APPLICATION_JSON)
    public void getEventsProcessed(@PathParam("id") String conversationId,
                                   @Suspended AsyncResponse asyncResponse) {

        ConversationMessages payload = ConversationMessages.GetEventsHandled;
        final ConversationEnvelope envelope = new ConversationEnvelope(conversationId, payload);

        Timeout timeout = Timeout.apply(1, TimeUnit.SECONDS);

        LOG.info("Requesting number of events handled from actor [cid:{}]", conversationId);
        final CompletionStage<Object> actorReply = PatternsCS.ask(this.conversationSharder, envelope, timeout);

        actorReply.thenApply(this::handleActorResponse)
                  .handle((eventProcessed, throwable) -> {
                      if (throwable != null) {
                          LOG.error("It barfed...", throwable);
                          asyncResponse.resume(throwable);
                      } else
                          asyncResponse.resume(eventProcessed);
                      return null;
                  });
    }

    private EventsProcessed handleActorResponse(Object msg) {
        LOG.debug("Handling actor response");
        EventsHandled eventsHandled = ((EventsHandled) msg);

        LOG.debug("Response handled!");
        return new EventsProcessed(eventsHandled.getNumberOfEvents());
    }
}
