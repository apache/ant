package org.example.junitlauncher.vintage;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 */
public class AlwaysFailingJUnit4Test {

    @Test
    public void testWillFail() throws Exception {
        Assert.assertEquals("Values weren't equal", 3, 4);
    }
}
