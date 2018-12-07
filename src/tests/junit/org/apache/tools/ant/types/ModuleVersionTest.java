package org.apache.tools.ant.types;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests {@link ModuleVersion} class.
 */
public class ModuleVersionTest {
    @Rule
    public final ExpectedException expected = ExpectedException.none();

    @Test
    public void testModuleVersionStringNumberPreBuild() {
        ModuleVersion moduleVersion = new ModuleVersion();

        moduleVersion.setNumber("1.1.3");
        moduleVersion.setPreRelease("ea");
        moduleVersion.setBuild("25");

        String versionStr = moduleVersion.toModuleVersionString();

        Assert.assertNotNull("Checking for non-null module version string.",
            versionStr);
        Assert.assertTrue("Checking for correct module version string.",
            versionStr.matches("1\\.1\\.3[-+]ea\\+25"));
    }

    @Test
    public void testModuleVersionStringNumberPre() {
        ModuleVersion moduleVersion = new ModuleVersion();

        moduleVersion.setNumber("1.1.3");
        moduleVersion.setPreRelease("ea");

        String versionStr = moduleVersion.toModuleVersionString();

        Assert.assertNotNull("Checking for non-null module version string.",
            versionStr);
        Assert.assertTrue("Checking for correct module version string.",
            versionStr.matches("1\\.1\\.3[-+]ea"));
    }

    @Test
    public void testModuleVersionStringNumberBuild() {
        ModuleVersion moduleVersion = new ModuleVersion();

        moduleVersion.setNumber("1.1.3");
        moduleVersion.setBuild("25");

        String versionStr = moduleVersion.toModuleVersionString();

        Assert.assertNotNull("Checking for non-null module version string.",
            versionStr);
        Assert.assertTrue("Checking for correct module version string.",
            versionStr.matches("1\\.1\\.3[-+]\\+25"));
    }

    @Test
    public void testModuleVersionStringNumberOnly() {
        ModuleVersion moduleVersion = new ModuleVersion();

        moduleVersion.setNumber("1.1.3");

        String versionStr = moduleVersion.toModuleVersionString();

        Assert.assertNotNull("Checking for non-null module version string.",
            versionStr);
        Assert.assertEquals("Checking for correct module version string.",
            "1.1.3", versionStr);
    }

    @Test
    public void testModuleVersionStringNullNumber() {
        expected.expect(IllegalStateException.class);

        ModuleVersion moduleVersion = new ModuleVersion();
        moduleVersion.toModuleVersionString();
    }

    @Test
    public void testNullNumber() {
        expected.expect(NullPointerException.class);

        ModuleVersion moduleVersion = new ModuleVersion();
        moduleVersion.setNumber(null);
    }

    @Test
    public void testInvalidNumber() {
        expected.expect(IllegalArgumentException.class);

        ModuleVersion moduleVersion = new ModuleVersion();
        moduleVersion.setNumber("1-1-3");
    }

    @Test
    public void testInvalidNumber2() {
        expected.expect(IllegalArgumentException.class);

        ModuleVersion moduleVersion = new ModuleVersion();
        moduleVersion.setNumber("1.1+3");
    }

    @Test
    public void testInvalidPreRelease() {
        expected.expect(IllegalArgumentException.class);

        ModuleVersion moduleVersion = new ModuleVersion();
        moduleVersion.setNumber("1.1.3");
        moduleVersion.setPreRelease("ea+interim");
    }
}
