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
import java.text.DecimalFormat;
import java.util.Random;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;
import java.net.URL;
import java.net.MalformedURLException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.filters.util.ChainReaderHelper;
import org.apache.tools.ant.types.FilterSetCollection;
import org.apache.tools.ant.taskdefs.condition.Os;

/**
 * This class also encapsulates methods which allow Files to be
 * refered to using abstract path names which are translated to native
 * system file paths at runtime as well as copying files or setting
 * there last modification time.
 *
 * @author duncan@x180.com
 * @author Conor MacNeill
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 * @author <a href="mailto:jtulley@novell.com">Jeff Tulley</a> 
 *
 * @version $Revision$
 */

public class FileUtils {
    private static Random rand = new Random(System.currentTimeMillis());
    private static Object lockReflection = new Object();
    private static java.lang.reflect.Method setLastModified = null;

    private boolean onNetWare = Os.isFamily("netware");

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
     * Get the URL for a file taking into account # characters
     *
     * @param file the file whose URL representation is required.
     * @return The FileURL value
     * @throws MalformedURLException if the URL representation cannot be
     *      formed.
     */
    public URL getFileURL(File file) throws MalformedURLException {
        String uri = "file:" + file.getAbsolutePath().replace('\\', '/');
        for (int i = uri.indexOf('#'); i != -1; i = uri.indexOf('#')) {
            uri = uri.substring(0, i) + "%23" + uri.substring(i + 1);
        }
        if (file.isDirectory()) {
            uri += "/";
        }
        return new URL(uri);
    }
    
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
    public void copyFile(String sourceFile, String destFile, FilterSetCollection filters)
        throws IOException {
        copyFile(new File(sourceFile), new File(destFile), filters, false, false);
    }

    /**
     * Convienence method to copy a file from a source to a
     * destination specifying if token filtering must be used and if
     * source files may overwrite newer destination files.
     *
     * @throws IOException
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
     * @throws IOException
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
     * @throws IOException
     *
     * @since 1.14, Ant 1.5
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
     * @throws IOException
     *
     * @since 1.15, Ant 1.5
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
    public void copyFile(File sourceFile, File destFile, FilterSetCollection filters)
        throws IOException {
        copyFile(sourceFile, destFile, filters, false, false);
    }

    /**
     * Convienence method to copy a file from a source to a
     * destination specifying if token filtering must be used and if
     * source files may overwrite newer destination files.
     *
     * @throws IOException
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
     * @throws IOException
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
     * @throws IOException
     *
     * @since 1.14, Ant 1.5
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
     * @throws IOException
     *
     * @since 1.15, Ant 1.5
     */
    public void copyFile(File sourceFile, File destFile,
                         FilterSetCollection filters, Vector filterChains,
                         boolean overwrite, boolean preserveLastModified,
                         String encoding, Project project)
        throws IOException {

        if (overwrite || !destFile.exists() ||
            destFile.lastModified() < sourceFile.lastModified()) {

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

            if (filterSetsAvailable || filterChainsAvailable) {
                BufferedReader in = null;
                BufferedWriter out = null;

                try {
                    if (encoding == null) {
                        in = new BufferedReader(new FileReader(sourceFile));
                        out = new BufferedWriter(new FileWriter(destFile));
                    } else {
                        in = 
                            new BufferedReader(new InputStreamReader(
                                                 new FileInputStream(sourceFile), 
                                                 encoding));
                        out = 
                            new BufferedWriter(new OutputStreamWriter(
                                                 new FileOutputStream(destFile), 
                                                 encoding));
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
                    
                    int length;
                    String newline = null;
                    String line = in.readLine();
                    while (line != null) {
                        if (line.length() == 0) {
                            out.newLine();
                        } else {
                            if (filterSetsAvailable) {
                                newline = filters.replaceTokens(line);
                            } else {
                                newline = line;
                            }
                            out.write(newline);
                            out.newLine();
                        }
                        line = in.readLine();
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
     */
    protected final Method getSetLastModified() {
        if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)) {
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
            if (!path.startsWith(File.separator) &&
                !(path.length() >= 2 &&
                   Character.isLetter(path.charAt(0)) &&
                   colon == 1)) {
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
        if ((!onNetWare && 
             path.length() >= 2 &&
             Character.isLetter(path.charAt(0)) &&
             path.charAt(1) == ':') ||
            (onNetWare && colon > -1)) {

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
     * as it doesn't create the file itself and doesn't use platform
     * specific temporary directory when the parentDir attribute is
     * null.</p>
     *
     * @param parentDir Directory to create the temporary file in -
     * current working directory will be assumed if this parameter is
     * null.
     *
     * @since 1.8
     */
    public File createTempFile(String prefix, String suffix, File parentDir) {

        File result = null;
        String parent = null;
        if (parentDir != null) {
            parent = parentDir.getPath();
        }
        DecimalFormat fmt = new DecimalFormat("#####");
        synchronized (rand) {
            do {
                result = new File(parent,
                                  prefix + fmt.format(rand.nextInt())
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

        if (f1.equals(f2)) {
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
                } catch (IOException e) {}
            }
            if (in2 != null) {
                try {
                    in2.close();
                } catch (IOException e) {}
            }
        }
    }

    /**
     * Emulation of File.getParentFile for JDK 1.1
     *
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
     */
    public static final String readFully(Reader rdr) throws IOException {
        return readFully(rdr, 8192);
    }

    /**
     * Read from reader till EOF
     */
    public static final String readFully(Reader rdr, int bufferSize) throws IOException {
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
     * @since 1.21, Ant 1.5
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
        // if leading's path ends with a slash, it will be stripped by
        // normalize - we always add one so we never think /foo was a
        // parent directory of /foobar
        String l = normalize(leading.getAbsolutePath()).getAbsolutePath()
            + File.separator;
        String p = normalize(path.getAbsolutePath()).getAbsolutePath();
        if (p.startsWith(l)) {
            return p.substring(l.length());
        } else {
            return p;
        }
    }
}

