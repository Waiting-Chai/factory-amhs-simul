package com.semi.jSimul.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Determinism and ordering checks for condition values.
 *
 * @author waiting
 * @date 2025/11/29
 */
public class ConditionDeterminismTest {

    @Test
    void allOfPreservesInputOrderWhenSimultaneous() {
        Environment env = new Environment();
        Timeout first = env.timeout(1.0, "first");
        Timeout second = env.timeout(1.0, "second");
        Timeout third = env.timeout(1.0, "third");

        SimEvent all = env.allOf(first, second, third);
        env.run(all);

        ConditionValue cv = (ConditionValue) all.asEvent().value();
        List<Event> order = new ArrayList<>();
        cv.events().forEach(order::add);

        assertEquals(List.of(first.asEvent(), second.asEvent(), third.asEvent()), order);
        assertEquals("first", cv.get(first.asEvent()));
        assertEquals("second", cv.get(second.asEvent()));
        assertEquals("third", cv.get(third.asEvent()));
    }
}
