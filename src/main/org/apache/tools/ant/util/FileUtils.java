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

package org.apache.tools.ant.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.CharacterIterator;
import java.text.DecimalFormat;
import java.text.StringCharacterIterator;
import java.util.Random;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.filters.util.ChainReaderHelper;
import org.apache.tools.ant.filters.TokenFilter;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.FilterSetCollection;
import org.apache.tools.ant.launch.Locator;

/**
 * This class also encapsulates methods which allow Files to be
 * refered to using abstract path names which are translated to native
 * system file paths at runtime as well as copying files or setting
 * there last modification time.
 *
 * @author duncan@x180.com
 * @author Conor MacNeill
 * @author Stefan Bodewig
 * @author Magesh Umasankar
 * @author <a href="mailto:jtulley@novell.com">Jeff Tulley</a>
 *
 * @version $Revision$
 */

public class FileUtils {
    private static Random rand = new Random(System.currentTimeMillis());
    private static Object lockReflection = new Object();
    private static java.lang.reflect.Method setLastModified = null;

    private boolean onNetWare = Os.isFamily("netware");

    // for toURI
    private static boolean[] isSpecial = new boolean[256];
    private static char[] escapedChar1 = new char[256];
    private static char[] escapedChar2 = new char[256];


    // stolen from FilePathToURI of the Xerces-J team
    static {
        for (int i = 0; i <= 0x20; i++) {
            isSpecial[i] = true;
            escapedChar1[i] = Character.forDigit(i >> 4, 16);
            escapedChar2[i] = Character.forDigit(i & 0xf, 16);
        }
        isSpecial[0x7f] = true;
        escapedChar1[0x7f] = '7';
        escapedChar2[0x7f] = 'F';
        char[] escChs = {'<', '>', '#', '%', '"', '{', '}',
                         '|', '\\', '^', '~', '[', ']', '`'};
        int len = escChs.length;
        char ch;
        for (int i = 0; i < len; i++) {
            ch = escChs[i];
            isSpecial[ch] = true;
            escapedChar1[ch] = Character.forDigit(ch >> 4, 16);
            escapedChar2[ch] = Character.forDigit(ch & 0xf, 16);
        }
    }

    /**
     * Factory method.
     *
     * @return a new instance of FileUtils.
     */
    public static FileUtils newFileUtils() {
        return new FileUtils();
    }

    /**
     * Empty constructor.
     */
    protected FileUtils() {
    }

    /**
     * Get the URL for a file taking into account # characters
     *
     * @param file the file whose URL representation is required.
     * @return The FileURL value
     * @throws MalformedURLException if the URL representation cannot be
     *      formed.
     */
    public URL getFileURL(File file) throws MalformedURLException {
        return new URL(toURI(file.getAbsolutePath()));
    }

    /**
     * Convienence method to copy a file from a source to a destination.
     * No filtering is performed.
     *
     * @param sourceFile Name of file to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile Name of file to copy to.
     *                 Must not be <code>null</code>.
     *
     * @throws IOException if the copying fails
     */
    public void copyFile(String sourceFile, String destFile)
        throws IOException {
        copyFile(new File(sourceFile), new File(destFile), null, false, false);
    }

    /**
     * Convienence method to copy a file from a source to a destination
     * specifying if token filtering must be used.
     *
     * @param sourceFile Name of file to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile Name of file to copy to.
     *                 Must not be <code>null</code>.
     * @param filters the collection of filters to apply to this copy
     *
     * @throws IOException if the copying fails
     */
    public void copyFile(String sourceFile, String destFile,
                         FilterSetCollection filters)
        throws IOException {
        copyFile(new File(sourceFile), new File(destFile), filters,
                 false, false);
    }

    /**
     * Convienence method to copy a file from a source to a
     * destination specifying if token filtering must be used and if
     * source files may overwrite newer destination files.
     *
     * @param sourceFile Name of file to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile Name of file to copy to.
     *                 Must not be <code>null</code>.
     * @param filters the collection of filters to apply to this copy
     * @param overwrite Whether or not the destination file should be
     *                  overwritten if it already exists.
     *
     * @throws IOException if the copying fails
     */
    public void copyFile(String sourceFile, String destFile, FilterSetCollection filters,
                         boolean overwrite) throws IOException {
        copyFile(new File(sourceFile), new File(destFile), filters,
                 overwrite, false);
    }

    /**
     * Convienence method to copy a file from a source to a
     * destination specifying if token filtering must be used, if
     * source files may overwrite newer destination files and the
     * last modified time of <code>destFile</code> file should be made equal
     * to the last modified time of <code>sourceFile</code>.
     *
     * @param sourceFile Name of file to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile Name of file to copy to.
     *                 Must not be <code>null</code>.
     * @param filters the collection of filters to apply to this copy
     * @param overwrite Whether or not the destination file should be
     *                  overwritten if it already exists.
     * @param preserveLastModified Whether or not the last modified time of
     *                             the resulting file should be set to that
     *                             of the source file.
     *
     * @throws IOException if the copying fails
     */
    public void copyFile(String sourceFile, String destFile, FilterSetCollection filters,
                         boolean overwrite, boolean preserveLastModified)
        throws IOException {
        copyFile(new File(sourceFile), new File(destFile), filters,
                 overwrite, preserveLastModified);
    }

    /**
     * Convienence method to copy a file from a source to a
     * destination specifying if token filtering must be used, if
     * source files may overwrite newer destination files and the
     * last modified time of <code>destFile</code> file should be made equal
     * to the last modified time of <code>sourceFile</code>.
     *
     * @param sourceFile Name of file to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile Name of file to copy to.
     *                 Must not be <code>null</code>.
     * @param filters the collection of filters to apply to this copy
     * @param overwrite Whether or not the destination file should be
     *                  overwritten if it already exists.
     * @param preserveLastModified Whether or not the last modified time of
     *                             the resulting file should be set to that
     *                             of the source file.
     * @param encoding the encoding used to read and write the files.
     *
     * @throws IOException if the copying fails
     *
     * @since Ant 1.5
     */
    public void copyFile(String sourceFile, String destFile,
                         FilterSetCollection filters, boolean overwrite,
                         boolean preserveLastModified, String encoding)
        throws IOException {
        copyFile(new File(sourceFile), new File(destFile), filters,
                 overwrite, preserveLastModified, encoding);
    }

    /**
     * Convienence method to copy a file from a source to a
     * destination specifying if token filtering must be used, if
     * filter chains must be used, if source files may overwrite
     * newer destination files and the last modified time of
     * <code>destFile</code> file should be made equal
     * to the last modified time of <code>sourceFile</code>.
     *
     * @param sourceFile Name of file to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile Name of file to copy to.
     *                 Must not be <code>null</code>.
     * @param filters the collection of filters to apply to this copy
     * @param filterChains filterChains to apply during the copy.
     * @param overwrite Whether or not the destination file should be
     *                  overwritten if it already exists.
     * @param preserveLastModified Whether or not the last modified time of
     *                             the resulting file should be set to that
     *                             of the source file.
     * @param encoding the encoding used to read and write the files.
     * @param project the project instance
     *
     * @throws IOException if the copying fails
     *
     * @since Ant 1.5
     */
    public void copyFile(String sourceFile, String destFile,
                         FilterSetCollection filters, Vector filterChains,
                         boolean overwrite, boolean preserveLastModified,
                         String encoding, Project project)
        throws IOException {
        copyFile(new File(sourceFile), new File(destFile), filters,
                 filterChains, overwrite, preserveLastModified,
                 encoding, project);
    }

    /**
     * Convienence method to copy a file from a source to a
     * destination specifying if token filtering must be used, if
     * filter chains must be used, if source files may overwrite
     * newer destination files and the last modified time of
     * <code>destFile</code> file should be made equal
     * to the last modified time of <code>sourceFile</code>.
     *
     * @param sourceFile Name of file to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile Name of file to copy to.
     *                 Must not be <code>null</code>.
     * @param filters the collection of filters to apply to this copy
     * @param filterChains filterChains to apply during the copy.
     * @param overwrite Whether or not the destination file should be
     *                  overwritten if it already exists.
     * @param preserveLastModified Whether or not the last modified time of
     *                             the resulting file should be set to that
     *                             of the source file.
     * @param inputEncoding the encoding used to read the files.
     * @param outputEncoding the encoding used to write the files.
     * @param project the project instance
     *
     * @throws IOException if the copying fails
     *
     * @since Ant 1.6
     */
    public void copyFile(String sourceFile, String destFile,
                         FilterSetCollection filters, Vector filterChains,
                         boolean overwrite, boolean preserveLastModified,
                         String inputEncoding, String outputEncoding,
                         Project project)
        throws IOException {
        copyFile(new File(sourceFile), new File(destFile), filters,
                 filterChains, overwrite, preserveLastModified,
                 inputEncoding, outputEncoding, project);
    }

    /**
     * Convienence method to copy a file from a source to a destination.
     * No filtering is performed.
     *
     * @param sourceFile the file to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile the file to copy to.
     *                 Must not be <code>null</code>.
     *
     * @throws IOException if the copying fails
     */
    public void copyFile(File sourceFile, File destFile) throws IOException {
        copyFile(sourceFile, destFile, null, false, false);
    }

    /**
     * Convienence method to copy a file from a source to a destination
     * specifying if token filtering must be used.
     *
     * @param sourceFile the file to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile the file to copy to.
     *                 Must not be <code>null</code>.
     * @param filters the collection of filters to apply to this copy
     *
     * @throws IOException if the copying fails
     */
    public void copyFile(File sourceFile, File destFile, FilterSetCollection filters)
        throws IOException {
        copyFile(sourceFile, destFile, filters, false, false);
    }

    /**
     * Convienence method to copy a file from a source to a
     * destination specifying if token filtering must be used and if
     * source files may overwrite newer destination files.
     *
     * @param sourceFile the file to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile the file to copy to.
     *                 Must not be <code>null</code>.
     * @param filters the collection of filters to apply to this copy
     * @param overwrite Whether or not the destination file should be
     *                  overwritten if it already exists.
     *
     * @throws IOException if the copying fails
     */
    public void copyFile(File sourceFile, File destFile, FilterSetCollection filters,
                         boolean overwrite) throws IOException {
        copyFile(sourceFile, destFile, filters, overwrite, false);
    }

    /**
     * Convienence method to copy a file from a source to a
     * destination specifying if token filtering must be used, if
     * source files may overwrite newer destination files and the
     * last modified time of <code>destFile</code> file should be made equal
     * to the last modified time of <code>sourceFile</code>.
     *
     * @param sourceFile the file to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile the file to copy to.
     *                 Must not be <code>null</code>.
     * @param filters the collection of filters to apply to this copy
     * @param overwrite Whether or not the destination file should be
     *                  overwritten if it already exists.
     * @param preserveLastModified Whether or not the last modified time of
     *                             the resulting file should be set to that
     *                             of the source file.
     *
     * @throws IOException if the copying fails
     */
    public void copyFile(File sourceFile, File destFile, FilterSetCollection filters,
                         boolean overwrite, boolean preserveLastModified)
        throws IOException {
        copyFile(sourceFile, destFile, filters, overwrite,
                 preserveLastModified, null);
    }

    /**
     * Convienence method to copy a file from a source to a
     * destination specifying if token filtering must be used, if
     * source files may overwrite newer destination files, the last
     * modified time of <code>destFile</code> file should be made
     * equal to the last modified time of <code>sourceFile</code> and
     * which character encoding to assume.
     *
     * @param sourceFile the file to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile the file to copy to.
     *                 Must not be <code>null</code>.
     * @param filters the collection of filters to apply to this copy
     * @param overwrite Whether or not the destination file should be
     *                  overwritten if it already exists.
     * @param preserveLastModified Whether or not the last modified time of
     *                             the resulting file should be set to that
     *                             of the source file.
     * @param encoding the encoding used to read and write the files.
     *
     * @throws IOException if the copying fails
     *
     * @since Ant 1.5
     */
    public void copyFile(File sourceFile, File destFile,
                         FilterSetCollection filters, boolean overwrite,
                         boolean preserveLastModified, String encoding)
        throws IOException {
        copyFile(sourceFile, destFile, filters, null, overwrite,
                 preserveLastModified, encoding, null);
    }

    /**
     * Convienence method to copy a file from a source to a
     * destination specifying if token filtering must be used, if
     * filter chains must be used, if source files may overwrite
     * newer destination files and the last modified time of
     * <code>destFile</code> file should be made equal
     * to the last modified time of <code>sourceFile</code>.
     *
     * @param sourceFile the file to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile the file to copy to.
     *                 Must not be <code>null</code>.
     * @param filters the collection of filters to apply to this copy
     * @param filterChains filterChains to apply during the copy.
     * @param overwrite Whether or not the destination file should be
     *                  overwritten if it already exists.
     * @param preserveLastModified Whether or not the last modified time of
     *                             the resulting file should be set to that
     *                             of the source file.
     * @param encoding the encoding used to read and write the files.
     * @param project the project instance
     *
     * @throws IOException if the copying fails
     *
     * @since Ant 1.5
     */
    public void copyFile(File sourceFile, File destFile,
                         FilterSetCollection filters, Vector filterChains,
                         boolean overwrite, boolean preserveLastModified,
                         String encoding, Project project)
        throws IOException {
        copyFile(sourceFile, destFile, filters, filterChains,
                 overwrite, preserveLastModified, encoding, encoding, project);
    }

    /**
     * Convienence method to copy a file from a source to a
     * destination specifying if token filtering must be used, if
     * filter chains must be used, if source files may overwrite
     * newer destination files and the last modified time of
     * <code>destFile</code> file should be made equal
     * to the last modified time of <code>sourceFile</code>.
     *
     * @param sourceFile the file to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile the file to copy to.
     *                 Must not be <code>null</code>.
     * @param filters the collection of filters to apply to this copy
     * @param filterChains filterChains to apply during the copy.
     * @param overwrite Whether or not the destination file should be
     *                  overwritten if it already exists.
     * @param preserveLastModified Whether or not the last modified time of
     *                             the resulting file should be set to that
     *                             of the source file.
     * @param inputEncoding the encoding used to read the files.
     * @param outputEncoding the encoding used to write the files.
     * @param project the project instance
     *
     *
     * @throws IOException if the copying fails
     *
     * @since Ant 1.6
     */
    public void copyFile(File sourceFile, File destFile,
                         FilterSetCollection filters, Vector filterChains,
                         boolean overwrite, boolean preserveLastModified,
                         String inputEncoding, String outputEncoding,
                         Project project)
        throws IOException {

        if (overwrite || !destFile.exists()
            || destFile.lastModified() < sourceFile.lastModified()) {

            if (destFile.exists() && destFile.isFile()) {
                destFile.delete();
            }

            // ensure that parent dir of dest file exists!
            // not using getParentFile method to stay 1.1 compat
            File parent = getParentFile(destFile);
            if (!parent.exists()) {
                parent.mkdirs();
            }

            final boolean filterSetsAvailable = (filters != null
                                                 && filters.hasFilters());
            final boolean filterChainsAvailable = (filterChains != null
                                                   && filterChains.size() > 0);

            if (filterSetsAvailable) {
                BufferedReader in = null;
                BufferedWriter out = null;

                try {
                    if (inputEncoding == null) {
                        in = new BufferedReader(new FileReader(sourceFile));
                    } else {
                        InputStreamReader isr
                            = new InputStreamReader(new FileInputStream(sourceFile),
                                                    inputEncoding);
                        in = new BufferedReader(isr);
                    }

                    if (outputEncoding == null) {
                        out = new BufferedWriter(new FileWriter(destFile));
                    } else {
                        OutputStreamWriter osw
                            = new OutputStreamWriter(new FileOutputStream(destFile),
                                                     outputEncoding);
                        out = new BufferedWriter(osw);
                    }

                    if (filterChainsAvailable) {
                        ChainReaderHelper crh = new ChainReaderHelper();
                        crh.setBufferSize(8192);
                        crh.setPrimaryReader(in);
                        crh.setFilterChains(filterChains);
                        crh.setProject(project);
                        Reader rdr = crh.getAssembledReader();
                        in = new BufferedReader(rdr);
                    }

                    TokenFilter.LineTokenizer lineTokenizer = new TokenFilter.LineTokenizer();
                    lineTokenizer.setIncludeDelims(true);
                    String newline = null;
                    String line = lineTokenizer.getToken(in);
                    while (line != null) {
                        if (line.length() == 0) {
                            // this should not happen, because the lines are
                            // returned with the end of line delimiter
                            out.newLine();
                        } else {
                            newline = filters.replaceTokens(line);
                            out.write(newline);
                        }
                        line = lineTokenizer.getToken(in);
                    }
                } finally {
                    if (out != null) {
                        out.close();
                    }
                    if (in != null) {
                        in.close();
                    }
                }
            } else if (filterChainsAvailable
                       || (inputEncoding != null
                           && !inputEncoding.equals(outputEncoding))
                       || (inputEncoding == null && outputEncoding != null)) {
                BufferedReader in = null;
                BufferedWriter out = null;

                 try {
                     if (inputEncoding == null) {
                         in = new BufferedReader(new FileReader(sourceFile));
                     } else {
                         in =
                             new BufferedReader(
                                 new InputStreamReader(
                                     new FileInputStream(sourceFile),
                                     inputEncoding));
                     }

                     if (outputEncoding == null) {
                         out = new BufferedWriter(new FileWriter(destFile));
                     } else {
                         out =
                             new BufferedWriter(
                                 new OutputStreamWriter(
                                     new FileOutputStream(destFile),
                                     outputEncoding));
                     }

                     if (filterChainsAvailable) {
                         ChainReaderHelper crh = new ChainReaderHelper();
                         crh.setBufferSize(8192);
                         crh.setPrimaryReader(in);
                         crh.setFilterChains(filterChains);
                         crh.setProject(project);
                         Reader rdr = crh.getAssembledReader();
                         in = new BufferedReader(rdr);
                     }
                     char[] buffer = new char[1024 * 8];
                     while (true) {
                         int nRead = in.read(buffer, 0, buffer.length);
                         if (nRead == -1) {
                             break;
                         }
                         out.write(buffer, 0, nRead);
                      }
                  } finally {
                      if (out != null) {
                         out.close();
                     }
                     if (in != null) {
                         in.close();
                     }
                 }
            } else {
                FileInputStream in = null;
                FileOutputStream out = null;
                try {
                    in = new FileInputStream(sourceFile);
                    out = new FileOutputStream(destFile);

                    byte[] buffer = new byte[8 * 1024];
                    int count = 0;
                    do {
                        out.write(buffer, 0, count);
                        count = in.read(buffer, 0, buffer.length);
                    } while (count != -1);
                } finally {
                    if (out != null) {
                        out.close();
                    }
                    if (in != null) {
                        in.close();
                    }
                }
            }

            if (preserveLastModified) {
                setFileLastModified(destFile, sourceFile.lastModified());
            }
        }
    }

    /**
     * see whether we have a setLastModified method in File and return it.
     *
     * @return a method to setLastModified.
     */
    protected final Method getSetLastModified() {
        if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)) {
            return null;
        }
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
        return setLastModified;
    }

    /**
     * Calls File.setLastModified(long time) in a Java 1.1 compatible way.
     *
     * @param file the file whose modified time is to be set
     * @param time the time to which the last modified time is to be set.
     *
     * @throws BuildException if the time cannot be set.
     */
    public void setFileLastModified(File file, long time)
        throws BuildException {
        if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)) {
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

    /**
     * Interpret the filename as a file relative to the given file -
     * unless the filename already represents an absolute filename.
     *
     * @param file the "reference" file for relative paths. This
     * instance must be an absolute file and must not contain
     * &quot;./&quot; or &quot;../&quot; sequences (same for \ instead
     * of /).  If it is null, this call is equivalent to
     * <code>new java.io.File(filename)</code>.
     *
     * @param filename a file name
     *
     * @return an absolute file that doesn't contain &quot;./&quot; or
     * &quot;../&quot; sequences and uses the correct separator for
     * the current platform.
     */
    public File resolveFile(File file, String filename) {
        filename = filename.replace('/', File.separatorChar)
            .replace('\\', File.separatorChar);

        // deal with absolute files
        if (!onNetWare) {
            if (filename.startsWith(File.separator)
                || (filename.length() >= 2
                    && Character.isLetter(filename.charAt(0))
                    && filename.charAt(1) == ':')) {
                return normalize(filename);
            }
        } else {
            // the assumption that the : will appear as the second character in
            // the path name breaks down when NetWare is a supported platform.
            // Netware volumes are of the pattern: "data:\"
            int colon = filename.indexOf(":");
            if (filename.startsWith(File.separator)
                || (colon > -1)) {
                return normalize(filename);
            }
        }

        if (file == null) {
            return new File(filename);
        }

        File helpFile = new File(file.getAbsolutePath());
        StringTokenizer tok = new StringTokenizer(filename, File.separator);
        while (tok.hasMoreTokens()) {
            String part = tok.nextToken();
            if (part.equals("..")) {
                helpFile = getParentFile(helpFile);
                if (helpFile == null) {
                    String msg = "The file or path you specified ("
                        + filename + ") is invalid relative to "
                        + file.getPath();
                    throw new BuildException(msg);
                }
            } else if (part.equals(".")) {
                // Do nothing here
            } else {
                helpFile = new File(helpFile, part);
            }
        }

        return new File(helpFile.getAbsolutePath());
    }

    /**
     * &quot;normalize&quot; the given absolute path.
     *
     * <p>This includes:
     * <ul>
     *   <li>Uppercase the drive letter if there is one.</li>
     *   <li>Remove redundant slashes after the drive spec.</li>
     *   <li>resolve all ./, .\, ../ and ..\ sequences.</li>
     *   <li>DOS style paths that start with a drive letter will have
     *     \ as the separator.</li>
     * </ul>
     *
     * @param path the path to be normalized
     * @param the normalized version of the path.
     *
     * @throws java.lang.NullPointerException if the file path is
     * equal to null.
     */
    public File normalize(String path) {
        String orig = path;

        path = path.replace('/', File.separatorChar)
            .replace('\\', File.separatorChar);

        // make sure we are dealing with an absolute path
        int colon = path.indexOf(":");

        if (!onNetWare) {
            if (!path.startsWith(File.separator)
                && !(path.length() >= 2
                    && Character.isLetter(path.charAt(0))
                    && colon == 1)) {
                String msg = path + " is not an absolute path";
                throw new BuildException(msg);
            }
        } else {
            if (!path.startsWith(File.separator)
                && (colon == -1)) {
                String msg = path + " is not an absolute path";
                throw new BuildException(msg);
            }
        }

        boolean dosWithDrive = false;
        String root = null;
        // Eliminate consecutive slashes after the drive spec
        if ((!onNetWare && path.length() >= 2
                && Character.isLetter(path.charAt(0))
                && path.charAt(1) == ':')
            || (onNetWare && colon > -1)) {

            dosWithDrive = true;

            char[] ca = path.replace('/', '\\').toCharArray();
            StringBuffer sbRoot = new StringBuffer();
            for (int i = 0; i < colon; i++) {
                sbRoot.append(Character.toUpperCase(ca[i]));
            }
            sbRoot.append(':');
            if (colon + 1 < path.length()) {
                sbRoot.append(File.separatorChar);
            }
            root = sbRoot.toString();

            // Eliminate consecutive slashes after the drive spec
            StringBuffer sbPath = new StringBuffer();
            for (int i = colon + 1; i < ca.length; i++) {
                if ((ca[i] != '\\') ||
                    (ca[i] == '\\' && ca[i - 1] != '\\')) {
                    sbPath.append(ca[i]);
                }
            }
            path = sbPath.toString().replace('\\', File.separatorChar);

        } else {
            if (path.length() == 1) {
                root = File.separator;
                path = "";
            } else if (path.charAt(1) == File.separatorChar) {
                // UNC drive
                root = File.separator + File.separator;
                path = path.substring(2);
            } else {
                root = File.separator;
                path = path.substring(1);
            }
        }

        Stack s = new Stack();
        s.push(root);
        StringTokenizer tok = new StringTokenizer(path, File.separator);
        while (tok.hasMoreTokens()) {
            String thisToken = tok.nextToken();
            if (".".equals(thisToken)) {
                continue;
            } else if ("..".equals(thisToken)) {
                if (s.size() < 2) {
                    throw new BuildException("Cannot resolve path " + orig);
                } else {
                    s.pop();
                }
            } else { // plain component
                s.push(thisToken);
            }
        }

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.size(); i++) {
            if (i > 1) {
                // not before the filesystem root and not after it, since root
                // already contains one
                sb.append(File.separatorChar);
            }
            sb.append(s.elementAt(i));
        }


        path = sb.toString();
        if (dosWithDrive) {
            path = path.replace('/', '\\');
        }
        return new File(path);
    }

    /**
     * Create a temporary file in a given directory.
     *
     * <p>The file denoted by the returned abstract pathname did not
     * exist before this method was invoked, any subsequent invocation
     * of this method will yield a different file name.</p>
     *
     * <p>This method is different to File.createTempFile of JDK 1.2
     * as it doesn't create the file itself.
     * It uses the location pointed to by java.io.tmpdir
     * when the parentDir attribute is
     * null.</p>
     *
     * @param parentDir Directory to create the temporary file in -
     * current working directory will be assumed if this parameter is
     * null.
     *
     * @return a File reference to the new temporary file.
     * @since ant 1.5
     */
    public File createTempFile(String prefix, String suffix, File parentDir) {

        File result = null;
        String parent = System.getProperty("java.io.tmpdir");
        if (parentDir != null) {
            parent = parentDir.getPath();
        }
        DecimalFormat fmt = new DecimalFormat("#####");
        synchronized (rand) {
            do {
                result = new File(parent,
                                  prefix + fmt.format(Math.abs(rand.nextInt()))
                                  + suffix);
            } while (result.exists());
        }
        return result;
    }

    /**
     * Compares the contents of two files.
     *
     * <p>simple but sub-optimal comparision algorithm.  written for
     * working rather than fast. Better would be a block read into
     * buffers followed by long comparisions apart from the final 1-7
     * bytes.</p>
     *
     * @param f1 the file whose content is to be compared.
     * @param f2 the other file whose content is to be compared.
     *
     * @return true if the content of the files is the same.
     *
     * @throws IOException if the files cannot be read.
     *
     * @since 1.9
     */
    public boolean contentEquals(File f1, File f2) throws IOException {
        if (f1.exists() != f2.exists()) {
            return false;
        }

        if (!f1.exists()) {
            // two not existing files are equal
            return true;
        }

        if (f1.isDirectory() || f2.isDirectory()) {
            // don't want to compare directory contents for now
            return false;
        }

        if (fileNameEquals(f1, f2)) {
            // same filename => true
            return true;
        }

        if (f1.length() != f2.length()) {
            // different size =>false
            return false;
        }

        InputStream in1 = null;
        InputStream in2 = null;
        try {
            in1 = new BufferedInputStream(new FileInputStream(f1));
            in2 = new BufferedInputStream(new FileInputStream(f2));

            int expectedByte = in1.read();
            while (expectedByte != -1) {
                if (expectedByte != in2.read()) {
                    return false;
                }
                expectedByte = in1.read();
            }
            if (in2.read() != -1) {
                return false;
            }
            return true;
        } finally {
            if (in1 != null) {
                try {
                    in1.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (in2 != null) {
                try {
                    in2.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Emulation of File.getParentFile for JDK 1.1
     *
     *
     * @param f the file whose parent is required.
     * @return the given file's parent, or null if the file does not have a
     *         parent.
     * @since 1.10
     */
    public File getParentFile(File f) {
        if (f != null) {
            String p = f.getParent();
            if (p != null) {
                return new File(p);
            }
        }
        return null;
    }

    /**
     * Read from reader till EOF
     * @param rdr the reader from which to read.
     * @return the contents read out of the given reader
     *
     * @throws IOException if the contents could not be read out from the
     *         reader.
     */
    public static final String readFully(Reader rdr) throws IOException {
        return readFully(rdr, 8192);
    }

    /**
     * Read from reader till EOF
     *
     * @param rdr the reader from which to read.
     * @param bufferSize the buffer size to use when reading
     *
     * @return the contents read out of the given reader
     *
     * @throws IOException if the contents could not be read out from the
     *         reader.
     */
    public static final String readFully(Reader rdr, int bufferSize)
        throws IOException {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Buffer size must be greater "
                                               + "than 0");
        }
        final char[] buffer = new char[bufferSize];
        int bufferLength = 0;
        String text = null;
        StringBuffer textBuffer = null;
        while (bufferLength != -1) {
            bufferLength = rdr.read(buffer);
            if (bufferLength != -1) {
                if (textBuffer == null) {
                    textBuffer = new StringBuffer(
                                                  new String(buffer, 0, bufferLength));
                } else {
                    textBuffer.append(new String(buffer, 0, bufferLength));
                }
            }
        }
        if (textBuffer != null) {
            text = textBuffer.toString();
        }
        return text;
    }

    /**
     * Emulation of File.createNewFile for JDK 1.1.
     *
     * <p>This method does <strong>not</strong> guarantee that the
     * operation is atomic.</p>
     *
     * @param f the file to be created
     * @return true if the file did not exist already.
     * @since Ant 1.5
     */
    public boolean createNewFile(File f) throws IOException {
        if (f != null) {
            if (f.exists()) {
                return false;
            }

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(f);
                fos.write(new byte[0]);
            } finally {
                if (fos != null) {
                    fos.close();
                }
            }

            return true;
        }
        return false;
    }

    /**
     * Checks whether a given file is a symbolic link.
     *
     * <p>It doesn't really test for symbolic links but whether the
     * canonical and absolute paths of the file are identical - this
     * may lead to false positives on some platforms.</p>
     *
     * @param parent the parent directory of the file to test
     * @param name the name of the file to test.
     *
     * @return true if the file is a symbolic link.
     * @since Ant 1.5
     */
    public boolean isSymbolicLink(File parent, String name)
        throws IOException {
        File resolvedParent = new File(parent.getCanonicalPath());
        File toTest = new File(resolvedParent, name);
        return !toTest.getAbsolutePath().equals(toTest.getCanonicalPath());
    }

    /**
     * Removes a leading path from a second path.
     *
     * @param leading The leading path, must not be null, must be absolute.
     * @param path The path to remove from, must not be null, must be absolute.
     *
     * @return path's normalized absolute if it doesn't start with
     * leading, path's path with leading's path removed otherwise.
     *
     * @since Ant 1.5
     */
    public String removeLeadingPath(File leading, File path) {
        String l = normalize(leading.getAbsolutePath()).getAbsolutePath();
        String p = normalize(path.getAbsolutePath()).getAbsolutePath();
        if (l.equals(p)) {
            return "";
        }

        // if leading's path ends with a slash, it will be stripped by
        // normalize - we always add one so we never think /foo was a
        // parent directory of /foobar
        l += File.separator;
        if (p.startsWith(l)) {
            return p.substring(l.length());
        } else {
            return p;
        }
    }

    /**
     * Constructs a <code>file:</code> URI that represents the
     * external form of the given pathname.
     *
     * <p>Will be an absolute URI if the given path is absolute.</p>
     *
     * <p>This code doesn't handle non-ASCII characters properly.</p>
     *
     * @param path the path in the local file system
     * @return the URI version of the local path.
     * @since Ant 1.6
     */
    public String toURI(String path) {
        boolean isDir = (new File(path)).isDirectory();

        StringBuffer sb = new StringBuffer("file:");

        // catch exception if normalize thinks this is not an absolute path
        try {
            path = normalize(path).getAbsolutePath();
            sb.append("//");
            // add an extra slash for filesystems with drive-specifiers
            if (!path.startsWith("/")) {
                sb.append("/");
            }

        } catch (BuildException e) {
            // relative path
        }

        path = path.replace('\\', '/');

        CharacterIterator iter = new StringCharacterIterator(path);
        for (char c = iter.first(); c != CharacterIterator.DONE;
             c = iter.next()) {
            if (isSpecial[c]) {
                sb.append('%');
                sb.append(escapedChar1[c]);
                sb.append(escapedChar2[c]);
            } else {
                sb.append(c);
            }
        }
        if (isDir && !path.endsWith("/")) {
            sb.append('/');
        }
        return sb.toString();
    }

    /**
     * Constructs a file path from a <code>file:</code> URI.
     *
     * <p>Will be an absolute path if the given URI is absolute.</p>
     *
     * <p>Swallows '%' that are not followed by two characters,
     * doesn't deal with non-ASCII characters.</p>
     *
     * @param uri the URI designating a file in the local filesystem.
     * @return the local file system path for the file.
     * @since Ant 1.6
     */
    public String fromURI(String uri) {
        String path = Locator.fromURI(uri);

        // catch exception if normalize thinks this is not an absolute path
        try {
            path = normalize(path).getAbsolutePath();
        } catch (BuildException e) {
            // relative path
        }
        return path;
    }

    /**
     * Compares two filenames.
     *
     * <p>Unlike java.io.File#equals this method will try to compare
     * the absolute paths and &quot;normalize&quot; the filenames
     * before comparing them.</p>
     *
     * @param f1 the file whose name is to be compared.
     * @param f2 the other file whose name is to be compared.
     *
     * @return true if the file are for the same file.
     *
     * @since Ant 1.5.3
     */
    public boolean fileNameEquals(File f1, File f2) {
        return normalize(f1.getAbsolutePath())
            .equals(normalize(f2.getAbsolutePath()));
    }

    /**
     * Renames a file, even if that involves crossing file system boundaries.
     *
     * <p>This will remove <code>to</code> (if it exists), ensure that
     * <code>to</code>'s parent directory exists and move
     * <code>from</code>, which involves deleting <code>from</code> as
     * well.</p>
     *
     * @throws IOException if anything bad happens during this
     * process.  Note that <code>to</code> may have been deleted
     * already when this happens.
     *
     * @param from the file to move
     * @param to the new file name
     *
     * @since Ant 1.6
     */
    public void rename(File from, File to) throws IOException {
        if (to.exists() && !to.delete()) {
            throw new IOException("Failed to delete " + to
                                  + " while trying to rename " + from);
        }

        File parent = getParentFile(to);
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create directory " + parent
                                  + " while trying to rename " + from);
        }

        if (!from.renameTo(to)) {
            copyFile(from, to);
            if (!from.delete()) {
                throw new IOException("Failed to delete " + from
                                      + " while trying to rename it.");
            }
        }
    }

}

