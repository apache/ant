/*
 * Copyright  2001-2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

package org.apache.tools.ant.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.condition.Os;

/**
 * Tests for org.apache.tools.ant.util.FileUtils.
 *
 */
public class FileUtilsTest extends TestCase {

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    private File removeThis;
    private String root;

    public FileUtilsTest(String name) {
        super(name);
    }

    public void setUp() {
        // Windows adds the drive letter in uppercase, unless you run Cygwin
        root = new File(File.separator).getAbsolutePath().toUpperCase();
    }

    public void tearDown() {
        if (removeThis != null && removeThis.exists()) {
            removeThis.delete();
        }
    }

    /**
     * test modification.
     * Since Ant1.7, the method being tested no longer uses
     * reflection to provide backwards support to Java1.1, so this
     * test is not so critical. But it does explore file system
     * behaviour and will help catch any regression in Java itself,
     * so is worth retaining.
     * @see FileUtils#setFileLastModified(java.io.File, long)
     * @throws IOException
     */
    public void testSetLastModified() throws IOException {
        removeThis = new File("dummy");
        FileOutputStream fos = new FileOutputStream(removeThis);
        fos.write(new byte[0]);
        fos.close();
        long modTime = removeThis.lastModified();
        assertTrue(modTime != 0);

        /*
         * Sleep for some time to make sure a touched file would get a
         * more recent timestamp according to the file system's
         * granularity (should be > 2s to account for Windows FAT).
         */
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ie) {
            fail(ie.getMessage());
        }

        FILE_UTILS.setFileLastModified(removeThis, -1);
        long secondModTime = removeThis.lastModified();
        assertTrue(secondModTime > modTime);

        // number of milliseconds in a day
        final int millisperday=24 * 3600 * 1000;
        // in a previous version, the date of the file was set to 123456
        // milliseconds since 01.01.1970
        // it did not work on a computer running JDK 1.4.1_02 + Windows 2000
        FILE_UTILS.setFileLastModified(removeThis, secondModTime + millisperday);
        long thirdModTime = removeThis.lastModified();
        /*
         * I would love to compare this with 123456, but depending on
         * the filesystems granularity it can take an arbitrary value.
         *
         * Just assert the time has changed.
         */
        assertTrue(thirdModTime != secondModTime);
    }

    public void testResolveFile() {
        /*
         * Start with simple absolute file names.
         */
        assertEquals(File.separator,
                     FILE_UTILS.resolveFile(null, "/").getPath());
        assertEquals(File.separator,
                     FILE_UTILS.resolveFile(null, "\\").getPath());

        /*
         * throw in drive letters
         */
        String driveSpec = "C:";
        assertEquals(driveSpec + "\\",
                     FILE_UTILS.resolveFile(null, driveSpec + "/").getPath());
        assertEquals(driveSpec + "\\",
                     FILE_UTILS.resolveFile(null, driveSpec + "\\").getPath());
        String driveSpecLower = "c:";
        assertEquals(driveSpec + "\\",
                     FILE_UTILS.resolveFile(null, driveSpecLower + "/").getPath());
        assertEquals(driveSpec + "\\",
                     FILE_UTILS.resolveFile(null, driveSpecLower + "\\").getPath());
        /*
         * promised to eliminate consecutive slashes after drive letter.
         */
        assertEquals(driveSpec + "\\",
                     FILE_UTILS.resolveFile(null, driveSpec + "/////").getPath());
        assertEquals(driveSpec + "\\",
                     FILE_UTILS.resolveFile(null, driveSpec + "\\\\\\\\\\\\").getPath());

        if (Os.isFamily("netware")) {
            /*
             * throw in NetWare volume names
             */
            driveSpec = "SYS:";
            assertEquals(driveSpec,
                         FILE_UTILS.resolveFile(null, driveSpec + "/").getPath());
            assertEquals(driveSpec,
                         FILE_UTILS.resolveFile(null, driveSpec + "\\").getPath());
            driveSpecLower = "sys:";
            assertEquals(driveSpec,
                         FILE_UTILS.resolveFile(null, driveSpecLower + "/").getPath());
            assertEquals(driveSpec,
                         FILE_UTILS.resolveFile(null, driveSpecLower + "\\").getPath());
            /*
             * promised to eliminate consecutive slashes after drive letter.
             */
            assertEquals(driveSpec,
                         FILE_UTILS.resolveFile(null, driveSpec + "/////").getPath());
            assertEquals(driveSpec,
                         FILE_UTILS.resolveFile(null, driveSpec + "\\\\\\\\\\\\").getPath());
        }

        /*
         * Now test some relative file name magic.
         */
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.resolveFile(new File(localize("/1/2/3")), "4").getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.resolveFile(new File(localize("/1/2/3")), "./4").getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.resolveFile(new File(localize("/1/2/3")), ".\\4").getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.resolveFile(new File(localize("/1/2/3")), "./.\\4").getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.resolveFile(new File(localize("/1/2/3")), "../3/4").getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.resolveFile(new File(localize("/1/2/3")), "..\\3\\4").getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.resolveFile(new File(localize("/1/2/3")), "../../5/.././2/./3/6/../4").getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.resolveFile(new File(localize("/1/2/3")), "..\\../5/..\\./2/./3/6\\../4").getPath());

        try {
            FILE_UTILS.resolveFile(new File(localize("/1")), "../../b");
            fail("successfully crawled beyond the filesystem root");
        } catch (BuildException e) {
            // Expected Exception caught
        }

    }

    public void testNormalize() {
        /*
         * Start with simple absolute file names.
         */
        assertEquals(File.separator,
                     FILE_UTILS.normalize("/").getPath());
        assertEquals(File.separator,
                     FILE_UTILS.normalize("\\").getPath());

        /*
         * throw in drive letters
         */
        String driveSpec = "C:";
        assertEquals(driveSpec,
                     FILE_UTILS.normalize(driveSpec).getPath());
        assertEquals(driveSpec + "\\",
                     FILE_UTILS.normalize(driveSpec + "/").getPath());
        assertEquals(driveSpec + "\\",
                     FILE_UTILS.normalize(driveSpec + "\\").getPath());
        String driveSpecLower = "c:";
        assertEquals(driveSpec + "\\",
                     FILE_UTILS.normalize(driveSpecLower + "/").getPath());
        assertEquals(driveSpec + "\\",
                     FILE_UTILS.normalize(driveSpecLower + "\\").getPath());
        /*
         * promised to eliminate consecutive slashes after drive letter.
         */
        assertEquals(driveSpec + "\\",
                     FILE_UTILS.normalize(driveSpec + "/////").getPath());
        assertEquals(driveSpec + "\\",
                     FILE_UTILS.normalize(driveSpec + "\\\\\\\\\\\\").getPath());

        if (Os.isFamily("netware")) {
            /*
             * throw in NetWare volume names
             */
            driveSpec = "SYS:";
            assertEquals(driveSpec,
                         FILE_UTILS.normalize(driveSpec).getPath());
            assertEquals(driveSpec,
                         FILE_UTILS.normalize(driveSpec + "/").getPath());
            assertEquals(driveSpec,
                         FILE_UTILS.normalize(driveSpec + "\\").getPath());
            driveSpecLower = "sys:";
            assertEquals(driveSpec,
                         FILE_UTILS.normalize(driveSpecLower).getPath());
            assertEquals(driveSpec,
                         FILE_UTILS.normalize(driveSpecLower + "/").getPath());
            assertEquals(driveSpec,
                         FILE_UTILS.normalize(driveSpecLower + "\\").getPath());
            assertEquals(driveSpec + "\\junk",
                         FILE_UTILS.normalize(driveSpecLower + "\\junk").getPath());
            /*
             * promised to eliminate consecutive slashes after drive letter.
             */
            assertEquals(driveSpec,
                         FILE_UTILS.normalize(driveSpec + "/////").getPath());
            assertEquals(driveSpec,
                         FILE_UTILS.normalize(driveSpec + "\\\\\\\\\\\\").getPath());
        }

        /*
         * Now test some relative file name magic.
         */
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.normalize(localize("/1/2/3/4")).getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.normalize(localize("/1/2/3/./4")).getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.normalize(localize("/1/2/3/.\\4")).getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.normalize(localize("/1/2/3/./.\\4")).getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.normalize(localize("/1/2/3/../3/4")).getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.normalize(localize("/1/2/3/..\\3\\4")).getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.normalize(localize("/1/2/3/../../5/.././2/./3/6/../4")).getPath());
        assertEquals(localize("/1/2/3/4"),
                     FILE_UTILS.normalize(localize("/1/2/3/..\\../5/..\\./2/./3/6\\../4")).getPath());

        try {
            FILE_UTILS.normalize("foo");
            fail("foo is not an absolute path");
        } catch (BuildException e) {
            // Expected exception caught
        }

        try {
            FILE_UTILS.normalize(localize("/1/../../b"));
            fail("successfully crawled beyond the filesystem root");
        } catch (BuildException e) {
            // Expected exception caught
        }
    }

    /**
     * Test handling of null arguments.
     */
    public void testNullArgs() {
        try {
            FILE_UTILS.normalize(null);
            fail("successfully normalized a null-file");
        } catch (NullPointerException npe) {
            // Expected exception caught
        }

        File f = FILE_UTILS.resolveFile(null, "a");
        assertEquals(f, new File("a"));
    }

    /**
     * Test createTempFile
     */
    public void testCreateTempFile() {
        File parent = new File((new File("/tmp")).getAbsolutePath());
        File tmp1 = FILE_UTILS.createTempFile("pre", ".suf", parent);
        assertTrue("new file", !tmp1.exists());

        String name = tmp1.getName();
        assertTrue("starts with pre", name.startsWith("pre"));
        assertTrue("ends with .suf", name.endsWith(".suf"));
        assertEquals("is inside parent dir",
                     parent.getAbsolutePath(),
                     tmp1.getParent());

        File tmp2 = FILE_UTILS.createTempFile("pre", ".suf", parent);
        assertTrue("files are different",
                   !tmp1.getAbsolutePath().equals(tmp2.getAbsolutePath()));

        // null parent dir
        File tmp3 = FILE_UTILS.createTempFile("pre", ".suf", null);
        String  tmploc = System.getProperty("java.io.tmpdir");
        assertEquals((new File(tmploc, tmp3.getName())).getAbsolutePath(),
                     tmp3.getAbsolutePath());
    }

    /**
     * Test contentEquals
     */
    public void testContentEquals() throws IOException {
        assertTrue("Non existing files", FILE_UTILS.contentEquals(new File(System.getProperty("root"), "foo"),
                                                          new File(System.getProperty("root"), "bar")));
        assertTrue("One exists, the other one doesn\'t",
                   !FILE_UTILS.contentEquals(new File(System.getProperty("root"), "foo"), new File(System.getProperty("root"), "build.xml")));
        assertTrue("Don\'t compare directories",
                   !FILE_UTILS.contentEquals(new File(System.getProperty("root"), "src"), new File(System.getProperty("root"), "src")));
        assertTrue("File equals itself",
                   FILE_UTILS.contentEquals(new File(System.getProperty("root"), "build.xml"),
                                    new File(System.getProperty("root"), "build.xml")));
        assertTrue("Files are different",
                   !FILE_UTILS.contentEquals(new File(System.getProperty("root"), "build.xml"),
                                     new File(System.getProperty("root"), "docs.xml")));
    }

    /**
     * Test createNewFile
     */
    public void testCreateNewFile() throws IOException {
        removeThis = new File("dummy");
        assertTrue(!removeThis.exists());
        FILE_UTILS.createNewFile(removeThis);
        assertTrue(removeThis.exists());
    }

    /**
     * Test removeLeadingPath.
     */
    public void testRemoveLeadingPath() {
        assertEquals("bar", FILE_UTILS.removeLeadingPath(new File("/foo"),
                                                 new File("/foo/bar")));
        assertEquals("bar", FILE_UTILS.removeLeadingPath(new File("/foo/"),
                                                 new File("/foo/bar")));
        assertEquals("bar", FILE_UTILS.removeLeadingPath(new File("\\foo"),
                                                 new File("\\foo\\bar")));
        assertEquals("bar", FILE_UTILS.removeLeadingPath(new File("\\foo\\"),
                                                 new File("\\foo\\bar")));
        assertEquals("bar", FILE_UTILS.removeLeadingPath(new File("c:/foo"),
                                                 new File("c:/foo/bar")));
        assertEquals("bar", FILE_UTILS.removeLeadingPath(new File("c:/foo/"),
                                                 new File("c:/foo/bar")));
        assertEquals("bar", FILE_UTILS.removeLeadingPath(new File("c:\\foo"),
                                                 new File("c:\\foo\\bar")));
        assertEquals("bar", FILE_UTILS.removeLeadingPath(new File("c:\\foo\\"),
                                                 new File("c:\\foo\\bar")));
        assertEqualsIgnoreDriveCase(FILE_UTILS.normalize("/bar").getAbsolutePath(),
                     FILE_UTILS.removeLeadingPath(new File("/foo"), new File("/bar")));
        assertEqualsIgnoreDriveCase(FILE_UTILS.normalize("/foobar").getAbsolutePath(),
                     FILE_UTILS.removeLeadingPath(new File("/foo"), new File("/foobar")));
        // bugzilla report 19979
        assertEquals("", FILE_UTILS.removeLeadingPath(new File("/foo/bar"),
                                              new File("/foo/bar")));
        assertEquals("", FILE_UTILS.removeLeadingPath(new File("/foo/bar"),
                                              new File("/foo/bar/")));
        assertEquals("", FILE_UTILS.removeLeadingPath(new File("/foo/bar/"),
                                              new File("/foo/bar/")));
        assertEquals("", FILE_UTILS.removeLeadingPath(new File("/foo/bar/"),
                                              new File("/foo/bar")));

        String expected = "foo/bar".replace('\\', File.separatorChar)
            .replace('/', File.separatorChar);
        assertEquals(expected, FILE_UTILS.removeLeadingPath(new File("/"),
                                                    new File("/foo/bar")));
        assertEquals(expected, FILE_UTILS.removeLeadingPath(new File("c:/"),
                                                    new File("c:/foo/bar")));
        assertEquals(expected, FILE_UTILS.removeLeadingPath(new File("c:\\"),
                                                    new File("c:\\foo\\bar")));
    }

    /**
     * test toUri
     */
    public void testToURI() {
        String dosRoot = null;
        if (Os.isFamily("dos")) {
            dosRoot = System.getProperty("user.dir").charAt(0) + ":/";
        }
        else
        {
            dosRoot = "";
        }
        if (Os.isFamily("dos")) {
            assertEquals("file:///C:/foo", FILE_UTILS.toURI("c:\\foo"));
        }
        if (Os.isFamily("netware")) {
            assertEquals("file:///SYS:/foo", FILE_UTILS.toURI("sys:\\foo"));
        }
        assertEquals("file:///" + dosRoot + "foo", FILE_UTILS.toURI("/foo"));
        /* May fail if the directory ${user.dir}/foo/ exists
         * (and anyway is the tested behavior actually desirable?):
        assertEquals("file:./foo",  fu.toURI("./foo"));
         */
        assertEquals("file:///" + dosRoot + "foo", FILE_UTILS.toURI("\\foo"));
        /* See above:
        assertEquals("file:./foo",  fu.toURI(".\\foo"));
         */
        assertEquals("file:///" + dosRoot + "foo%20bar", FILE_UTILS.toURI("/foo bar"));
        assertEquals("file:///" + dosRoot + "foo%20bar", FILE_UTILS.toURI("\\foo bar"));
        assertEquals("file:///" + dosRoot + "foo%23bar", FILE_UTILS.toURI("/foo#bar"));
        assertEquals("file:///" + dosRoot + "foo%23bar", FILE_UTILS.toURI("\\foo#bar"));
    }

    /**
     * test fromUri
     */
    public void testFromURI() {
        if (Os.isFamily("netware")) {
            assertEqualsIgnoreDriveCase("SYS:\\foo", FILE_UTILS.fromURI("file:///sys:/foo"));
        }
        if (Os.isFamily("dos")) {
            assertEqualsIgnoreDriveCase("C:\\foo", FILE_UTILS.fromURI("file:///c:/foo"));
        }
        assertEqualsIgnoreDriveCase(localize("/foo"), FILE_UTILS.fromURI("file:///foo"));
        assertEquals("." + File.separator + "foo",
                     FILE_UTILS.fromURI("file:./foo"));
        assertEqualsIgnoreDriveCase(localize("/foo bar"), FILE_UTILS.fromURI("file:///foo%20bar"));
        assertEqualsIgnoreDriveCase(localize("/foo#bar"), FILE_UTILS.fromURI("file:///foo%23bar"));
    }

    public void testModificationTests() {

        //get a time
        long firstTime=System.currentTimeMillis();
        //add some time. We assume no OS has a granularity this bad
        long secondTime=firstTime+60000;
/*
        assertTrue("same timestamp is up to date",
                fu.isUpToDate(firstTime, firstTime));
                */

        //check that older is up to date with a newer dest
        assertTrue("older source files are up to date",
                FILE_UTILS.isUpToDate(firstTime,secondTime));
        //check that older is up to date with a newer dest
        assertFalse("newer source files are no up to date",
                FILE_UTILS.isUpToDate(secondTime, firstTime));

        assertTrue("-1 dest timestamp implies nonexistence",
                !FILE_UTILS.isUpToDate(firstTime,-1L));
    }

    /**
     * adapt file separators to local conventions
     */
    private String localize(String path) {
        path = root + path.substring(1);
        return path.replace('\\', File.separatorChar).replace('/', File.separatorChar);
    }
    /**
     * convenience method
     * normalize brings the drive in uppercase
     * the drive letter is in lower case under cygwin
     * calling this method allows tests where normalize is called to pass under cygwin
     */
    private void assertEqualsIgnoreDriveCase(String s1, String s2) {
        if (Os.isFamily("dos") && s1.length()>=1 && s2.length()>=1) {
            StringBuffer sb1= new StringBuffer(s1);
            StringBuffer sb2= new StringBuffer(s2);
            sb1.setCharAt(0,Character.toUpperCase(s1.charAt(0)));
            sb2.setCharAt(0,Character.toUpperCase(s2.charAt(0)));
            assertEquals(sb1.toString(),sb2.toString());
        }   else {
            assertEquals(s1,s2);
        }
    }
}
