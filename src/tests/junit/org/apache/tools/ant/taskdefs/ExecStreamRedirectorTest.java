package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@code exec} task which uses a {@code redirector} to redirect its output and error streams
 */
public class ExecStreamRedirectorTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File outputDir;

    @Before
    public void setUp() throws IOException {
        buildRule.configureProject("src/etc/testcases/taskdefs/exec/exec-with-redirector.xml");
        outputDir = folder.newFolder(String.valueOf("temp-" + System.nanoTime()));
        buildRule.getProject().setUserProperty("output", outputDir.toString());
        buildRule.executeTarget("setUp");
    }

    /**
     * Tests that the redirected streams of the exec'ed process aren't truncated.
     *
     * @throws Exception if something goes wrong
     * @see <a href="https://bz.apache.org/bugzilla/show_bug.cgi?id=58451">bz-58451</a> and
     * <a href="https://bz.apache.org/bugzilla/show_bug.cgi?id=58833">bz-58833</a> for more details
     */
    @Test
    public void testRedirection() throws Exception {
        final String dirToList = buildRule.getProject().getProperty("dir.to.ls");
        assertNotNull("Directory to list isn't available", dirToList);
        assertTrue(dirToList + " is not a directory", new File(dirToList).isDirectory());

        buildRule.executeTarget("list-dir");

        // verify the redirected output
        byte[] dirListingOutput = null;
        for (int i = 1; i <= 16; i++) {
            final File redirectedOutputFile = new File(outputDir, "ls" + i + ".txt");
            assertTrue(redirectedOutputFile + " is missing or not a regular file",
                    redirectedOutputFile.isFile());
            final byte[] redirectedOutput = readAllBytes(redirectedOutputFile);
            assertNotNull("No content was redirected to " + redirectedOutputFile, redirectedOutput);
            assertNotEquals("Content in redirected file " + redirectedOutputFile + " was empty",
                    0, redirectedOutput.length);
            if (dirListingOutput != null) {
                // Compare the directory listing that was redirected to these files.
                // All files should have the same content.
                assertTrue("Redirected output in file " + redirectedOutputFile +
                        " doesn't match content in other redirected output file(s)",
                        Arrays.equals(dirListingOutput, redirectedOutput));
            }
            dirListingOutput = redirectedOutput;
        }
    }

    private static byte[] readAllBytes(final File file) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (FileInputStream fis = new FileInputStream(file)) {
            final byte[] dataChunk = new byte[1024];
            int numRead = -1;
            while ((numRead = fis.read(dataChunk)) > 0) {
                bos.write(dataChunk, 0, numRead);
            }
        }
        return bos.toByteArray();
    }
}
