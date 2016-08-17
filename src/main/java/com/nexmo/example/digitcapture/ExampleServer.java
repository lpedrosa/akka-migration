package com.nexmo.example.digitcapture;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import org.eclipse.jetty.server.Server;

import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import com.nexmo.example.digitcapture.api.ConversationResource;
import com.nexmo.example.digitcapture.api.HelloWorld;
import com.nexmo.example.digitcapture.api.ShutdownResource;
import com.nexmo.example.digitcapture.conversation.Conversation;
import com.nexmo.example.digitcapture.conversation.ConversationMessageExtractor;
import com.nexmo.example.digitcapture.shutdown.GracefulShutdownInitiator;

public class ExampleServer {

    public static void main(String[] args) throws Exception {
        Config config = ConfigFactory.load();

        final String applicationName = config.getString("application.name");

        final ActorSystem system = ActorSystem.create(applicationName, config);

        final ClusterShardingSettings settings = ClusterShardingSettings.create(system);

        final ActorRef clusterSharder = ClusterSharding.get(system)
                                                           .start("digit",
                                                                   Conversation.props(),
                                                                   settings,
                                                                   new ConversationMessageExtractor());

        final ActorRef shutdownActor = system.actorOf(GracefulShutdownInitiator.props(clusterSharder, 10000), "shutdownActor");

        final ResourceConfig resourceConfig = setUpResources(clusterSharder, shutdownActor);

        final Server server = createServer(config, resourceConfig);
        server.join();
        server.destroy();
    }

    private static ResourceConfig setUpResources(ActorRef clusterSharder, ActorRef shutdownActor) {
        ResourceConfig resourceConfig = new ResourceConfig();

        HelloWorld helloResource = new HelloWorld();
        resourceConfig.register(helloResource);

        ConversationResource conversationResource = new ConversationResource(clusterSharder);
        resourceConfig.register(conversationResource);

        ShutdownResource shutdownResource = new ShutdownResource(shutdownActor);
        resourceConfig.register(shutdownResource);

        return resourceConfig;
    }

    private static Server createServer(Config config, ResourceConfig resourceConfig) {
        final String host = config.getString("application.http.host");
        final int port = config.getInt("application.http.port");

        URI baseURI = UriBuilder.fromUri("http://" + host).port(port).build();

        return JettyHttpContainerFactory.createServer(baseURI, resourceConfig, true);
    }

}
