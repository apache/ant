package org.example.junitlauncher.vintage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class JUnit4SampleTest {

    @Test
    public void testFoo() {
        assertEquals(1, 1);
    }

    @Test
    public void testBar() {
        assertTrue(true);
    }

    @Test
    public void testFooBar() {
        assertFalse(false);
    }
}
