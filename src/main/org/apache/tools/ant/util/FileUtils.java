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
import java.lang.reflect.Method;

import org.apache.tools.ant.BuildException; 
import org.apache.tools.ant.Project; 
import org.apache.tools.ant.types.FilterSet; 

/**
 * Central representation of an Ant project. This class defines a
 * Ant project with all of it's targets and tasks. It also provides
 * the mechanism to kick off a build using a particular target name.
 * <p>
 * This class also encapsulates methods which allow Files to be refered
 * to using abstract path names which are translated to native system
 * file paths at runtime as well as defining various project properties.
 *
 * @author duncan@x180.com
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 */
 
public class FileUtils {
    private static Object lockReflection = new Object();
    private static java.lang.reflect.Method setLastModified = null;

    /**
     * Factory method.
     */
    public static FileUtils newFileUtils() {
        return new FileUtils();
    }

    /**
     * Empty constructor.
     */
    protected FileUtils() {}

    /**
     * Convienence method to copy a file from a source to a destination.
     * No filtering is performed.
     *
     * @throws IOException
     */
    public void copyFile(String sourceFile, String destFile) throws IOException {
        copyFile(new File(sourceFile), new File(destFile), null, false, false);
    }

    /**
     * Convienence method to copy a file from a source to a destination
     * specifying if token filtering must be used.
     *
     * @throws IOException
     */
    public void copyFile(String sourceFile, String destFile, FilterSet filterSet)
        throws IOException
    {
        copyFile(new File(sourceFile), new File(destFile), filterSet, false, false);
    }

    /**
     * Convienence method to copy a file from a source to a
     * destination specifying if token filtering must be used and if
     * source files may overwrite newer destination files.
     *
     * @throws IOException 
     */
    public void copyFile(String sourceFile, String destFile, FilterSet filterSet,
                         boolean overwrite) throws IOException {
        copyFile(new File(sourceFile), new File(destFile), filterSet, 
                 overwrite, false);
    }

     /**
     * Convienence method to copy a file from a source to a
     * destination specifying if token filtering must be used, if
     * source files may overwrite newer destination files and the
     * last modified time of <code>destFile</code> file should be made equal
     * to the last modified time of <code>sourceFile</code>.
     *
     * @throws IOException 
     */
    public void copyFile(String sourceFile, String destFile, FilterSet filterSet,
                         boolean overwrite, boolean preserveLastModified)
        throws IOException {
        copyFile(new File(sourceFile), new File(destFile), filterSet, 
                 overwrite, preserveLastModified);
    }

    /**
     * Convienence method to copy a file from a source to a destination.
     * No filtering is performed.
     *
     * @throws IOException
     */
    public void copyFile(File sourceFile, File destFile) throws IOException {
        copyFile(sourceFile, destFile, null, false, false);
    }

    /**
     * Convienence method to copy a file from a source to a destination
     * specifying if token filtering must be used.
     *
     * @throws IOException
     */
    public void copyFile(File sourceFile, File destFile, FilterSet filterSet)
        throws IOException {
        copyFile(sourceFile, destFile, filterSet, false, false);
    }

    /**
     * Convienence method to copy a file from a source to a
     * destination specifying if token filtering must be used and if
     * source files may overwrite newer destination files.
     *
     * @throws IOException 
     */
    public void copyFile(File sourceFile, File destFile, FilterSet filterSet,
                         boolean overwrite) throws IOException {
        copyFile(sourceFile, destFile, filterSet, overwrite, false);
    }

    /**
     * Convienence method to copy a file from a source to a
     * destination specifying if token filtering must be used, if
     * source files may overwrite newer destination files and the
     * last modified time of <code>destFile</code> file should be made equal
     * to the last modified time of <code>sourceFile</code>.
     *
     * @throws IOException 
     */
    public void copyFile(File sourceFile, File destFile, FilterSet filterSet,
                         boolean overwrite, boolean preserveLastModified)
        throws IOException {
        
        if (overwrite || !destFile.exists() ||
            destFile.lastModified() < sourceFile.lastModified()) {

            if (destFile.exists() && destFile.isFile()) {
                destFile.delete();
            }

            // ensure that parent dir of dest file exists!
            // not using getParentFile method to stay 1.1 compat
            File parent = new File(destFile.getParent());
            if (!parent.exists()) {
                parent.mkdirs();
            }

            if (filterSet != null && filterSet.hasFilters()) {
                BufferedReader in = new BufferedReader(new FileReader(sourceFile));
                BufferedWriter out = new BufferedWriter(new FileWriter(destFile));

                int length;
                String newline = null;
                String line = in.readLine();
                while (line != null) {
                    if (line.length() == 0) {
                        out.newLine();
                    } else {
                        newline = filterSet.replaceTokens(line);
                        out.write(newline);
                        out.newLine();
                    }
                    line = in.readLine();
                }

                out.close();
                in.close();
            } else {
                FileInputStream in = new FileInputStream(sourceFile);
                FileOutputStream out = new FileOutputStream(destFile);

                byte[] buffer = new byte[8 * 1024];
                int count = 0;
                do {
                    out.write(buffer, 0, count);
                    count = in.read(buffer, 0, buffer.length);
                } while (count != -1);

                in.close();
                out.close();
            }

            if (preserveLastModified) {
                setFileLastModified(destFile, sourceFile.lastModified());
            }
        }
    }

    /**
     * see whether we have a setLastModified method in File and return it.
     */
    protected final Method getSetLastModified() {
        if (Project.getJavaVersion() == Project.JAVA_1_1) {
            return null;
        }
        if (setLastModified == null) {
            synchronized (lockReflection) {
                if (setLastModified == null) {
                    try {
                        setLastModified = 
                            java.io.File.class.getMethod("setLastModified", 
                                                         new Class[] {Long.TYPE});
                    } catch (NoSuchMethodException nse) {
                        throw new BuildException("File.setlastModified not in JDK > 1.1?",
                                                 nse);
                    }
                }
            }
        }
        return setLastModified;
    }

    /**
     * Calls File.setLastModified(long time) in a Java 1.1 compatible way.
     */
    public void setFileLastModified(File file, long time) throws BuildException {
        if (Project.getJavaVersion() == Project.JAVA_1_1) {
            return;
        }
        Long[] times = new Long[1];
        if (time < 0) {
            times[0] = new Long(System.currentTimeMillis());
        } else {
            times[0] = new Long(time);
        }

        try {
            getSetLastModified().invoke(file, times);
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable nested = ite.getTargetException();
            throw new BuildException("Exception setting the modification time "
                                     + "of " + file, nested);
        } catch (Throwable other) {
            throw new BuildException("Exception setting the modification time "
                                     + "of " + file, other);
        }
    }

}

