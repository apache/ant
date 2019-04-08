/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs.modules;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;

import java.nio.file.Paths;

import java.util.Optional;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.junit.Assert;

/**
 * Tests module-related capabilities of &lt;jar&gt; task.
 */
public class JarModuleRelatedTests {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/jar.xml");
        buildRule.executeTarget("setUp");
    }

    private Optional<ModuleReference> getTestModule() {
        String moduleName =
            buildRule.getProject().getProperty("test-module.name");

        String jarFileName = buildRule.getProject().getProperty("tmp.jar");
        ModuleFinder finder = ModuleFinder.of(Paths.get(jarFileName));

        return finder.find(moduleName);
    }

    private ModuleDescriptor getTestModuleDescriptor() {
        Optional<ModuleReference> module = getTestModule();
        Assert.assertTrue("Verifying that test jar is a modular jar.",
            module.isPresent());

        return module.get().descriptor();
    }

    @Test
    public void testModularJar() {
        buildRule.executeTarget("testModularJar");

        Assert.assertTrue(
            "Verifying that specifying neither version nor main class "
            + "allows unmodified module-info to be included in jar.",
            getTestModule().isPresent());
    }

    @Test
    public void testModuleVersion() {
        buildRule.executeTarget("testModuleVersion");

        Optional<String> version = getTestModuleDescriptor().rawVersion();
        Assert.assertTrue("Checking that modular jar has a module version.",
            version.isPresent());

        Assert.assertEquals(
            "Checking that modular jar has correct module version.",
            "1.0-test+0001", version.get());
    }

    @Test
    public void testNestedVersion() {
        buildRule.executeTarget("testNestedVersion");

        Optional<String> version = getTestModuleDescriptor().rawVersion();
        Assert.assertTrue("Checking that modular jar has a module version.",
            version.isPresent());

        Assert.assertEquals(
            "Checking that modular jar has correct module version.",
            "1.0-test+0001", version.get());
    }

    @Test
    public void testNestedVersionVersionNumberOnly() {
        buildRule.executeTarget("testNestedVersionNumberOnly");

        Optional<String> version = getTestModuleDescriptor().rawVersion();
        Assert.assertTrue("Checking that modular jar has a module version.",
            version.isPresent());

        Assert.assertEquals(
            "Checking that modular jar has correct module version.",
            "1.0", version.get());
    }

    @Test
    public void testNestedVersionNumberAndPreReleaseOnly() {
        buildRule.executeTarget("testNestedVersionNumberAndPreReleaseOnly");

        Optional<String> version = getTestModuleDescriptor().rawVersion();
        Assert.assertTrue("Checking that modular jar has a module version.",
            version.isPresent());

        Assert.assertEquals(
            "Checking that modular jar has correct module version.",
            "1.0-test", version.get());
    }

    @Test
    public void testNestedVersionNumberAndBuildOnly() {
        buildRule.executeTarget("testNestedVersionNumberAndBuildOnly");

        Optional<String> version = getTestModuleDescriptor().rawVersion();
        Assert.assertTrue("Checking that modular jar has a module version.",
            version.isPresent());

        Assert.assertEquals(
            "Checking that modular jar has correct module version.",
            "1.0-+0001", version.get());
    }

    @Test
    public void testNestedVersionMissingNumber() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("number");

        buildRule.executeTarget("testNestedVersionMissingNumber");

    }

    @Test
    public void testNestedVersionInvalidNumber() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("contain");

        buildRule.executeTarget("testNestedVersionInvalidNumber");
    }

    @Test
    public void testNestedVersionInvalidPreRelease() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("contain");

        buildRule.executeTarget("testNestedVersionInvalidPreRelease");
    }

    @Test
    public void testNestedVersionAndAttribute() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("both");

        buildRule.executeTarget("testNestedVersionAndAttribute");
    }

    @Test
    public void testMainClass() {
        buildRule.executeTarget("testMainClass");

        String packageName =
            buildRule.getProject().getProperty("package.name");

        Optional<String> mainClass = getTestModuleDescriptor().mainClass();
        Assert.assertTrue("Checking that modular jar has a main class.",
            mainClass.isPresent());

        Assert.assertEquals(
            "Checking that modular jar has correct main class.",
            packageName + ".Test", mainClass.get());
    }

    @Test
    public void testModuleVersionAndMainClass() {
        buildRule.executeTarget("testModuleVersionAndMainClass");

        String packageName =
            buildRule.getProject().getProperty("package.name");

        ModuleDescriptor descriptor = getTestModuleDescriptor();

        Optional<String> version = getTestModuleDescriptor().rawVersion();
        Assert.assertTrue("Checking that modular jar has a module version.",
            version.isPresent());

        Optional<String> mainClass = descriptor.mainClass();
        Assert.assertTrue("Checking that modular jar has a main class.",
            mainClass.isPresent());

        Assert.assertEquals(
            "Checking that modular jar has correct module version.",
            "1.0-test+0001", version.get());

        Assert.assertEquals(
            "Checking that modular jar has correct main class.",
            packageName + ".Test", mainClass.get());
    }
}
