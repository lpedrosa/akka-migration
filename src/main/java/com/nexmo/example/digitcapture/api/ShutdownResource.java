package com.nexmo.example.digitcapture.api;

import akka.actor.ActorRef;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import com.nexmo.example.digitcapture.shutdown.message.GracefulShutdownInitiatorMessages;

@Path("/shutdown")
public class ShutdownResource {

    private final ActorRef shutdownActor;

    public ShutdownResource(ActorRef shutdownActor) {
        this.shutdownActor = shutdownActor;
    }

    @POST
    public Response shutdownGracefully() {
        this.shutdownActor.tell(GracefulShutdownInitiatorMessages.ShutdownGracefully, ActorRef.noSender());
        return Response.ok().build();
    }
}
