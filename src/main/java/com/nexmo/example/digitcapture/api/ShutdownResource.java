package com.nexmo.example.digitcapture.api;

import akka.actor.ActorRef;

import java.util.function.Supplier;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.eclipse.jetty.server.Server;

import com.nexmo.example.digitcapture.shutdown.message.GracefulShutdownInitiatorMessages;

@Path("/shutdown")
public class ShutdownResource {

    private final ActorRef shutdownActor;
    private final Supplier<Server> server;

    public ShutdownResource(ActorRef shutdownActor, Supplier<Server> server) {
        this.shutdownActor = shutdownActor;
        this.server = server;
    }

    @POST
    public Response shutdownGracefully() {
        this.shutdownActor.tell(GracefulShutdownInitiatorMessages.ShutdownGracefully, ActorRef.noSender());

        new Thread(() -> {
            try {
                server.get().stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        return Response.ok().build();
    }
}
