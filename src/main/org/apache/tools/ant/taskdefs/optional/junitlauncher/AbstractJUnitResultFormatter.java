package org.apache.tools.ant.taskdefs.optional.junitlauncher;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Contains some common behaviour that's used by our internal {@link TestResultFormatter}s
 */
abstract class AbstractJUnitResultFormatter implements TestResultFormatter {

    protected static String NEW_LINE = System.getProperty("line.separator");
    protected Path sysOutFilePath;
    protected Path sysErrFilePath;
    protected Task task;

    private OutputStream sysOutStream;
    private OutputStream sysErrStream;

    @Override
    public void sysOutAvailable(final byte[] data) {
        if (this.sysOutStream == null) {
            try {
                this.sysOutFilePath = Files.createTempFile(null, "sysout");
                this.sysOutFilePath.toFile().deleteOnExit();
                this.sysOutStream = Files.newOutputStream(this.sysOutFilePath);
            } catch (IOException e) {
                handleException(e);
                return;
            }
        }
        try {
            this.sysOutStream.write(data);
        } catch (IOException e) {
            handleException(e);
            return;
        }
    }

    @Override
    public void sysErrAvailable(final byte[] data) {
        if (this.sysErrStream == null) {
            try {
                this.sysErrFilePath = Files.createTempFile(null, "syserr");
                this.sysErrFilePath.toFile().deleteOnExit();
                this.sysErrStream = Files.newOutputStream(this.sysOutFilePath);
            } catch (IOException e) {
                handleException(e);
                return;
            }
        }
        try {
            this.sysErrStream.write(data);
        } catch (IOException e) {
            handleException(e);
            return;
        }
    }

    @Override
    public void setExecutingTask(final Task task) {
        this.task = task;
    }

    protected void writeSysOut(final Writer writer) throws IOException {
        this.writeFrom(this.sysOutFilePath, writer);
    }

    protected void writeSysErr(final Writer writer) throws IOException {
        this.writeFrom(this.sysErrFilePath, writer);
    }

    static Optional<TestIdentifier> traverseAndFindTestClass(final TestPlan testPlan, final TestIdentifier testIdentifier) {
        if (isTestClass(testIdentifier).isPresent()) {
            return Optional.of(testIdentifier);
        }
        final Optional<TestIdentifier> parent = testPlan.getParent(testIdentifier);
        return parent.isPresent() ? traverseAndFindTestClass(testPlan, parent.get()) : Optional.empty();
    }

    static Optional<ClassSource> isTestClass(final TestIdentifier testIdentifier) {
        if (testIdentifier == null) {
            return Optional.empty();
        }
        final Optional<TestSource> source = testIdentifier.getSource();
        if (!source.isPresent()) {
            return Optional.empty();
        }
        final TestSource testSource = source.get();
        if (testSource instanceof ClassSource) {
            return Optional.of((ClassSource) testSource);
        }
        return Optional.empty();
    }

    private void writeFrom(final Path path, final Writer writer) throws IOException {
        final byte[] content = new byte[1024];
        int numBytes;
        try (final InputStream is = Files.newInputStream(path)) {
            while ((numBytes = is.read(content)) != -1) {
                writer.write(new String(content, 0, numBytes));
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (this.sysOutStream != null) {
            try {
                this.sysOutStream.close();
            } catch (Exception e) {
                // ignore
            }
        }
        if (this.sysErrStream != null) {
            try {
                this.sysErrStream.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    protected void handleException(final Throwable t) {
        // we currently just log it and move on.
        task.getProject().log("Exception in listener " + this.getClass().getName(), t, Project.MSG_DEBUG);
    }
}
