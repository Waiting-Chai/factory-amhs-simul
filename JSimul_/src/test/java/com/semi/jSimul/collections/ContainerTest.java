package com.semi.jSimul.collections;

import static org.junit.jupiter.api.Assertions.*;

import com.semi.jSimul.core.Environment;
import org.junit.jupiter.api.Test;

/**
 * Test class for Container functionality.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class ContainerTest {
    @Test
    void putGetAmounts() {
        Environment env = new Environment();
        Container c = new Container(env, 100, 10);
        c.put(5);
        env.step();
        assertEquals(15.0, c.level(), 1e-9);
        GetEvent ge = c.get(8);
        env.step();
        assertEquals(8.0, ge.asEvent().value());
        assertEquals(7.0, c.level(), 1e-9);
    }

    @Test
    void negativeAmountsAreRejected() {
        Environment env = new Environment();
        Container c = new Container(env, 50, 10);
        assertThrows(IllegalArgumentException.class, () -> c.put(0));
        assertThrows(IllegalArgumentException.class, () -> c.put(-1));
        assertThrows(IllegalArgumentException.class, () -> c.get(0));
        assertThrows(IllegalArgumentException.class, () -> c.get(-5));
    }
}
