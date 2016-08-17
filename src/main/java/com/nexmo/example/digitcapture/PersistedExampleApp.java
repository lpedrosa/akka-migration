package com.nexmo.example.digitcapture;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.PatternsCS;
import akka.util.Timeout;
import scala.concurrent.Await;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import com.nexmo.example.digitcapture.capturer.DigitCapturer;
import com.nexmo.example.digitcapture.capturer.message.DigitPressed;
import com.nexmo.example.digitcapture.capturer.message.GetSum;

public class PersistedExampleApp {

    private static final Logger LOG = LoggerFactory.getLogger(PersistedExampleApp.class);

    public static void main(String[] args) throws Exception {
        final Config config = ConfigFactory.load();
        final String applicationName = config.getString("application.name");

        final ActorSystem system = ActorSystem.create(applicationName, config);

        final Collection<String> appIds = config.getStringList("application.ids");
        final Collection<ActorRef> refs = appIds.stream()
                                                .map(id -> createDigitCapturer(system, id))
                                                .collect(Collectors.toList());

        refs.forEach(PersistedExampleApp::doStuffToActor);
        Thread.sleep(2000);

        final String id = killFirstAndPrintPath(refs);
        Thread.sleep(2000);

        ActorRef resurrected = createDigitCapturer(system, id);
        final CompletionStage<Object> reply = PatternsCS.ask(resurrected,
                                                             GetSum.getInstance(),
                                                             Timeout.apply(3000, TimeUnit.MILLISECONDS));

        reply.thenAccept(sum -> {
            LOG.debug("Sum for resurrected actor [{}] is {}",
                      resurrected.path().name(),
                      sum);
        });

        final Duration duration = config.getDuration("application.cooldown");

        LOG.info("Sleeping for {}", duration);
        Thread.sleep(duration.toMillis());

        LOG.info("Dying...");
        Await.result(system.terminate(),
                     scala.concurrent.duration.Duration.apply(500, TimeUnit.MILLISECONDS));
    }

    private static ActorRef createDigitCapturer(ActorSystem system, String id) {
        return system.actorOf(DigitCapturer.props(id), DigitCapturer.name(id));
    }

    private static void doStuffToActor(ActorRef ref) {
        ref.tell(new DigitPressed("1"), ActorRef.noSender());
        ref.tell(new DigitPressed("3"), ActorRef.noSender());
        ref.tell(new DigitPressed("5"), ActorRef.noSender());

        final CompletionStage<Object> reply = PatternsCS.ask(ref,
                                                             GetSum.getInstance(),
                                                             Timeout.apply(3000, TimeUnit.MILLISECONDS));

        final String path = ref.path().name();

        reply.handle((Object sum, Throwable throwable) -> {
            if (throwable != null) {
                LOG.error("Asking for sum blew up for actor [{}]", path);
            }
            LOG.debug("Sum for actor [{}] is: {}", path, sum);
            return null;
        });
    }

    private static String killFirstAndPrintPath(Collection<ActorRef> refs) {
        final ActorRef ref = new ArrayDeque<>(refs).pollFirst();
        final String path = ref.path().name();

        final CompletionStage<Boolean> done = PatternsCS.gracefulStop(ref,
                scala.concurrent.duration.Duration.create(500, TimeUnit.MILLISECONDS));

        done.thenAccept(success -> LOG.info("Killed actor with path [{}]", path));
        return "1";
    }

}
