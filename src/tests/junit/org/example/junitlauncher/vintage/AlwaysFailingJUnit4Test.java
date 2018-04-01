package org.example.junitlauncher.vintage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class AlwaysFailingJUnit4Test {

    @Test
    public void testWillFail() {
        assertEquals("Values weren't equal", 3, 4);
    }
}
