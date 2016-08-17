package com.nexmo.example.digitcapture.shutdown;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Scheduler;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.cluster.sharding.ShardRegion;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

import com.nexmo.example.digitcapture.shutdown.message.GracefulShutdownInitiatorMessages;

public class GracefulShutdownInitiator extends UntypedActor {

    public static Props props(final ActorRef clusterSharding, final long gracePeriod) {
        return Props.create(GracefulShutdownInitiator.class,
                            () -> new GracefulShutdownInitiator(clusterSharding, gracePeriod));
    }

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private final ActorRef clusterSharding;
    private final long gracePeriod;

    private GracefulShutdownInitiator(ActorRef clusterSharding, long gracePeriod) {
        this.clusterSharding = clusterSharding;
        this.gracePeriod = gracePeriod;
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message == GracefulShutdownInitiatorMessages.ShutdownGracefully) {
            log.info("Shutting down the system gracefully!");
            context().watch(this.clusterSharding);
            this.clusterSharding.tell(ShardRegion.gracefulShutdownInstance(), getSelf());
        } else if (message == GracefulShutdownInitiatorMessages.LeftTheCluster) {
            log.info("Left the cluster. Stopping the actor system in {} ms", this.gracePeriod);
            final Scheduler scheduler = getContext().system().scheduler();
            scheduler.scheduleOnce(Duration.create(gracePeriod, TimeUnit.MILLISECONDS),
                    getSelf(),
                    GracefulShutdownInitiatorMessages.StopTheSystem,
                    getContext().dispatcher(),
                    ActorRef.noSender());
        } else if (message == GracefulShutdownInitiatorMessages.StopTheSystem) {
            log.info("Stopping the actor system! Bye!");
            getContext().system().terminate();
        } else if (message instanceof Terminated) {
            final Cluster cluster = Cluster.get(getContext().system());
            final ActorRef self = getSelf();
            cluster.registerOnMemberRemoved(
                    () -> self.tell(GracefulShutdownInitiatorMessages.LeftTheCluster, ActorRef.noSender()));
            cluster.leave(cluster.selfAddress());
        } else {
            unhandled(message);
        }
    }

}
