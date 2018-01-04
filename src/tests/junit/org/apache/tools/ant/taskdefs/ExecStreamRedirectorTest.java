package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@code exec} task which uses a {@code redirector} to redirect its output and error streams
 */
public class ExecStreamRedirectorTest {

    private Project project;

    @Before
    public void setUp() throws Exception {
        project = new Project();
        project.init();
        final File antFile = new File(System.getProperty("root"), "src/etc/testcases/taskdefs/exec/exec-with-redirector.xml");
        project.setUserProperty("ant.file", antFile.getAbsolutePath());
        final File outputDir = this.createTmpDir();
        project.setUserProperty("output", outputDir.toString());
        ProjectHelper.configureProject(project, antFile);
        project.executeTarget("setUp");
    }

    /**
     * Tests that the redirected streams of the exec'ed process aren't truncated.
     *
     * @throws Exception
     * @see <a href="https://bz.apache.org/bugzilla/show_bug.cgi?id=58451">bz-58451</a> and
     * <a href="https://bz.apache.org/bugzilla/show_bug.cgi?id=58833">bz-58833</a> for more details
     */
    @Test
    public void testRedirection() throws Exception {
        final String dirToList = project.getProperty("dir.to.ls");
        assertNotNull("Directory to list isn't available", dirToList);
        assertTrue(dirToList + " is not a directory", new File(dirToList).isDirectory());

        project.executeTarget("list-dir");

        // verify the redirected output
        final String outputDirPath = project.getProperty("output");
        byte[] dirListingOutput = null;
        for (int i = 1; i <= 16; i++) {
            final File redirectedOutputFile = new File(outputDirPath, "ls" + i + ".txt");
            assertTrue(redirectedOutputFile + " is missing or not a regular file", redirectedOutputFile.isFile());
            final byte[] redirectedOutput = readAllBytes(redirectedOutputFile);
            assertNotNull("No content was redirected to " + redirectedOutputFile, redirectedOutput);
            assertFalse("Content in redirected file " + redirectedOutputFile + " was empty", redirectedOutput.length == 0);
            if (dirListingOutput != null) {
                // compare the directory listing that was redirected to these files. all files should have the same content
                assertTrue("Redirected output in file " + redirectedOutputFile +
                        " doesn't match content in other redirected output file(s)", Arrays.equals(dirListingOutput, redirectedOutput));
            }
            dirListingOutput = redirectedOutput;
        }
    }

    private File createTmpDir() {
        final File tmpDir = new File(System.getProperty("java.io.tmpdir"), String.valueOf("temp-" + System.nanoTime()));
        tmpDir.mkdir();
        tmpDir.deleteOnExit();
        return tmpDir;
    }

    private static byte[] readAllBytes(final File file) throws IOException {
        final FileInputStream fis = new FileInputStream(file);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            final byte[] dataChunk = new byte[1024];
            int numRead = -1;
            while ((numRead = fis.read(dataChunk)) > 0) {
                bos.write(dataChunk, 0, numRead);
            }
        } finally {
            fis.close();
        }
        return bos.toByteArray();
    }
}
