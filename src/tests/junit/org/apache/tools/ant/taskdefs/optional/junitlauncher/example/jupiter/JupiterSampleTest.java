package org.apache.tools.ant.taskdefs.optional.junitlauncher.example.jupiter;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 */
public class JupiterSampleTest {

    @BeforeAll
    static void beforeAll() {
    }

    @BeforeEach
    void beforeEach() {
    }

    @Test
    void testSucceeds() {
        System.out.println("in test succeeds " + new Date());
        System.out.println("in test succeeds 2" + new Date());
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
