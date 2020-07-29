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

package org.apache.tools.ant.types;

import java.io.File;
import java.nio.file.Paths;
import java.util.Locale;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.MagicTestNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * JUnit testcases for org.apache.tools.ant.types.Path
 *
 */

public class PathTest {

    public static boolean isUnixStyle = File.pathSeparatorChar == ':';
    public static boolean isNetWare = Os.isFamily("netware");

    private Project project;
    private Path p;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        project = new Project();
        if (System.getProperty(MagicTestNames.TEST_ROOT_DIRECTORY) != null) {
            project.setBasedir(System.getProperty(MagicTestNames.TEST_ROOT_DIRECTORY));
        }
        p = new Path(project);
    }

    // actually tests constructor as well as setPath
    @Test
    public void testConstructorUnixStyle() {
        p = new Path(project, "/a:/b");
        String[] l = p.list();
        assertEquals("two items, Unix style", 2, l.length);
        if (isUnixStyle) {
            assertEquals("/a", l[0]);
            assertEquals("/b", l[1]);
        } else if (isNetWare) {
            assertEquals("\\a", l[0]);
            assertEquals("\\b", l[1]);
        } else {
            String base = new File(File.separator).getAbsolutePath();
            assertEquals(base + "a", l[0]);
            assertEquals(base + "b", l[1]);
        }
    }

    @Test
    public void testRelativePathUnixStyle() {
        project.setBasedir(new File(project.getBaseDir(), "src/etc").getAbsolutePath());
        p = new Path(project, "..:testcases");
        String[] l = p.list();
        assertEquals("two items, Unix style", 2, l.length);
        if (isUnixStyle) {
           assertThat("test resolved relative to src/etc",
                 l[0], endsWith("/src"));
           assertThat("test resolved relative to src/etc",
                 l[1], endsWith("/src/etc/testcases"));
        } else if (isNetWare) {
           assertThat("test resolved relative to src/etc",
                 l[0], endsWith("\\src"));
           assertThat("test resolved relative to src/etc",
                 l[1], endsWith("\\src\\etc\\testcases"));
        } else {
           assertThat("test resolved relative to src/etc",
                 l[0], endsWith("\\src"));
           assertThat("test resolved relative to src/etc",
                 l[1], endsWith("\\src\\etc\\testcases"));
        }
    }

    @Test
    public void testConstructorWindowsStyleTwoItemsNoDrive() {
        p = new Path(project, "\\a;\\b");
        String[] l = p.list();
        assertEquals("two items, DOS style", 2, l.length);
        if (isUnixStyle) {
            assertEquals("/a", l[0]);
            assertEquals("/b", l[1]);
        } else if (isNetWare) {
            assertEquals("\\a", l[0]);
            assertEquals("\\b", l[1]);
        } else {
            String base = new File(File.separator).getAbsolutePath();
            assertEquals(base + "a", l[0]);
            assertEquals(base + "b", l[1]);
        }
    }

    @Test
    public void testConstructorWindowsStyle() {
        p = new Path(project, "c:\\test");
        String[] l = p.list();
        if (isUnixStyle) {
            assertEquals("no drives on Unix", 2, l.length);
            assertThat("c resolved relative to project's basedir",
                   l[0], endsWith("/c"));
            assertEquals("/test", l[1]);
        } else if (isNetWare) {
            assertEquals("volumes on NetWare", 1, l.length);
            assertEquals("c:\\test", l[0].toLowerCase(Locale.US));
        } else {
            assertEquals("drives on DOS", 1, l.length);
            assertEquals("c:\\test", l[0].toLowerCase(Locale.US));
        }
    }

    @Test
    public void testConstructorWindowsStyleTwoItems() {
        p = new Path(project, "c:\\test;d:\\programs");
        String[] l = p.list();
        if (isUnixStyle) {
            assertEquals("no drives on Unix", 4, l.length);
            assertThat("c resolved relative to project's basedir",
                   l[0], endsWith("/c"));
            assertEquals("/test", l[1]);
            assertThat("d resolved relative to project's basedir",
                   l[2], endsWith("/d"));
            assertEquals("/programs", l[3]);
        } else if (isNetWare) {
            assertEquals("volumes on NetWare", 2, l.length);
            assertEquals("c:\\test", l[0].toLowerCase(Locale.US));
            assertEquals("d:\\programs", l[1].toLowerCase(Locale.US));
        } else {
            assertEquals("drives on DOS", 2, l.length);
            assertEquals("c:\\test", l[0].toLowerCase(Locale.US));
            assertEquals("d:\\programs", l[1].toLowerCase(Locale.US));
        }
    }

    @Test
    public void testConstructorWindowsStyleUnixFS() {
        p = new Path(project, "c:/test");
        String[] l = p.list();
        if (isUnixStyle) {
            assertEquals("no drives on Unix", 2, l.length);
            assertThat("c resolved relative to project's basedir",
                   l[0], endsWith("/c"));
            assertEquals("/test", l[1]);
        } else if (isNetWare) {
            assertEquals("volumes on NetWare", 1, l.length);
            assertEquals("c:\\test", l[0].toLowerCase(Locale.US));
        } else {
            assertEquals("drives on DOS", 1, l.length);
            assertEquals("c:\\test", l[0].toLowerCase(Locale.US));
        }
    }

    @Test
    public void testConstructorWindowsStyleUnixFSTwoItems() {
        p = new Path(project, "c:/test;d:/programs");
        String[] l = p.list();
        if (isUnixStyle) {
            assertEquals("no drives on Unix", 4, l.length);
            assertThat("c resolved relative to project's basedir",
                   l[0], endsWith("/c"));
            assertEquals("/test", l[1]);
            assertThat("d resolved relative to project's basedir",
                   l[2], endsWith("/d"));
            assertEquals("/programs", l[3]);
        } else if (isNetWare) {
            assertEquals("volumes on NetWare", 2, l.length);
            assertEquals("c:\\test", l[0].toLowerCase(Locale.US));
            assertEquals("d:\\programs", l[1].toLowerCase(Locale.US));
        } else {
            assertEquals("drives on DOS", 2, l.length);
            assertEquals("c:\\test", l[0].toLowerCase(Locale.US));
            assertEquals("d:\\programs", l[1].toLowerCase(Locale.US));
        }
    }

    // try a netware-volume length path, see how it is handled
    @Test
    public void testConstructorNetWareStyle() {
        p = new Path(project, "sys:\\test");
        String[] l = p.list();
        if (isUnixStyle) {
            assertEquals("no drives on Unix", 2, l.length);
            assertThat("sys resolved relative to project's basedir",
                   l[0], endsWith("/sys"));
            assertEquals("/test", l[1]);
        } else if (isNetWare) {
            assertEquals("sys:\\test", l[0].toLowerCase(Locale.US));
            assertEquals("volumes on NetWare", 1, l.length);
        } else {
            assertEquals("no multiple character-length volumes on Windows", 2, l.length);
            assertThat("sys resolved relative to project's basedir",
                   l[0], endsWith("\\sys"));
            assertThat("test resolved relative to project's basedir",
                   l[1], endsWith("\\test"));
        }
    }

    // try a multi-part netware-volume length path, see how it is handled
    @Test
    public void testConstructorNetWareStyleTwoItems() {
        p = new Path(project, "sys:\\test;dev:\\temp");
        String[] l = p.list();
        if (isUnixStyle) {
            assertEquals("no drives on Unix", 4, l.length);
            assertThat("sys resolved relative to project's basedir",
                   l[0], endsWith("/sys"));
            assertEquals("/test", l[1]);
            assertThat("dev resolved relative to project's basedir",
                   l[2], endsWith("/dev"));
            assertEquals("/temp", l[3]);
        } else if (isNetWare) {
            assertEquals("volumes on NetWare", 2, l.length);
            assertEquals("sys:\\test", l[0].toLowerCase(Locale.US));
            assertEquals("dev:\\temp", l[1].toLowerCase(Locale.US));
        } else {
            assertEquals("no multiple character-length volumes on Windows", 4, l.length);
            assertThat("sys resolved relative to project's basedir",
                   l[0], endsWith("\\sys"));
            assertThat("test resolved relative to project's basedir",
                   l[1], endsWith("\\test"));
            assertThat("dev resolved relative to project's basedir",
                   l[2], endsWith("\\dev"));
            assertThat("temp resolved relative to project's basedir",
                   l[3], endsWith("\\temp"));
        }
    }

    // try a netware-volume length path w/forward slash, see how it is handled
    @Test
    public void testConstructorNetWareStyleUnixFS() {
        p = new Path(project, "sys:/test");
        String[] l = p.list();
        if (isUnixStyle) {
            assertEquals("no drives on Unix", 2, l.length);
            assertThat("sys resolved relative to project's basedir",
                   l[0], endsWith("/sys"));
            assertEquals("/test", l[1]);
        } else if (isNetWare) {
            assertEquals("volumes on NetWare", 1, l.length);
            assertEquals("sys:\\test", l[0].toLowerCase(Locale.US));
        } else {
            assertEquals("no multiple character-length volumes on Windows", 2, l.length);
            assertThat("sys resolved relative to project's basedir",
                   l[0], endsWith("\\sys"));
            assertThat("test resolved relative to project's basedir",
                   l[1], endsWith("\\test"));
        }
    }

    // try a multi-part netware-volume length path w/forward slash, see how it is handled
    @Test
    public void testConstructorNetWareStyleUnixFSTwoItems() {
        p = new Path(project, "sys:/test;dev:/temp");
        String[] l = p.list();
        if (isUnixStyle) {
            assertEquals("no drives on Unix", 4, l.length);
            assertThat("sys resolved relative to project's basedir",
                   l[0], endsWith("/sys"));
            assertEquals("/test", l[1]);
            assertThat("dev resolved relative to project's basedir",
                   l[2], endsWith("/dev"));
            assertEquals("/temp", l[3]);
        } else if (isNetWare) {
            assertEquals("volumes on NetWare", 2, l.length);
            assertEquals("sys:\\test", l[0].toLowerCase(Locale.US));
            assertEquals("dev:\\temp", l[1].toLowerCase(Locale.US));
        } else {
            assertEquals("no multiple character-length volumes on Windows", 4, l.length);
            assertThat("sys resolved relative to project's basedir",
                   l[0], endsWith("\\sys"));
            assertThat("test resolved relative to project's basedir",
                   l[1], endsWith("\\test"));
            assertThat("dev resolved relative to project's basedir",
                   l[2], endsWith("\\dev"));
            assertThat("temp resolved relative to project's basedir",
                   l[3], endsWith("\\temp"));
         }
    }

    // try a multi-part netware-volume length path with UNIX
    // separator (this testcase if from an actual bug that was
    // found, in AvailableTest, which uses PathTokenizer)
    @Test
    public void testConstructorNetWareStyleUnixPS() {
        p = new Path(project, "SYS:\\JAVA/lib/rt.jar:SYS:\\JAVA/lib/classes.zip");
        String[] l = p.list();
        if (isUnixStyle) {
            assertEquals("no drives on Unix", 3, l.length);
            assertThat("sys resolved relative to project's basedir",
                   l[0], endsWith("/SYS"));
            assertEquals("/JAVA/lib/rt.jar", l[1]);
            assertEquals("/JAVA/lib/classes.zip", l[2]);
        } else if (isNetWare) {
            assertEquals("volumes on NetWare", 2, l.length);
            assertEquals("sys:\\java\\lib\\rt.jar", l[0].toLowerCase(Locale.US));
            assertEquals("sys:\\java\\lib\\classes.zip", l[1].toLowerCase(Locale.US));
        } else {
            assertEquals("no multiple character-length volumes on Windows", 3, l.length);
            assertThat("sys resolved relative to project's basedir",
                   l[0], endsWith("\\SYS"));
            assertThat("java/lib/rt.jar resolved relative to project's basedir",
                   l[1], endsWith("\\JAVA\\lib\\rt.jar"));
            assertThat("java/lib/classes.zip resolved relative to project's basedir",
                   l[2], endsWith("\\JAVA\\lib\\classes.zip"));
        }
    }

    @Test
    public void testConstructorMixedStyle() {
        p = new Path(project, "\\a;\\b:/c");
        String[] l = p.list();
        assertEquals("three items, mixed style", 3, l.length);
        if (isUnixStyle) {
            assertEquals("/a", l[0]);
            assertEquals("/b", l[1]);
            assertEquals("/c", l[2]);
        } else if (isNetWare) {
            assertEquals("\\a", l[0]);
            assertEquals("\\b", l[1]);
            assertEquals("\\c", l[2]);
        } else {
            String base = new File(File.separator).getAbsolutePath();
            assertEquals(base + "a", l[0]);
            assertEquals(base + "b", l[1]);
            assertEquals(base + "c", l[2]);
        }
    }

    @Test
    public void testSetLocation() {
        p.setLocation(new File(File.separatorChar + "a"));
        String[] l = p.list();
        if (isUnixStyle) {
            assertEquals(1, l.length);
            assertEquals("/a", l[0]);
        } else if (isNetWare) {
            assertEquals(1, l.length);
            assertEquals("\\a", l[0]);
        } else {
            assertEquals(1, l.length);
            assertEquals(":\\a", l[0].substring(1));
        }
    }

    @Test
    public void testAppending() {
        p = new Path(project, "/a:/b");
        String[] l = p.list();
        assertEquals("2 after construction", 2, l.length);
        p.setLocation(new File("/c"));
        l = p.list();
        assertEquals("3 after setLocation", 3, l.length);
        p.setPath("\\d;\\e");
        l = p.list();
        assertEquals("5 after setPath", 5, l.length);
        p.append(new Path(project, "\\f"));
        l = p.list();
        assertEquals("6 after append", 6, l.length);
        p.createPath().setLocation(new File("/g"));
        l = p.list();
        assertEquals("7 after append", 7, l.length);
    }

    @Test
    public void testEmptyPath() {
        p = new Path(project, "");
        String[] l = p.list();
        assertEquals("0 after construction", 0, l.length);
        p.setPath("");
        l = p.list();
        assertEquals("0 after setPath", 0, l.length);
        p.append(new Path(project));
        l = p.list();
        assertEquals("0 after append", 0, l.length);
        p.createPath();
        l = p.list();
        assertEquals("0 after append", 0, l.length);
    }

    @Test
    public void testUnique() {
        p = new Path(project, "/a:/a");
        String[] l = p.list();
        assertEquals("1 after construction", 1, l.length);
        String base = new File(File.separator).getAbsolutePath();
        p.setLocation(new File(base, "a"));
        l = p.list();
        assertEquals("1 after setLocation", 1, l.length);
        p.setPath("\\a;/a");
        l = p.list();
        assertEquals("1 after setPath", 1, l.length);
        p.append(new Path(project, "/a;\\a:\\a"));
        l = p.list();
        assertEquals("1 after append", 1, l.length);
        p.createPath().setPath("\\a:/a");
        l = p.list();
        assertEquals("1 after append", 1, l.length);
    }

    @Test
    public void testEmptyElementSetRefid() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        p = new Path(project, "/a:/a");
        p.setRefid(new Reference(project, "dummyref"));
    }

    @Test
    public void testEmptyElementSetLocationThenRefid() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        p.setLocation(new File("/a"));
        p.setRefid(new Reference(project, "dummyref"));
    }

    @Test
    public void testUseExistingRefid() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        Path another = new Path(project, "/a:/a");
        project.addReference("dummyref", another);
        p.setRefid(new Reference(project, "dummyref"));
        p.setLocation(new File("/a"));
    }

    @Test
    public void testEmptyElementIfIsReferenceAttr() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        p.setRefid(new Reference(project, "dummyref"));
        p.setPath("/a;\\a");
    }

    @Test
    public void testEmptyElementCreatePath() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify nested elements when using refid");
        p.setRefid(new Reference(project, "dummyref"));
        p.createPath();
    }

    @Test
    public void testEmptyElementCreatePathElement() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify nested elements when using refid");
        p.setRefid(new Reference(project, "dummyref"));
        p.createPathElement();
    }

    @Test
    public void testEmptyElementAddFileset() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify nested elements when using refid");
        p.setRefid(new Reference(project, "dummyref"));
        p.addFileset(new FileSet());
    }

    @Test
    public void testEmptyElementAddFilelist() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify nested elements when using refid");
        p.setRefid(new Reference(project, "dummyref"));
        p.addFilelist(new FileList());
    }

    @Test
    public void testEmptyElementAddDirset() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify nested elements when using refid");
        p.setRefid(new Reference(project, "dummyref"));
        p.addDirset(new DirSet());
    }

    @Test
    public void testCircularReferenceCheck() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("This data type contains a circular reference.");
        project.addReference("dummy", p);
        p.setRefid(new Reference(project, "dummy"));
        p.list();
    }

    @Test
    public void testLoopReferenceCheck() {
        // dummy1 --> dummy2 --> dummy3 --> dummy1
        thrown.expect(BuildException.class);
        thrown.expectMessage("This data type contains a circular reference.");
        project.addReference("dummy1", p);
        Path pa = p.createPath();
        project.addReference("dummy2", pa);
        Path pb = pa.createPath();
        project.addReference("dummy3", pb);
        pb.setRefid(new Reference(project, "dummy1"));
        p.list();
    }

    @Test
    public void testLoopReferenceCheckWithLocation() {
        // dummy1 --> dummy2 --> dummy3 (with Path "/a")
        project.addReference("dummy1", p);
        Path pa = p.createPath();
        project.addReference("dummy2", pa);
        Path pb = pa.createPath();
        project.addReference("dummy3", pb);
        pb.setLocation(new File("/a"));
        String[] l = p.list();
        assertEquals("One element buried deep inside a nested path structure",
                     1, l.length);
        if (isUnixStyle) {
            assertEquals("/a", l[0]);
        } else if (isNetWare) {
            assertEquals("\\a", l[0]);
        } else {
            assertEquals(":\\a", l[0].substring(1));
        }
    }

    @Test
    public void testFileList() {
        FileList f = new FileList();
        f.setProject(project);
        f.setDir(project.resolveFile("."));
        f.setFiles("build.xml");
        p.addFilelist(f);
        String[] l = p.list();
        assertEquals(1, l.length);
        assertEquals(project.resolveFile("build.xml").getAbsolutePath(), l[0]);
    }

    @Test
    public void testFileSet() {
        FileSet f = new FileSet();
        f.setProject(project);
        f.setDir(project.resolveFile("."));
        f.setIncludes("build.xml");
        p.addFileset(f);
        String[] l = p.list();
        assertEquals(1, l.length);
        assertEquals(project.resolveFile("build.xml").getAbsolutePath(), l[0]);
    }

    @Test
    public void testDirSet() {
        DirSet d = new DirSet();
        d.setProject(project);
        d.setDir(project.resolveFile("."));
        String s = System.getProperty("build.tests.value");
        assertNotNull("build.tests.value not set", s);
        String n = Paths.get(s).getParent().getFileName().toString().equals("ant")
                ? "target" : "build";
        d.setIncludes(n);
        p.addDirset(d);
        String[] l = p.list();
        assertEquals(1, l.length);
        assertEquals(project.resolveFile(n).getAbsolutePath(), l[0]);
    }

    @Test
    public void testRecursion() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("circular");
        try {
            p.append(p);
        } finally {
            assertEquals(0, p.list().length);
        }
    }

}
