package com.semi.jSimul.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Tests ConditionValue behaving like a dict (ordering, equality, iterators).
 */
public class ConditionValueDictCompatTest {

    @Test
    void keysValuesItemsPreserveOrder() {
        Environment env = new Environment();
        Timeout a = env.timeout(1.0, "A");
        Timeout b = env.timeout(1.0, "B");

        SimEvent all = env.allOf(a, b);
        env.run(all);

        ConditionValue cv = (ConditionValue) all.asEvent().value();
        assertIterableEquals(List.of(a.asEvent(), b.asEvent()), cv.events());
        assertIterableEquals(List.of("A", "B"), cv.values());

        List<Map.Entry<Event, Object>> items = new java.util.ArrayList<>();
        cv.items().forEach(items::add);
        assertEquals(a.asEvent(), items.get(0).getKey());
        assertEquals("A", items.get(0).getValue());
        assertEquals(b.asEvent(), items.get(1).getKey());
        assertEquals("B", items.get(1).getValue());
    }

    @Test
    void equalsMapWithSameEntries() {
        Environment env = new Environment();
        Timeout a = env.timeout(1.0, "A");
        Timeout b = env.timeout(1.0, "B");
        SimEvent all = env.allOf(a, b);
        env.run(all);
        ConditionValue cv = (ConditionValue) all.asEvent().value();

        Map<Event, Object> expected = new LinkedHashMap<>();
        expected.put(a.asEvent(), "A");
        expected.put(b.asEvent(), "B");

        assertEquals(expected, cv.toMap());
        assertEquals(cv, expected);
    }
}
