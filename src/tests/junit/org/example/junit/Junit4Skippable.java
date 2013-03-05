package org.example.junit;

import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Junit4Skippable {

    @Test
    public void passingTest() {
        assertTrue("This test passed", true);
    }

    @Ignore("Please don't ignore me!")
    @Test
    public void explicitIgnoreTest() {
        fail("This test should be skipped");
    }

    @Test
    public void implicitlyIgnoreTest() {
        Assume.assumeFalse("This test will be ignored", true);
        fail("I told you, this test should have been ignored!");
    }

    @Test
    @Ignore
    public void explicitlyIgnoreTestNoMessage() {
        fail("This test should be skipped");
    }

    @Test
    public void implicitlyIgnoreTestNoMessage() {
        Assume.assumeFalse(true);
        fail("I told you, this test should have been ignored!");
    }

    @Test
    public void failingTest() {
        fail("I told you this test was going to fail");
    }

    @Test
    public void failingTestNoMessage() {
        fail();
    }

    @Test
    public void errorTest() {
        throw new RuntimeException("Whoops, this test went wrong");
    }

}
