package com.semi.jSimul.collections;

/**
 * Exception thrown when an active request is preempted by a higher-priority request.
 *
 * @author waiting
 * @date 2025/11/29
 */
public final class Preempted extends RuntimeException {
    private final PreemptiveRequest preemptor;

    public Preempted(PreemptiveRequest preemptor) {
        super("Preempted by request priority=" + preemptor.priority + " order=" + preemptor.order);
        this.preemptor = preemptor;
    }

    public PreemptiveRequest preemptor() {
        return preemptor;
    }
}
