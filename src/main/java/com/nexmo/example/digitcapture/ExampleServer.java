package com.nexmo.example.digitcapture;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.cluster.sharding.ShardCoordinator.LeastShardAllocationStrategy;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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
import com.nexmo.example.digitcapture.formatter.AppInfoLoggerFormatter;
import com.nexmo.example.digitcapture.formatter.LoggerFormatter;
import com.nexmo.example.digitcapture.migrate.MigrationMessage;
import com.nexmo.example.digitcapture.shutdown.GracefulShutdownInitiator;

public class ExampleServer {

    public static void main(String[] args) throws Exception {
        Config config = ConfigFactory.load();

        final String applicationName = config.getString("application.name");

        final ActorSystem system = ActorSystem.create(applicationName, config);

        final ClusterShardingSettings settings = ClusterShardingSettings.create(system);

        final LeastShardAllocationStrategy strategy = new LeastShardAllocationStrategy(
                settings.tuningParameters().leastShardAllocationRebalanceThreshold(),
                settings.tuningParameters().leastShardAllocationMaxSimultaneousRebalance());

        final String clusterHost = config.getString("application.clustering.host");
        final String clusterPort = config.getString("application.clustering.port");
        final LoggerFormatter formatter = new AppInfoLoggerFormatter(clusterHost,
                                                                     clusterPort,
                                                                     applicationName);

        final ActorRef clusterSharder = ClusterSharding.get(system)
                                                       .start("digit",
                                                              Conversation.props(formatter),
                                                              settings,
                                                              new ConversationMessageExtractor(),
                                                              strategy,
                                                              MigrationMessage.Migrate);


        final ResourceConfig resourceConfig = setUpResources(clusterSharder);

        final ActorRef shutdownActor =
                system.actorOf(GracefulShutdownInitiator.props(clusterSharder, 10000), "shutdownActor");
        final ServerSupplier serverSupplier = new ServerSupplier();
        ShutdownResource shutdownResource = new ShutdownResource(shutdownActor, serverSupplier);
        resourceConfig.register(shutdownResource);

        final Server server = createServer(config, resourceConfig);

        serverSupplier.setServer(server);

        server.start();
        server.join();

        TimeUnit.SECONDS.sleep(10);

        server.destroy();
    }

    private static ResourceConfig setUpResources(ActorRef clusterSharder) {
        ResourceConfig resourceConfig = new ResourceConfig();

        HelloWorld helloResource = new HelloWorld();
        resourceConfig.register(helloResource);

        ConversationResource conversationResource = new ConversationResource(clusterSharder);
        resourceConfig.register(conversationResource);

        return resourceConfig;
    }

    private static Server createServer(Config config, ResourceConfig resourceConfig) {
        final String host = config.getString("application.http.host");
        final int port = config.getInt("application.http.port");

        URI baseURI = UriBuilder.fromUri("http://" + host).port(port).build();

        return JettyHttpContainerFactory.createServer(baseURI, resourceConfig, false);
    }

    static class ServerSupplier implements Supplier<Server> {
        private Server server;

        @Override
        public Server get() {
            return server;
        }

        public void setServer(Server server) {
            this.server = server;
        }
    }

}
