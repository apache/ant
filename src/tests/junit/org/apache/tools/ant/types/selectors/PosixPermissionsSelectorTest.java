package org.apache.tools.ant.types.selectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

@RunWith(Enclosed.class)
public class PosixPermissionsSelectorTest {

    @RunWith(Parameterized.class)
    public static class IllegalArgumentTest {

        private PosixPermissionsSelector s;

        // requires JUnit 4.12
        @Parameterized.Parameters(name = "illegal argument: |{0}|")
        public static Collection<String> data() {
            return Arrays.asList("855", "4555", "-rwxr-xr-x", "xrwr-xr-x");
        }

        @Parameterized.Parameter
        public String argument;

        @Before
        public void setUp() {
            assumeTrue("no POSIX", Os.isFamily("unix"));
            s = new PosixPermissionsSelector();
        }

        @Test(expected = BuildException.class)
        public void test() {
            s.setPermissions(argument);
        }
    }

    @RunWith(Parameterized.class)
    public static class LegalArgumentTest {

        private PosixPermissionsSelector s;

        @Rule
        public TemporaryFolder folder = new TemporaryFolder();

        // requires JUnit 4.12
        @Parameterized.Parameters(name = "legal argument: |{0}|")
        public static Collection<String> data() {
            return Arrays.asList("755", "rwxr-xr-x");
        }

        @Parameterized.Parameter
        public String argument;

        @Before
        public void setUp() {
            assumeTrue("No POSIX", Os.isFamily("unix"));
            s = new PosixPermissionsSelector();
        }

        @Test
        public void PosixPermissionsIsTrueForSelf() throws Exception {
            s.setPermissions(argument);
            assertTrue(s.isSelected(null, null, folder.newFolder()));
        }
    }

}
