package com.semi.jSimul.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.semi.jSimul.collections.PriorityRequest;
import com.semi.jSimul.collections.PriorityResource;
import com.semi.jSimul.collections.Request;
import com.semi.jSimul.collections.Resource;
import com.semi.jSimul.collections.Store;
import com.semi.jSimul.collections.StoreGet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Integration-style POC tests that combine Process, Resource, and Store.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public class LogisticsPocIntegrationTest {

    @Test
    void transportPipelineUsesResourceAndStore() throws Exception {
        Environment env = new Environment();
        Resource controlPoint = new Resource(env, 1);
        Store<String> buffer = new Store<>(env, 1);

        AtomicReference<Double> producerAcquire = new AtomicReference<>();
        AtomicReference<Double> producerRelease = new AtomicReference<>();
        AtomicReference<Double> consumerGot = new AtomicReference<>();

        Process producer =
                env.process(
                        ctx -> {
                            ctx.await(env.timeout(1.0));
                            Request req = controlPoint.request();
                            ctx.await(req);
                            producerAcquire.set(env.now());
                            ctx.await(env.timeout(1.0));
                            ctx.await(buffer.put("box"));
                            ctx.await(controlPoint.release(req));
                            producerRelease.set(env.now());
                            return "producer";
                        });

        Process consumer =
                env.process(
                        ctx -> {
                            ctx.await(env.timeout(1.2));
                            Request req = controlPoint.request();
                            ctx.await(req);
                            StoreGet<String> get = buffer.get();
                            Object item = ctx.await(get);
                            consumerGot.set(env.now());
                            ctx.await(controlPoint.release(req));
                            return item;
                        });

        env.run();
        assertEquals(1.0, producerAcquire.get(), 1e-9);
        assertEquals(2.0, producerRelease.get(), 1e-9);
        assertEquals(2.0, consumerGot.get(), 1e-9);
        assertEquals(2.0, env.now(), 1e-9);
    }

    @Test
    void priorityResourceHonorsQueueOrderInsideProcesses() throws Exception {
        Environment env = new Environment();
        PriorityResource resource = new PriorityResource(env, 1);
        List<String> order = new ArrayList<>();

        Process holder =
                env.process(
                        ctx -> {
                            PriorityRequest req = resource.request(1);
                            ctx.await(req);
                            ctx.await(env.timeout(1.0));
                            ctx.await(resource.release(req));
                            return "holder";
                        });

        Process low =
                env.process(
                        ctx -> {
                            ctx.await(env.timeout(0.1));
                            PriorityRequest req = resource.request(5);
                            ctx.await(req);
                            order.add("low@" + env.now());
                            ctx.await(resource.release(req));
                            return "low";
                        });

        Process high =
                env.process(
                        ctx -> {
                            ctx.await(env.timeout(0.2));
                            PriorityRequest req = resource.request(0);
                            ctx.await(req);
                            order.add("high@" + env.now());
                            ctx.await(env.timeout(0.1));
                            ctx.await(resource.release(req));
                            return "high";
                        });

        env.run();
        assertEquals(List.of("high@1.0", "low@1.1"), order);
        assertEquals(1.1, env.now(), 1e-9);
    }
}
