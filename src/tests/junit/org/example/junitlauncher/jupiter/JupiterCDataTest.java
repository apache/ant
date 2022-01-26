package org.example.junitlauncher.jupiter;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Test class to ensure that CData is properly encoded by legacy-xml
 */
class JupiterCDataTest {
    @Test
    void testExceptionCData() {
        fail(new IllegalArgumentException("]]>"));
    }

    @Test
    void testAbortedCData() {
        assumeTrue(Instant.now().isAfter(Instant.MIN), "]]>");
        fail("We should never reach this");
    }
}
