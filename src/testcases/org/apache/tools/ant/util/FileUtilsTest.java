/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant.util;

import java.io.*;

import junit.framework.TestCase;

import org.apache.tools.ant.BuildException;

/**
 * Tests for org.apache.tools.ant.util.FileUtils.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 */
public class FileUtilsTest extends TestCase {

    private FileUtils fu;
    private File removeThis;
    private String root;

    public FileUtilsTest(String name) {
        super(name);
    }

    public void setUp() {
        fu = FileUtils.newFileUtils();
        // Windows adds the drive letter in uppercase, unless you run Cygnus
        root = new File(File.separator).getAbsolutePath().toUpperCase();
    }

    public void tearDown() {
        if (removeThis != null && removeThis.exists()) {
            removeThis.delete();
        }
    }

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
            Thread.currentThread().sleep(5000);
        } catch (InterruptedException ie) {
            fail(ie.getMessage());
        }

        fu.setFileLastModified(removeThis, -1);
        long secondModTime = removeThis.lastModified();
        try {
            Class.forName("java.lang.ThreadLocal");
            assertTrue(secondModTime > modTime);
        } catch (ClassNotFoundException e) {
            // JDK 1.1
            assertEquals(modTime, secondModTime);
        }
        

        fu.setFileLastModified(removeThis, 123456);
        long thirdModTime = removeThis.lastModified();
        try {
            Class.forName("java.lang.ThreadLocal");
            /*
             * I would love to compare this with 123456, but depending on
             * the filesystems granularity it can take an arbitrary value.
             *
             * Just assert the time has changed.
             */
            assertTrue(thirdModTime != secondModTime);
        } catch (ClassNotFoundException e) {
            // JDK 1.1
            assertEquals(modTime, thirdModTime);
        }
    }

    public void testResolveFile() {
        /*
         * Start with simple absolute file names.
         */
        assertEquals(File.separator, 
                     fu.resolveFile(null, "/").getPath());
        assertEquals(File.separator, 
                     fu.resolveFile(null, "\\").getPath());

        /*
         * throw in drive letters
         */
        String driveSpec = "C:";
        assertEquals(driveSpec + "\\", 
                     fu.resolveFile(null, driveSpec + "/").getPath());
        assertEquals(driveSpec + "\\", 
                     fu.resolveFile(null, driveSpec + "\\").getPath());
        String driveSpecLower = "c:";
        assertEquals(driveSpec + "\\", 
                     fu.resolveFile(null, driveSpecLower + "/").getPath());
        assertEquals(driveSpec + "\\", 
                     fu.resolveFile(null, driveSpecLower + "\\").getPath());
        /*
         * promised to eliminate consecutive slashes after drive letter.
         */
        assertEquals(driveSpec + "\\", 
                     fu.resolveFile(null, driveSpec + "/////").getPath());
        assertEquals(driveSpec + "\\", 
                     fu.resolveFile(null, driveSpec + "\\\\\\\\\\\\").getPath());

        /*
         * Now test some relative file name magic.
         */
        assertEquals(localize("/1/2/3/4"),
                     fu.resolveFile(new File(localize("/1/2/3")), "4").getPath());
        assertEquals(localize("/1/2/3/4"),
                     fu.resolveFile(new File(localize("/1/2/3")), "./4").getPath());
        assertEquals(localize("/1/2/3/4"),
                     fu.resolveFile(new File(localize("/1/2/3")), ".\\4").getPath());
        assertEquals(localize("/1/2/3/4"),
                     fu.resolveFile(new File(localize("/1/2/3")), "./.\\4").getPath());
        assertEquals(localize("/1/2/3/4"),
                     fu.resolveFile(new File(localize("/1/2/3")), "../3/4").getPath());
        assertEquals(localize("/1/2/3/4"),
                     fu.resolveFile(new File(localize("/1/2/3")), "..\\3\\4").getPath());
        assertEquals(localize("/1/2/3/4"),
                     fu.resolveFile(new File(localize("/1/2/3")), "../../5/.././2/./3/6/../4").getPath());
        assertEquals(localize("/1/2/3/4"),
                     fu.resolveFile(new File(localize("/1/2/3")), "..\\../5/..\\./2/./3/6\\../4").getPath());

        try {
            fu.resolveFile(new File(localize("/1")), "../../b");
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
                     fu.normalize("/").getPath());
        assertEquals(File.separator, 
                     fu.normalize("\\").getPath());

        /*
         * throw in drive letters
         */
        String driveSpec = "C:";
        assertEquals(driveSpec + "\\", 
                     fu.normalize(driveSpec + "/").getPath());
        assertEquals(driveSpec + "\\", 
                     fu.normalize(driveSpec + "\\").getPath());
        String driveSpecLower = "c:";
        assertEquals(driveSpec + "\\", 
                     fu.normalize(driveSpecLower + "/").getPath());
        assertEquals(driveSpec + "\\", 
                     fu.normalize(driveSpecLower + "\\").getPath());
        /*
         * promised to eliminate consecutive slashes after drive letter.
         */
        assertEquals(driveSpec + "\\", 
                     fu.normalize(driveSpec + "/////").getPath());
        assertEquals(driveSpec + "\\", 
                     fu.normalize(driveSpec + "\\\\\\\\\\\\").getPath());

        /*
         * Now test some relative file name magic.
         */
        assertEquals(localize("/1/2/3/4"),
                     fu.normalize(localize("/1/2/3/4")).getPath());
        assertEquals(localize("/1/2/3/4"),
                     fu.normalize(localize("/1/2/3/./4")).getPath());
        assertEquals(localize("/1/2/3/4"),
                     fu.normalize(localize("/1/2/3/.\\4")).getPath());
        assertEquals(localize("/1/2/3/4"),
                     fu.normalize(localize("/1/2/3/./.\\4")).getPath());
        assertEquals(localize("/1/2/3/4"),
                     fu.normalize(localize("/1/2/3/../3/4")).getPath());
        assertEquals(localize("/1/2/3/4"),
                     fu.normalize(localize("/1/2/3/..\\3\\4")).getPath());
        assertEquals(localize("/1/2/3/4"),
                     fu.normalize(localize("/1/2/3/../../5/.././2/./3/6/../4")).getPath());
        assertEquals(localize("/1/2/3/4"),
                     fu.normalize(localize("/1/2/3/..\\../5/..\\./2/./3/6\\../4")).getPath());

        try {
            fu.normalize("foo");
            fail("foo is not an absolute path");
        } catch (BuildException e) {
            // Expected exception caught
        }
        
        try {
            fu.normalize(localize("/1/../../b"));
            fail("successfully crawled beyond the filesystem root");
        } catch (BuildException e) {
            // Expected exception caught
        }
    }

    /**
     * adapt file separators to local conventions
     */
    private String localize(String path) {
        path = root + path.substring(1);
        return path.replace('\\', File.separatorChar).replace('/', File.separatorChar);
    }
}
