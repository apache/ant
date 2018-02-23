package org.apache.tools.ant.taskdefs.optional.junitlauncher.example.jupiter;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 */
public class JupiterSampleTest {

    private static final String message = "The quick brown fox jumps over the lazy dog";

    @BeforeAll
    static void beforeAll() {
    }

    @BeforeEach
    void beforeEach() {
    }

    @Test
    void testSucceeds() {
        System.out.println(message);
        System.out.print("<some-other-message>Hello world! <!-- some comment --></some-other-message>");
    }

    @Test
    void testFails() {
        fail("intentionally failing");
    }

    @Test
    @Disabled("intentionally skipped")
    void testSkipped() {
    }

    @AfterEach
    void afterEach() {
    }

    @AfterAll
    static void afterAll() {
    }
}
