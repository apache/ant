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

import java.io.BufferedReader;
import java.io.StringReader;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.function.Predicate;
import java.util.regex.Pattern;

import java.util.spi.ToolProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;

/**
 * Tests the {@link Jmod} task.
 */
public class JmodTest {
    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public final ExpectedException expected = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/jmod.xml");
        buildRule.executeTarget("setUp");
    }

    @Test
    public void testDestAndClasspathNoJmod() {
        buildRule.executeTarget("destAndClasspathNoJmod");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());
    }

    @Test
    public void testDestAndNestedClasspath() {
        buildRule.executeTarget("classpath-nested");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());
    }

    @Test
    public void testDestAndClasspathOlderThanJmod()
    throws IOException {
        buildRule.executeTarget("destAndClasspathOlderThanJmod");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        File jar = new File(buildRule.getProject().getProperty("hello.jar"));
        Assert.assertTrue("Checking that newer jmod was not written "
            + "when source files are older.",
            Files.getLastModifiedTime(jmod.toPath()).toInstant().isAfter(
                Instant.now().plus(30, ChronoUnit.MINUTES)));
    }

    @Test
    public void testNoDestFile() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("noDestFile");
    }

    @Test
    public void testNoClasspath() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("noClasspath");
    }

    @Test
    public void testEmptyClasspath() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("emptyClasspath");
    }

    @Test
    public void testClasspathEntirelyNonexistent() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("nonexistentClasspath");
    }

    @Test
    public void testClasspathref() {
        buildRule.executeTarget("classpathref");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());
    }

    @Test
    public void testClasspathAttributeAndChildElement() {
        buildRule.executeTarget("classpath-both");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());
    }

    @Test
    public void testModulepath() {
        buildRule.executeTarget("modulepath");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());
    }

    @Test
    public void testModulepathref() {
        buildRule.executeTarget("modulepathref");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());
    }

    @Test
    public void testModulepathNested() {
        buildRule.executeTarget("modulepath-nested");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());
    }

    @Test
    public void testModulepathNonDir() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("modulepathnondir");
    }

    @Test
    public void testModulepathAttributeAndChildElement() {
        buildRule.executeTarget("modulepath-both");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());
    }

    @Test
    public void testCommandPath() {
        buildRule.executeTarget("commandpath");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("list", jmod.toString());
        Assert.assertTrue("Checking that jmod contains command.",
            containsLine(output, l -> l.equals("bin/command1")));
    }

    @Test
    public void testCommandPathref() {
        buildRule.executeTarget("commandpathref");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("list", jmod.toString());
        Assert.assertTrue("Checking that jmod contains command.",
            containsLine(output, l -> l.equals("bin/command2")));
    }

    @Test
    public void testCommandPathNested() {
        buildRule.executeTarget("commandpath-nested");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("list", jmod.toString());
        Assert.assertTrue("Checking that jmod contains command.",
            containsLine(output, l -> l.equals("bin/command3")));
    }

    @Test
    public void testCommandPathAttributeAndChildElement() {
        buildRule.executeTarget("commandpath-both");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("list", jmod.toString());
        Assert.assertTrue("Checking that jmod contains commands "
            + "from both attribute and child element.",
            containsAll(output,
                l -> l.equals("bin/command4"),
                l -> l.equals("bin/command5")));
    }

    @Test
    public void testHeaderPath() {
        buildRule.executeTarget("headerpath");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("list", jmod.toString());
        Assert.assertTrue("Checking that jmod contains header file.",
            containsLine(output, l -> l.equals("include/header1.h")));
    }

    @Test
    public void testHeaderPathref() {
        buildRule.executeTarget("headerpathref");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("list", jmod.toString());
        Assert.assertTrue("Checking that jmod contains header file.",
            containsLine(output, l -> l.equals("include/header2.h")));
    }

    @Test
    public void testHeaderPathNested() {
        buildRule.executeTarget("headerpath-nested");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("list", jmod.toString());
        Assert.assertTrue("Checking that jmod contains header file.",
            containsLine(output, l -> l.equals("include/header3.h")));
    }

    @Test
    public void testHeaderPathAttributeAndChildElement() {
        buildRule.executeTarget("headerpath-both");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("list", jmod.toString());
        Assert.assertTrue("Checking that jmod contains header files "
            + "from both attribute and child element.",
            containsAll(output,
                l -> l.equals("include/header4.h"),
                l -> l.equals("include/header5.h")));
    }

    @Test
    public void testConfigPath() {
        buildRule.executeTarget("configpath");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("list", jmod.toString());
        Assert.assertTrue("Checking that jmod contains config file.",
            containsLine(output, l -> l.equals("conf/config1.properties")));
    }

    @Test
    public void testConfigPathref() {
        buildRule.executeTarget("configpathref");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("list", jmod.toString());
        Assert.assertTrue("Checking that jmod contains config file.",
            containsLine(output, l -> l.equals("conf/config2.properties")));
    }

    @Test
    public void testConfigPathNested() {
        buildRule.executeTarget("configpath-nested");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("list", jmod.toString());
        Assert.assertTrue("Checking that jmod contains config file.",
            containsLine(output, l -> l.equals("conf/config3.properties")));
    }

    @Test
    public void testConfigPathAttributeAndChildElement() {
        buildRule.executeTarget("configpath-both");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("list", jmod.toString());
        Assert.assertTrue("Checking that jmod contains config files "
            + "from both attribute and child element.",
            containsAll(output,
                l -> l.equals("conf/config4.properties"),
                l -> l.equals("conf/config5.properties")));
    }

    @Test
    public void testLegalPath() {
        buildRule.executeTarget("legalpath");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("list", jmod.toString());
        Assert.assertTrue("Checking that jmod contains license file.",
            containsLine(output, l -> l.equals("legal/legal1.txt")));
    }

    @Test
    public void testLegalPathref() {
        buildRule.executeTarget("legalpathref");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("list", jmod.toString());
        Assert.assertTrue("Checking that jmod contains license file.",
            containsLine(output, l -> l.equals("legal/legal2.txt")));
    }

    @Test
    public void testLegalPathNested() {
        buildRule.executeTarget("legalpath-nested");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("list", jmod.toString());
        Assert.assertTrue("Checking that jmod contains license file.",
            containsLine(output, l -> l.equals("legal/legal3.txt")));
    }

    @Test
    public void testLegalPathAttributeAndChildElement() {
        buildRule.executeTarget("legalpath-both");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("list", jmod.toString());
        Assert.assertTrue("Checking that jmod contains legal files "
            + "from both attribute and child element.",
            containsAll(output,
                l -> l.equals("legal/legal4.txt"),
                l -> l.equals("legal/legal5.txt")));
    }

    @Test
    public void testManPath() {
        buildRule.executeTarget("manpath");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("list", jmod.toString());
        Assert.assertTrue("Checking that jmod contains man page.",
            containsLine(output, l -> l.equals("man/man1.1")));
    }

    @Test
    public void testManPathref() {
        buildRule.executeTarget("manpathref");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("list", jmod.toString());
        Assert.assertTrue("Checking that jmod contains man page.",
            containsLine(output, l -> l.equals("man/man2.1")));
    }

    @Test
    public void testManPathNested() {
        buildRule.executeTarget("manpath-nested");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("list", jmod.toString());
        Assert.assertTrue("Checking that jmod contains man page.",
            containsLine(output, l -> l.equals("man/man3.1")));
    }

    @Test
    public void testManPathAttributeAndChildElement() {
        buildRule.executeTarget("manpath-both");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("list", jmod.toString());
        Assert.assertTrue("Checking that jmod contains man pages "
            + "from both attribute and child element.",
            containsAll(output,
                l -> l.equals("man/man4.1"),
                l -> l.equals("man/man5.1")));
    }

    @Test
    public void testNativeLibPath() {
        buildRule.executeTarget("nativelibpath");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("list", jmod.toString());
        Assert.assertTrue("Checking that jmod contains native library.",
            containsLine(output, l -> l.matches("lib/[^/]+\\.(dll|dylib|so)")));
    }

    @Test
    public void testNativeLibPathref() {
        buildRule.executeTarget("nativelibpathref");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("list", jmod.toString());
        Assert.assertTrue("Checking that jmod contains native library.",
            containsLine(output, l -> l.matches("lib/[^/]+\\.(dll|dylib|so)")));
    }

    @Test
    public void testNativeLibPathNested() {
        buildRule.executeTarget("nativelibpath-nested");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("list", jmod.toString());
        Assert.assertTrue("Checking that jmod contains native library.",
            containsLine(output, l -> l.matches("lib/[^/]+\\.(dll|dylib|so)")));
    }

    @Test
    public void testNativeLibPathAttributeAndChildElement() {
        buildRule.executeTarget("nativelibpath-both");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("list", jmod.toString());
        Assert.assertTrue("Checking that jmod contains native libraries "
            + "from both attribute and child element.",
            containsAll(output,
                l -> l.matches("lib/(lib)?zip\\.(dll|dylib|so)"),
                l -> l.matches("lib/(lib)?jvm\\.(dll|dylib|so)")));
    }

    @Test
    public void testVersion() {
        buildRule.executeTarget("version");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String version = buildRule.getProject().getProperty("version");
        Assert.assertNotNull("Checking that 'version' property is set",
            version);
        Assert.assertFalse("Checking that 'version' property is not empty",
            version.isEmpty());

        String output = runJmod("describe", jmod.toString());
        Assert.assertTrue("Checking that jmod has correct version.",
            containsLine(output, l -> l.endsWith("@" + version)));
    }

    @Test
    public void testNestedVersion() {
        buildRule.executeTarget("version-nested");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("describe", jmod.toString());
        Assert.assertTrue("Checking that jmod has correct version.",
            containsLine(output, l -> l.matches(".*@1\\.0\\.1[-+]+99")));
    }

    @Test
    public void testNestedVersionNumberOnly() {
        buildRule.executeTarget("version-nested-number");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("describe", jmod.toString());
        Assert.assertTrue("Checking that jmod has correct version.",
            containsLine(output, l -> l.endsWith("@1.0.1")));
    }

    @Test
    public void testNestedVersionNoNumber() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("version-nested-no-number");
    }

    @Test
    public void testNestedVersionInvalidNumber() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("version-nested-invalid-number");
    }

    @Test
    public void testNestedVersionInvalidPreRelease() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("version-nested-invalid-prerelease");
    }

    @Test
    public void testVersionAttributeAndChildElement() {
        expected.expect(BuildException.class);
        buildRule.executeTarget("version-both");
    }

    @Test
    public void testMainClass() {
        buildRule.executeTarget("mainclass");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String mainClass =
            buildRule.getProject().getProperty("hello.main-class");
        Assert.assertNotNull("Checking that 'main-class' property is set",
            mainClass);
        Assert.assertFalse("Checking that 'main-class' property is not empty",
            mainClass.isEmpty());

        String output = runJmod("describe", jmod.toString());

        String mainClassPattern = "main-class\\s+" + Pattern.quote(mainClass);
        Assert.assertTrue("Checking that jmod has correct main class.",
            containsLine(output, l -> l.matches(mainClassPattern)));
    }

    @Test
    public void testPlatform() {
        buildRule.executeTarget("platform");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String platform = buildRule.getProject().getProperty("target-platform");
        Assert.assertNotNull("Checking that 'target-platform' property is set",
            platform);
        Assert.assertFalse("Checking that 'target-platform' property "
            + "is not empty", platform.isEmpty());

        String output = runJmod("describe", jmod.toString());

        String platformPattern = "platform\\s+" + Pattern.quote(platform);
        Assert.assertTrue("Checking that jmod has correct main class.",
            containsLine(output, l -> l.matches(platformPattern)));
    }

    @Test
    public void testHashing() {
        buildRule.executeTarget("hashing");

        File jmod = new File(buildRule.getProject().getProperty("jmod"));
        Assert.assertTrue("Checking that jmod was successfully created.",
            jmod.exists());

        String output = runJmod("describe", jmod.toString());

        Assert.assertTrue("Checking that jmod has module hashes.",
            containsLine(output, l -> l.startsWith("hashes")));
    }

    private String runJmod(final String... args) {
        ToolProvider jmod = ToolProvider.findFirst("jmod").orElseThrow(
            () -> new RuntimeException("jmod tool not found in JDK."));

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        int exitCode;
        try (PrintStream out = new PrintStream(stdout);
             PrintStream err = new PrintStream(stderr)) {

            exitCode = jmod.run(out, err, args);
        }

        if (exitCode != 0) {
            throw new RuntimeException(
                "jmod failed, output is: " + stdout + ", error is: " + stderr);
        }

        return stdout.toString();
    }

    private boolean containsLine(final String lines,
                                 final Predicate<? super String> test) {
        try (BufferedReader reader =
            new BufferedReader(new StringReader(lines))) {

            return reader.lines().anyMatch(test);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean containsAll(final String lines,
                                final Predicate<? super String> test1,
                                final Predicate<? super String> test2) {

        try (BufferedReader reader =
            new BufferedReader(new StringReader(lines))) {

            boolean test1Matched = false;
            boolean test2Matched = false;

            String line;
            while ((line = reader.readLine()) != null) {
                test1Matched |= test1.test(line);
                test2Matched |= test2.test(line);
            }

            return test1Matched && test2Matched;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
