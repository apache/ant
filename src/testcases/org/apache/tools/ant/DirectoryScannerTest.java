/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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

package org.apache.tools.ant;

import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.util.JavaEnvUtils;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;
import java.io.File;
import java.io.IOException;
import java.util.TreeSet;
import java.util.Iterator;

/**
 * JUnit 3 testcases for org.apache.tools.ant.DirectoryScanner
 *
 * @author Stefan Bodewig
 */
public class DirectoryScannerTest extends BuildFileTest {

    public DirectoryScannerTest(String name) {super(name);}

    // keep track of what operating systems are supported here.
    private boolean supportsSymlinks = Os.isFamily("unix")
        && !JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1);

    public void setUp() {
        configureProject("src/etc/testcases/core/directoryscanner.xml");
        getProject().executeTarget("setup");
    }

    public void tearDown() {
        getProject().executeTarget("cleanup");
    }

    public void test1() {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha"});
        ds.scan();
        compareFiles(ds, new String[] {} ,new String[] {"alpha"});
    }

    public void test2() {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/"});
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/beta.xml", 
                                       "alpha/beta/gamma/gamma.xml"},
                     new String[] {"alpha", "alpha/beta", "alpha/beta/gamma"});
    }

    public void test3() {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/beta.xml", 
                                       "alpha/beta/gamma/gamma.xml"},
                     new String[] {"", "alpha", "alpha/beta", 
                                   "alpha/beta/gamma"});
    }

    public void testFullPathMatchesCaseSensitive() {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/beta/gamma/GAMMA.XML"});
        ds.scan();
        compareFiles(ds, new String[] {}, new String[] {});
    }

    public void testFullPathMatchesCaseInsensitive() {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setCaseSensitive(false);
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/beta/gamma/GAMMA.XML"});
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/gamma/gamma.xml"},
                     new String[] {});
    }

    public void test2ButCaseInsesitive() {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"ALPHA/"});
        ds.setCaseSensitive(false);
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/beta.xml", 
                                       "alpha/beta/gamma/gamma.xml"},
                     new String[] {"alpha", "alpha/beta", "alpha/beta/gamma"});
    }

    public void testAllowSymlinks() {
        if (!supportsSymlinks) {
            return;
        }
        
        getProject().executeTarget("symlink-setup");
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/beta/gamma/"});
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/gamma/gamma.xml"},
                     new String[] {"alpha/beta/gamma"});
    }

    public void testProhibitSymlinks() {
        if (!supportsSymlinks) {
            return;
        }
        
        getProject().executeTarget("symlink-setup");
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/beta/gamma/"});
        ds.setFollowSymlinks(false);
        ds.scan();
        compareFiles(ds, new String[] {}, new String[] {});
    }

    // father and child pattern test
    public void testOrderOfIncludePatternsIrrelevant() {
        String [] expectedFiles = {"alpha/beta/beta.xml", 
                                   "alpha/beta/gamma/gamma.xml"};
        String [] expectedDirectories = {"alpha/beta", "alpha/beta/gamma" };
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/be?a/**", "alpha/beta/gamma/"});
        ds.scan();
        compareFiles(ds, expectedFiles, expectedDirectories);
        // redo the test, but the 2 include patterns are inverted
        ds = new DirectoryScanner();
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/beta/gamma/", "alpha/be?a/**"});
        ds.scan();
        compareFiles(ds, expectedFiles, expectedDirectories);
    }

    /**
     * Test case for setFollowLinks() and associated funtionality.
     * Only supports test on linux, at the moment because Java has
     * no real notion of symlinks built in, so an os-specfic call
     * to Runtime.exec() must be made to create a link to test against.
     */

    public void testSetFollowLinks() {
        if (supportsSymlinks) {
            try {
                // add conditions and more commands as soon as the need arises
                String[] command = new String[] {
                    "ln", "-s", "ant", "src/main/org/apache/tools/ThisIsALink"
                };
                try {
                    Runtime.getRuntime().exec(command);
                    // give ourselves some time for the system call
                    // to execute... tweak if you have a really over
                    // loaded system.
                    Thread.sleep(1000);
                } catch (IOException ioe) {
                    fail("IOException making link "+ioe);
                } catch (InterruptedException ie) {
                }

                File dir = new File("src/main/org/apache/tools");
                DirectoryScanner ds = new DirectoryScanner();

                // followLinks should be true by default, but if this ever
                // changes we will need this line.
                ds.setFollowSymlinks(true);

                ds.setBasedir(dir);
                ds.setExcludes(new String[] {"ant/**"});
                ds.scan();

                boolean haveZipPackage = false;
                boolean haveTaskdefsPackage = false;

                String[] included = ds.getIncludedDirectories();
                for (int i=0; i<included.length; i++) {
                    if (included[i].equals("zip")) {
                        haveZipPackage = true;
                    } else if (included[i].equals("ThisIsALink"
                                                  + File.separator
                                                  + "taskdefs")) {
                        haveTaskdefsPackage = true;
                    }
                }

                // if we followed the symlink we just made we should
                // bypass the excludes.

                assertTrue("(1) zip package included", haveZipPackage);
                assertTrue("(1) taskdefs package included",
                           haveTaskdefsPackage);


                ds = new DirectoryScanner();
                ds.setFollowSymlinks(false);

                ds.setBasedir(dir);
                ds.setExcludes(new String[] {"ant/**"});
                ds.scan();

                haveZipPackage = false;
                haveTaskdefsPackage = false;
                included = ds.getIncludedDirectories();
                for (int i=0; i<included.length; i++) {
                    if (included[i].equals("zip")) {
                        haveZipPackage = true;
                    } else if (included[i].equals("ThisIsALink" 
                                                  + File.separator
                                                  + "taskdefs")) {
                        haveTaskdefsPackage = true;
                    }
                }
                assertTrue("(2) zip package included", haveZipPackage);
                assertTrue("(2) taskdefs package not included",
                           !haveTaskdefsPackage);

            } finally {
                File f = new File("src/main/org/apache/tools/ThisIsALink");
                if (!f.delete()) {
                    throw new RuntimeException("Failed to delete " + f);
                }
            }
        }
    }

    /**
     * Test inspired by Bug#1415.
     */
    public void testChildrenOfExcludedDirectory() {
        File dir = new File("src/main/org/apache/tools");
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(dir);
        ds.setExcludes(new String[] {"ant/**"});
        ds.scan();

        boolean haveZipPackage = false;
        boolean haveTaskdefsPackage = false;
        String[] included = ds.getIncludedDirectories();
        for (int i=0; i<included.length; i++) {
            if (included[i].equals("zip")) {
                haveZipPackage = true;
            } else if (included[i].equals("ant"+File.separator+"taskdefs")) {
                haveTaskdefsPackage = true;
            }
        }
        assertTrue("(1) zip package included", haveZipPackage);
        assertTrue("(1) taskdefs package not included", !haveTaskdefsPackage);

        ds = new DirectoryScanner();
        ds.setBasedir(dir);
        ds.setExcludes(new String[] {"ant"});
        ds.scan();
        haveZipPackage = false;
        included = ds.getIncludedDirectories();
        for (int i=0; i<included.length; i++) {
            if (included[i].equals("zip")) {
                haveZipPackage = true;
            } else if (included[i].equals("ant"+File.separator+"taskdefs")) {
                haveTaskdefsPackage = true;
            }
        }
        assertTrue("(2) zip package included", haveZipPackage);
        assertTrue("(2) taskdefs package included", haveTaskdefsPackage);
    }

    private void compareFiles(DirectoryScanner ds, String[] expectedFiles, 
                              String[] expectedDirectories) {
        String includedFiles[] = ds.getIncludedFiles();
        String includedDirectories[] = ds.getIncludedDirectories();
        assertEquals("file present: ", expectedFiles.length,  
                     includedFiles.length);
        assertEquals("directories present: ", expectedDirectories.length,  
                     includedDirectories.length);

        TreeSet files = new TreeSet();
        for (int counter=0; counter < includedFiles.length; counter++) {
            files.add(includedFiles[counter].replace(File.separatorChar, '/'));
        }
        TreeSet directories = new TreeSet();
        for (int counter=0; counter < includedDirectories.length; counter++) {
            directories.add(includedDirectories[counter]
                            .replace(File.separatorChar, '/'));
        }

        String currentfile;
        Iterator i = files.iterator();
        int counter = 0;
        while (i.hasNext()) {
            currentfile = (String) i.next();
            assertEquals(expectedFiles[counter], currentfile);
            counter++;
        }
        String currentdirectory;
        Iterator dirit = directories.iterator();
        counter = 0;
        while (dirit.hasNext()) {
            currentdirectory = (String) dirit.next();
            assertEquals(expectedDirectories[counter], currentdirectory);
            counter++;
        }
    }

}
