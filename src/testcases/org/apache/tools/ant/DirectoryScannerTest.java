/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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
import org.apache.tools.ant.util.JavaEnvUtils;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;
import java.io.File;
import java.io.IOException;

/**
 * JUnit 3 testcases for org.apache.tools.ant.DirectoryScanner
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 */
public class DirectoryScannerTest extends TestCase {

    public DirectoryScannerTest(String name) {super(name);}


    // keep track of what operating systems are supported here.
    private boolean supportsSymlinks = Os.isFamily("unix") 
        && !JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1);

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
                    } else if (included[i].equals("ThisIsALink"+File.separator+
                                                  "taskdefs")) {
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
                    } else if (included[i].equals("ThisIsALink"+File.separator+
                                                  "taskdefs")) {
                        haveTaskdefsPackage = true;
                    }
                }
                assertTrue("(2) zip package included", haveZipPackage);
                assertTrue("(2) taskdefs package not included", 
                           !haveTaskdefsPackage);
                
            } finally {
                File f = new File("src/main/org/apache/tools/ThisIsALink");
                if (!f.delete()) {
                    throw new RuntimeException("Failed to delete "+f);
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

}
