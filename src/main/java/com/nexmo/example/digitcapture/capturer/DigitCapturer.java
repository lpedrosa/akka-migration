package com.nexmo.example.digitcapture.capturer;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.persistence.RecoveryCompleted;
import akka.persistence.UntypedPersistentActor;

import com.nexmo.example.digitcapture.capturer.message.DigitPressed;
import com.nexmo.example.digitcapture.capturer.message.GetSum;
import com.nexmo.example.digitcapture.capturer.message.IncrementedSum;

public class DigitCapturer extends UntypedPersistentActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public static final Props props(final String applicationId) {
        return Props.create(DigitCapturer.class, () -> new DigitCapturer(applicationId));
    }

    public static String name(String id) {
        return String.format("digit-capturer-%s", id);
    }

    private final String applicationId;

    private long sum;

    private DigitCapturer(String applicationId) {
        this.applicationId = applicationId;
        this.sum = 0;
    }

    @Override
    public String persistenceId() {
        return "digit-capturer-" + this.applicationId;
    }

    @Override
    public void onReceiveRecover(Object message) throws Throwable {
        if (message instanceof IncrementedSum) {
            updateState((IncrementedSum) message);
        } else if (message == RecoveryCompleted.getInstance()) {

        }
    }

    @Override
    public void onReceiveCommand(Object message) throws Throwable {
        if (message instanceof DigitPressed) {
            handleDigitPressed((DigitPressed) message);
        } else if (message == GetSum.getInstance()) {
            getSender().tell(this.sum, getSelf());
        } else {
            unhandled(message);
        }
    }

    private void handleDigitPressed(DigitPressed message) {
        final String digit = message.getDigit();

        if (log.isDebugEnabled())
            log.debug("[pid: {}] Handling digit [{}]", persistenceId(), digit);

        final IncrementedSum event = new IncrementedSum(Integer.parseInt(digit));

        persist(event, eventToPersist -> {
            if (log.isDebugEnabled())
                log.debug("[pid: {}]  digit [{}]", persistenceId(), digit);
            updateState(eventToPersist);
        });
    }

    private void updateState(IncrementedSum evt) {
        this.sum += evt.getValue();
    }

}
