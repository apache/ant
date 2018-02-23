package org.example.junitlauncher.vintage;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class JUnit4SampleTest {

    @Test
    public void testFoo() {
        Assert.assertEquals(1, 1);
    }

    @Test
    public void testBar() throws Exception {
        Assert.assertTrue(true);
    }

    @Test
    public void testFooBar() {
        Assert.assertFalse(false);
    }
}
