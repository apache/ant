/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs.modules;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.StringReader;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.spi.ToolProvider;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;

/**
 * Tests the {@link Link} task.
 */
public class LinkTest {
    /*
     * TODO:
     * Test --order-resources (how?)
     * Test --exclude-files (what does this actually do?)
     * Test --endian (how?)
     * Test --vm (how?)
     */

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public final ExpectedException expected = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/link.xml");
        buildRule.executeTarget("setUp");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").contains("Windows");
    }

    private static boolean isEarlierThan(final Instant time,
                                         final Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant().isBefore(time);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static class ImageStructure {
        final File root;
        final File bin;
        final File java;

        ImageStructure(final File root) {
            this.root = root;

            bin = new File(root, "bin");
            java = new File(bin, isWindows() ? "java.exe" : "java");
        }
    }

    private ImageStructure verifyImageBuiltNormally() {
        ImageStructure image = new ImageStructure(
            new File(buildRule.getProject().getProperty("image")));

        Assert.assertTrue("Checking that image was successfully created.",
            image.root.exists());

        Assert.assertTrue("Checking that image has java executable.",
            image.java.exists());

        return image;
    }

    @Test
    public void testModulepath() {
        buildRule.executeTarget("modulepath");
        verifyImageBuiltNormally();
    }

    @Test
    public void testImageNotRecreatedFromStaleJmods()
    throws IOException {
        buildRule.executeTarget("imageNewerThanJmods");
        ImageStructure image = verifyImageBuiltNormally();

        Instant future = Instant.now().plus(30, ChronoUnit.MINUTES);
        try (Stream<Path> imageFiles = Files.walk(image.root.toPath())) {

            Assert.assertTrue("Checking that newer image was not written "
                + "when source files are older.",
                imageFiles.noneMatch(i -> isEarlierThan(future, i)));
        }
    }

    @Test
    public void testNoModulePath() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("nomodulepath");
    }

    @Test
    public void testNoModules() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("nomodules");
    }

    @Test
    public void testModulePathRef() {
        buildRule.executeTarget("modulepathref");
        verifyImageBuiltNormally();
    }

    @Test
    public void testNestedModulePath() {
        buildRule.executeTarget("modulepath-nested");
        verifyImageBuiltNormally();
    }

    @Test
    public void testModulePathInAttributeAndNested() {
        buildRule.executeTarget("modulepath-both");
        verifyImageBuiltNormally();
    }

    @Test
    public void testNestedModules()
    throws IOException,
           InterruptedException {

        buildRule.executeTarget("modules-nested");

        ImageStructure image = verifyImageBuiltNormally();

        ProcessBuilder builder = new ProcessBuilder(
            image.java.toString(),
            buildRule.getProject().getProperty("hello.main-class"));
        builder.inheritIO();
        int exitCode = builder.start().waitFor();
        Assert.assertEquals(
            "Checking that execution of first module succeeded.", 0, exitCode);

        builder.command(
            image.java.toString(),
            buildRule.getProject().getProperty("smile.main-class"));
        exitCode = builder.start().waitFor();
        Assert.assertEquals(
            "Checking that execution of second module succeeded.", 0, exitCode);
    }

    @Test
    public void testNestedModuleMissingName() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("modules-nested-missing-name");
    }

    @Test
    public void testModulesInAttributeAndNested() {
        buildRule.executeTarget("modules-both");
        verifyImageBuiltNormally();
    }

    @Test
    public void testObservableModules() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("observable");
    }

    @Test
    public void testNestedObservableModules() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("observable-nested");
    }

    @Test
    public void testNestedObservableModuleMissingName() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("observable-nested-missing-name");
    }

    @Test
    public void testObservableModulesInAttributeAndNested() {
        buildRule.executeTarget("observable-both");
        verifyImageBuiltNormally();
    }

    private void verifyLaunchersExist() {
        ImageStructure image = verifyImageBuiltNormally();

        File launcher1 =
            new File(image.bin, isWindows() ? "Hello.bat" : "Hello");
        Assert.assertTrue("Checking that image has 'Hello' launcher.",
            launcher1.exists());

        File launcher2 =
            new File(image.bin, isWindows() ? "Smile.bat" : "Smile");
        Assert.assertTrue("Checking that image has 'Smile' launcher.",
            launcher2.exists());
    }

    @Test
    public void testLaunchers() {
        buildRule.executeTarget("launchers");
        verifyLaunchersExist();
    }

    @Test
    public void testNestedLaunchers() {
        buildRule.executeTarget("launchers-nested");
        verifyLaunchersExist();
    }

    @Test
    public void testNestedLauncherMissingName() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("launchers-nested-missing-name");
    }

    @Test
    public void testNestedLauncherMissingModule() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("launchers-nested-missing-module");
    }

    @Test
    public void testLaunchersInAttributeAndNested() {
        buildRule.executeTarget("launchers-both");
        verifyLaunchersExist();
    }

    private void verifyLocales()
    throws IOException,
           InterruptedException {

        ImageStructure image = verifyImageBuiltNormally();

        String mainClass =
            buildRule.getProject().getProperty("localefinder.main-class");
        Assert.assertNotNull("Checking that main-class property exists",
            mainClass);

        ProcessBuilder builder =
            new ProcessBuilder(image.java.toString(), mainClass, "zh", "in");
        builder.inheritIO();
        int exitCode = builder.start().waitFor();

        Assert.assertEquals("Verifying that image has access to locales "
            + "specified during linking.", 0, exitCode);

        builder.command(image.java.toString(), mainClass, "ja");
        exitCode = builder.start().waitFor();

        Assert.assertNotEquals(
            "Verifying that image does not have access to locales "
            + "not specified during linking.", 0, exitCode);
    }

    @Test
    public void testLocales()
    throws IOException,
           InterruptedException {

        buildRule.executeTarget("locales");
        verifyLocales();
    }

    @Test
    public void testNestedLocales()
    throws IOException,
           InterruptedException {

        buildRule.executeTarget("locales-nested");
        verifyLocales();
    }

    @Test
    public void testNestedLocaleMissingName() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("locales-nested-missing-name");
    }

    @Test
    public void testLocalesInAttributeAndNested()
    throws IOException,
           InterruptedException {

        buildRule.executeTarget("locales-both");
        verifyLocales();
    }

    @Test
    public void testExcludeResources()
    throws IOException {
        buildRule.executeTarget("excluderesources");
        ImageStructure image = verifyImageBuiltNormally();

        String mainClass =
            buildRule.getProject().getProperty("hello.main-class");
        Assert.assertNotNull("Checking that main-class property exists",
            mainClass);

        ProcessBuilder builder =
            new ProcessBuilder(image.java.toString(), mainClass,
                "resource1.txt", "resource2.txt");
        builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectErrorStream(true);

        Collection<String> outputLines;
        Process process = builder.start();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream()))) {

            outputLines = reader.lines().collect(Collectors.toList());
        }

        Assert.assertTrue(
            "Checking that excluded resource is actually excluded.",
            outputLines.stream().anyMatch(
                l -> l.endsWith("resource1.txt absent")));

        Assert.assertTrue(
            "Checking that resource not excluded is present.",
            outputLines.stream().anyMatch(
                l -> l.endsWith("resource2.txt present")));
    }

    @Test
    public void testNestedExcludeResources()
    throws IOException {
        buildRule.executeTarget("excluderesources-nested");
        ImageStructure image = verifyImageBuiltNormally();

        String mainClass =
            buildRule.getProject().getProperty("hello.main-class");
        Assert.assertNotNull("Checking that main-class property exists",
            mainClass);

        ProcessBuilder builder =
            new ProcessBuilder(image.java.toString(), mainClass,
                "resource1.txt", "resource2.txt");
        builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectErrorStream(true);

        Collection<String> outputLines;
        Process process = builder.start();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream()))) {

            outputLines = reader.lines().collect(Collectors.toList());
        }

        Assert.assertTrue(
            "Checking that excluded resource is actually excluded.",
            outputLines.stream().anyMatch(
                l -> l.endsWith("resource1.txt absent")));

        Assert.assertTrue(
            "Checking that resource not excluded is present.",
            outputLines.stream().anyMatch(
                l -> l.endsWith("resource2.txt present")));
    }

    @Test
    public void testNestedExcludeResourcesFile()
    throws IOException {
        buildRule.executeTarget("excluderesources-nested-file");
        ImageStructure image = verifyImageBuiltNormally();

        String mainClass =
            buildRule.getProject().getProperty("hello.main-class");
        Assert.assertNotNull("Checking that main-class property exists",
            mainClass);

        ProcessBuilder builder =
            new ProcessBuilder(image.java.toString(), mainClass,
                "resource1.txt", "resource2.txt");
        builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectErrorStream(true);

        Collection<String> outputLines;
        Process process = builder.start();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream()))) {

            outputLines = reader.lines().collect(Collectors.toList());
        }

        Assert.assertTrue(
            "Checking that excluded resource is actually excluded.",
            outputLines.stream().anyMatch(
                l -> l.endsWith("resource1.txt absent")));

        Assert.assertTrue(
            "Checking that resource not excluded is present.",
            outputLines.stream().anyMatch(
                l -> l.endsWith("resource2.txt present")));
    }

    @Test
    public void testNestedExcludeResourcesNoAttributes() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("excluderesources-nested-no-attr");
    }

    @Test
    public void testNestedExcludeResourcesFileAndPattern() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("excluderesources-nested-both");
    }

    @Test
    public void testExcludeResourcesAttributeAndNested()
    throws IOException {
        buildRule.executeTarget("excluderesources-both");
        ImageStructure image = verifyImageBuiltNormally();

        String mainClass =
            buildRule.getProject().getProperty("hello.main-class");
        Assert.assertNotNull("Checking that main-class property exists",
            mainClass);

        ProcessBuilder builder =
            new ProcessBuilder(image.java.toString(), mainClass,
                "resource1.txt", "resource2.txt");
        builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectErrorStream(true);

        Collection<String> outputLines;
        Process process = builder.start();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream()))) {

            outputLines = reader.lines().collect(Collectors.toList());
        }

        Assert.assertTrue(
            "Checking that first excluded resource is actually excluded.",
            outputLines.stream().anyMatch(
                l -> l.endsWith("resource1.txt absent")));

        Assert.assertTrue(
            "Checking that second excluded resource is actually excluded.",
            outputLines.stream().anyMatch(
                l -> l.endsWith("resource2.txt absent")));
    }

    @Test
    public void testExcludeFiles()
    throws IOException {
        buildRule.executeTarget("excludefiles");
        verifyImageBuiltNormally();
        // TODO: Test created image (what does --exclude-files actually do?)
    }

    @Test
    public void testNestedExcludeFiles()
    throws IOException {
        buildRule.executeTarget("excludefiles-nested");
        verifyImageBuiltNormally();
        // TODO: Test created image (what does --exclude-files actually do?)
    }

    @Test
    public void testNestedExcludeFilesFile()
    throws IOException {
        buildRule.executeTarget("excludefiles-nested-file");
        ImageStructure image = verifyImageBuiltNormally();
        // TODO: Test created image (what does --exclude-files actually do?)
    }

    @Test
    public void testNestedExcludeFilesNoAttributes() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("excludefiles-nested-no-attr");
    }

    @Test
    public void testNestedExcludeFilesFileAndPattern() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("excludefiles-nested-both");
    }

    @Test
    public void testExcludeFilesAttributeAndNested()
    throws IOException {
        buildRule.executeTarget("excludefiles-both");
        verifyImageBuiltNormally();
        // TODO: Test created image (what does --exclude-files actually do?)
    }

    @Test
    public void testOrdering()
    throws IOException {
        buildRule.executeTarget("ordering");
        verifyImageBuiltNormally();
        // TODO: Test resource order in created image (how?)
    }

    @Test
    public void testNestedOrdering()
    throws IOException {
        buildRule.executeTarget("ordering-nested");
        verifyImageBuiltNormally();
        // TODO: Test resource order in created image (how?)
    }

    @Test
    public void testNestedOrderingListFile()
    throws IOException {
        buildRule.executeTarget("ordering-nested-file");
        ImageStructure image = verifyImageBuiltNormally();
        // TODO: Test resource order in created image (how?)
    }

    @Test
    public void testNestedOrderingNoAttributes() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("ordering-nested-no-attr");
    }

    @Test
    public void testNestedOrderingFileAndPattern() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("ordering-nested-both");
    }

    @Test
    public void testOrderingAttributeAndNested()
    throws IOException {
        buildRule.executeTarget("ordering-both");
        verifyImageBuiltNormally();
        // TODO: Test resource order in created image (how?)
    }

    @Test
    public void testIncludeHeaders() {
        buildRule.executeTarget("includeheaders");
        ImageStructure image = verifyImageBuiltNormally();

        File[] headers = new File(image.root, "include").listFiles();
        Assert.assertTrue("Checking that include files were omitted.",
            headers == null || headers.length == 0);
    }

    @Test
    public void testIncludeManPages() {
        buildRule.executeTarget("includemanpages");
        ImageStructure image = verifyImageBuiltNormally();

        File[] manPages = new File(image.root, "man").listFiles();
        Assert.assertTrue("Checking that man pages were omitted.",
            manPages == null || manPages.length == 0);
    }

    @Test
    public void testIncludeNativeCommands() {
        buildRule.executeTarget("includenativecommands");
        ImageStructure image = new ImageStructure(
            new File(buildRule.getProject().getProperty("image")));

        Assert.assertTrue("Checking that image was successfully created.",
            image.root.exists());

        Assert.assertFalse(
            "Checking that image was stripped of java executable.",
            image.java.exists());
    }

    private long totalSizeOf(final Path path)
    throws IOException {
        if (Files.isDirectory(path)) {
            long size = 0;
            try (DirectoryStream<Path> children = Files.newDirectoryStream(path)) {
                for (Path child : children) {
                    size += totalSizeOf(child);
                }
            }
            return size;
        }

        if (Files.isRegularFile(path)) {
            return Files.size(path);
        }

        return 0;
    }

    @Test
    public void testCompression()
    throws IOException {
        buildRule.executeTarget("compression");
        ImageStructure image = verifyImageBuiltNormally();

        File compressedImageRoot =
            new File(buildRule.getProject().getProperty("compressed-image"));

        long size = totalSizeOf(image.root.toPath());
        long compressedSize = totalSizeOf(compressedImageRoot.toPath());

        Assert.assertTrue("Checking that compression resulted in smaller image.",
            compressedSize < size);
    }

    @Test
    public void testNestedCompression()
    throws IOException {
        buildRule.executeTarget("compression-nested");
        ImageStructure image = verifyImageBuiltNormally();

        File compressedImageRoot =
            new File(buildRule.getProject().getProperty("compressed-image"));

        long size = totalSizeOf(image.root.toPath());
        long compressedSize = totalSizeOf(compressedImageRoot.toPath());

        Assert.assertTrue("Checking that compression resulted in smaller image.",
            compressedSize < size);
    }

    @Test
    public void testNestedCompressionNoAttributes() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("compression-nested-no-attr");
    }

    @Test
    public void testNestedCompressionAttributeAndNested() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("compression-both");
    }

    @Test
    public void testEndian() {
        buildRule.executeTarget("endian");
        verifyImageBuiltNormally();
        // TODO: How can we test the created image?  Which files does --endian
        // affect?
    }

    @Test
    public void testVMType() {
        buildRule.executeTarget("vm");
        verifyImageBuiltNormally();
        // TODO: How can we test the created image?  Which files does --vm
        // affect?
    }

    @Test
    public void testReleaseInfoFile()
    throws IOException {
        buildRule.executeTarget("releaseinfo-file");
        ImageStructure image = verifyImageBuiltNormally();

        File release = new File(image.root, "release");
        try (BufferedReader reader =
            Files.newBufferedReader(release.toPath())) {

            Assert.assertTrue("Checking for 'test=true' in image release info.",
                reader.lines().anyMatch(l -> l.equals("test=true")));
        }
    }

    @Test
    public void testReleaseInfoDelete()
    throws IOException {
        buildRule.executeTarget("releaseinfo-delete");
        ImageStructure image = verifyImageBuiltNormally();

        File release = new File(image.root, "release");
        try (BufferedReader reader =
            Files.newBufferedReader(release.toPath())) {

            Assert.assertFalse("Checking that 'test' was deleted "
                + "from image release info.",
                reader.lines().anyMatch(l -> l.startsWith("test=")));
        }
    }

    @Test
    public void testReleaseInfoNestedDelete()
    throws IOException {
        buildRule.executeTarget("releaseinfo-nested-delete");
        ImageStructure image = verifyImageBuiltNormally();

        File release = new File(image.root, "release");
        try (BufferedReader reader =
            Files.newBufferedReader(release.toPath())) {

            Assert.assertFalse("Checking that 'test' was deleted "
                + "from image release info.",
                reader.lines().anyMatch(l -> l.startsWith("test=")));
        }
    }

    @Test
    public void testReleaseInfoNestedDeleteNoKey() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("releaseinfo-nested-delete-no-key");
    }

    @Test
    public void testReleaseInfoDeleteAttributeAndNested()
    throws IOException {
        buildRule.executeTarget("releaseinfo-nested-delete-both");
        ImageStructure image = verifyImageBuiltNormally();

        File release = new File(image.root, "release");
        try (BufferedReader reader =
            Files.newBufferedReader(release.toPath())) {

            Assert.assertTrue(
                "Checking that 'test' and 'foo' were deleted "
                + "from image release info.",
                reader.lines().noneMatch(l ->
                    l.startsWith("test=") || l.startsWith("foo=")));
        }
    }

    @Test
    public void testReleaseInfoAddFile()
    throws IOException {
        buildRule.executeTarget("releaseinfo-add-file");
        ImageStructure image = verifyImageBuiltNormally();

        File release = new File(image.root, "release");
        try (BufferedReader reader = new BufferedReader(
            new FileReader(release))) {

            Assert.assertTrue("Checking that 'test=s\u00ed' was added "
                + "to image release info.",
                reader.lines().anyMatch(l -> l.equals("test=s\u00ed")));
        }
    }

    @Test
    public void testReleaseInfoAddFileWithCharset()
    throws IOException {
        buildRule.executeTarget("releaseinfo-add-file-charset");
        ImageStructure image = verifyImageBuiltNormally();

        File release = new File(image.root, "release");
        // Using FileReader here since 'release' file is in platform's charset.
        try (BufferedReader reader = new BufferedReader(
            new FileReader(release))) {

            Assert.assertTrue("Checking that 'test=s\u00ed' was added "
                + "to image release info.",
                reader.lines().anyMatch(l -> l.equals("test=s\u00ed")));
        }
    }

    @Test
    public void testReleaseInfoAddKeyAndValue()
    throws IOException {
        buildRule.executeTarget("releaseinfo-add-key");
        ImageStructure image = verifyImageBuiltNormally();

        File release = new File(image.root, "release");
        try (BufferedReader reader =
            Files.newBufferedReader(release.toPath())) {

            Assert.assertTrue("Checking that 'test=true' was added "
                + "to image release info.",
                reader.lines().anyMatch(l -> l.equals("test=true")));
        }
    }

    @Test
    public void testReleaseInfoAddNoValue() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("releaseinfo-add-no-value");
    }

    @Test
    public void testReleaseInfoAddNoKey() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("releaseinfo-add-no-key");
    }

    @Test
    public void testReleaseInfoAddFileAndKey() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("releaseinfo-add-file-and-key");
    }

    @Test
    public void testReleaseInfoAddFileAndValue() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("releaseinfo-add-file-and-value");
    }

    @Test
    public void testDebugStripping()
    throws IOException,
           InterruptedException {

        buildRule.executeTarget("debug");
        ImageStructure image = verifyImageBuiltNormally();

        ProcessBuilder builder = new ProcessBuilder(
            image.java.toString(),
            buildRule.getProject().getProperty("thrower.main-class"));
        builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectErrorStream(true);

        Process process = builder.start();
        try (BufferedReader linesReader = new BufferedReader(
            new InputStreamReader(process.getInputStream()))) {

            Assert.assertTrue(
                "Checking that stack trace contains no debug information.",
                linesReader.lines().noneMatch(
                    l -> l.matches(".*\\([^)]*:[0-9]+\\)")));
        }
        process.waitFor();
    }

    @Test
    public void testDeduplicationOfLicenses() {
        buildRule.executeTarget("dedup");
        ImageStructure image = verifyImageBuiltNormally();

        String helloModuleName =
            buildRule.getProject().getProperty("hello.mod");
        String smileModuleName =
            buildRule.getProject().getProperty("smile.mod");

        Assert.assertNotNull("Checking that 'hello.mod' property was set.",
            helloModuleName);
        Assert.assertNotNull("Checking that 'smile.mod' property was set.",
            smileModuleName);

        Assume.assumeFalse("Checking that this operating system"
            + " supports symbolic links as a means of license de-duplication.",
            System.getProperty("os.name").contains("Windows"));

        Path legal = image.root.toPath().resolve("legal");

        Path[] licenses = {
            legal.resolve(helloModuleName).resolve("USELESSLICENSE"),
            legal.resolve(smileModuleName).resolve("USELESSLICENSE"),
        };

        int nonLinkCount = 0;
        for (Path license : licenses) {
            if (!Files.isSymbolicLink(license)) {
                nonLinkCount++;
            }
        }

        Assert.assertEquals(
            "Checking that USELESSLICENSE only exists once in image "
            + "and all other instances are links to it.",
            1, nonLinkCount);
    }

    @Test
    public void testIgnoreSigning() {
        buildRule.executeTarget("ignoresigning");
        verifyImageBuiltNormally();
    }

    /**
     * Should fail due to jlink rejecting identically named files whose
     * contents are different.
     */
    @Test
    public void testDeduplicationOfInconsistentLicenses() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("dedup-identical");
    }

    @Test
    public void testBindingOfServices()
    throws IOException,
           InterruptedException {
        buildRule.executeTarget("bindservices");
        ImageStructure image = verifyImageBuiltNormally();

        String mainClass = buildRule.getProject().getProperty("inc.main-class");

        ProcessBuilder builder = new ProcessBuilder(
            image.java.toString(), mainClass);
        builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);

        Process process = builder.start();
        try (BufferedReader linesReader = new BufferedReader(
            new InputStreamReader(process.getInputStream()))) {

            Assert.assertEquals(
                "Checking that bindServices=false results in no providers in image.",
                0, linesReader.lines().count());
        }
        process.waitFor();

        image = new ImageStructure(
            new File(buildRule.getProject().getProperty("image2")));

        Assert.assertTrue("Checking that image2 was successfully created.",
            image.root.exists());
        Assert.assertTrue("Checking that image2 has java executable.",
            image.java.exists());

        builder = new ProcessBuilder(image.java.toString(), mainClass);
        builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);

        process = builder.start();
        try (BufferedReader linesReader = new BufferedReader(
            new InputStreamReader(process.getInputStream()))) {

            Assert.assertEquals(
                "Checking that bindServices=true results in image with provider.",
                5, linesReader.lines().count());
        }
        process.waitFor();
    }

    private String runJlink(final String... args) {
        ToolProvider jlink = ToolProvider.findFirst("jlink").orElseThrow(
            () -> new RuntimeException("jlink tool not found in JDK."));

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        int exitCode;
        try (PrintStream out = new PrintStream(stdout);
             PrintStream err = new PrintStream(stderr)) {

            exitCode = jlink.run(out, err, args);
        }

        if (exitCode != 0) {
            throw new RuntimeException(
                "jlink failed, output is: " + stdout + ", error is: " + stderr);
        }

        return stdout.toString();
    }
}
