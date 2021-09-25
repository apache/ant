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
package org.apache.tools.ant.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.PathTokenizer;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.launch.Locator;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.types.FilterSetCollection;
import org.apache.tools.ant.types.resources.FileResource;

/**
 * This class also encapsulates methods which allow Files to be
 * referred to using abstract path names which are translated to native
 * system file paths at runtime as well as copying files or setting
 * their last modification time.
 *
 */
public class FileUtils {
    private static final int DELETE_RETRY_SLEEP_MILLIS = 10;
    private static final int EXPAND_SPACE = 50;
    private static final FileUtils PRIMARY_INSTANCE = new FileUtils();

    //get some non-crypto-grade randomness from various places.
    private static Random rand = new Random(System.currentTimeMillis()
            + Runtime.getRuntime().freeMemory());

    private static final boolean ON_NETWARE = Os.isFamily("netware");
    private static final boolean ON_DOS = Os.isFamily("dos");
    private static final boolean ON_WIN9X = Os.isFamily("win9x");
    private static final boolean ON_WINDOWS = Os.isFamily("windows");

    static final int BUF_SIZE = 8192;


    /**
     * The granularity of timestamps under FAT.
     */
    public static final long FAT_FILE_TIMESTAMP_GRANULARITY = 2000;

    /**
     * The granularity of timestamps under Unix.
     */
    public static final long UNIX_FILE_TIMESTAMP_GRANULARITY = 1000;

    /**
     * The granularity of timestamps under the NT File System.
     * NTFS has a granularity of 100 nanoseconds, which is less
     * than 1 millisecond, so we round this up to 1 millisecond.
     */
    public static final long NTFS_FILE_TIMESTAMP_GRANULARITY = 1;

    private static final FileAttribute[] TMPFILE_ATTRIBUTES =
        new FileAttribute[] {
            PosixFilePermissions.asFileAttribute(EnumSet.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE))
        };
    private static final FileAttribute[] TMPDIR_ATTRIBUTES =
        new FileAttribute[] {
            PosixFilePermissions.asFileAttribute(EnumSet.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE))
        };
    private static final FileAttribute[] NO_TMPFILE_ATTRIBUTES = new FileAttribute[0];

    /**
     * A one item cache for fromUri.
     * fromUri is called for each element when parsing ant build
     * files. It is a costly operation. This just caches the result
     * of the last call.
     */
    private Object cacheFromUriLock = new Object();
    private String cacheFromUriRequest = null;
    private String cacheFromUriResponse = null;

    /**
     * Factory method.
     *
     * @return a new instance of FileUtils.
     * @deprecated since 1.7.
     *             Use getFileUtils instead,
     * FileUtils do not have state.
     */
    @Deprecated
    public static FileUtils newFileUtils() {
        return new FileUtils();
    }

    /**
     * Method to retrieve The FileUtils, which is shared by all users of this
     * method.
     * @return an instance of FileUtils.
     * @since Ant 1.6.3
     */
    public static FileUtils getFileUtils() {
        return PRIMARY_INSTANCE;
    }

    /**
     * Empty constructor.
     */
    protected FileUtils() {
    }

    /**
     * Get the URL for a file taking into account # characters.
     *
     * @param file the file whose URL representation is required.
     * @return The FileURL value.
     * @throws MalformedURLException if the URL representation cannot be
     *      formed.
     */
    public URL getFileURL(File file) throws MalformedURLException {
        return new URL(file.toURI().toASCIIString());
    }

    /**
     * Convenience method to copy a file from a source to a destination.
     * No filtering is performed.
     *
     * @param sourceFile Name of file to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile Name of file to copy to.
     *                 Must not be <code>null</code>.
     *
     * @throws IOException if the copying fails.
     */
    public void copyFile(String sourceFile, String destFile) throws IOException {
        copyFile(new File(sourceFile), new File(destFile), null, false, false);
    }

    /**
     * Convenience method to copy a file from a source to a destination
     * specifying if token filtering must be used.
     *
     * @param sourceFile Name of file to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile Name of file to copy to.
     *                 Must not be <code>null</code>.
     * @param filters the collection of filters to apply to this copy.
     *
     * @throws IOException if the copying fails.
     */
    public void copyFile(String sourceFile, String destFile, FilterSetCollection filters)
            throws IOException {
        copyFile(new File(sourceFile), new File(destFile), filters, false, false);
    }

    /**
     * Convenience method to copy a file from a source to a destination specifying if token
     * filtering must be used and if source files may overwrite newer destination files.
     *
     * @param sourceFile Name of file to copy from. Must not be <code>null</code>.
     * @param destFile Name of file to copy to. Must not be <code>null</code>.
     * @param filters the collection of filters to apply to this copy.
     * @param overwrite Whether or not the destination file should be overwritten if it already
     *            exists.
     *
     * @throws IOException if the copying fails.
     */
    public void copyFile(String sourceFile, String destFile, FilterSetCollection filters,
                         boolean overwrite) throws IOException {
        copyFile(new File(sourceFile), new File(destFile), filters, overwrite, false);
    }

    /**
     * Convenience method to copy a file from a source to a destination
     * specifying if token
     * filtering must be used, if source files may overwrite newer destination
     * files and the last
     * modified time of <code>destFile</code> file should be made equal to
     * the last modified time
     * of <code>sourceFile</code>.
     *
     * @param sourceFile Name of file to copy from. Must not be <code>null</code>.
     * @param destFile Name of file to copy to. Must not be <code>null</code>.
     * @param filters the collection of filters to apply to this copy.
     * @param overwrite Whether or not the destination file should be
     *            overwritten if it already exists.
     * @param preserveLastModified Whether or not the last modified time of
     *            the resulting file
     *            should be set to that of the source file.
     *
     * @throws IOException if the copying fails.
     */
    public void copyFile(String sourceFile, String destFile,
                         FilterSetCollection filters,
                         boolean overwrite, boolean preserveLastModified)
        throws IOException {
        copyFile(new File(sourceFile), new File(destFile), filters, overwrite,
                 preserveLastModified);
    }

    /**
     * Convenience method to copy a file from a source to a destination specifying if token
     * filtering must be used, if source files may overwrite newer destination files and the last
     * modified time of <code>destFile</code> file should be made equal to the last modified time
     * of <code>sourceFile</code>.
     *
     * @param sourceFile Name of file to copy from. Must not be <code>null</code>.
     * @param destFile Name of file to copy to. Must not be <code>null</code>.
     * @param filters the collection of filters to apply to this copy.
     * @param overwrite Whether or not the destination file should be overwritten if it already
     *            exists.
     * @param preserveLastModified Whether or not the last modified time of the resulting file
     *            should be set to that of the source file.
     * @param encoding the encoding used to read and write the files.
     *
     * @throws IOException if the copying fails.
     *
     * @since Ant 1.5
     */
    public void copyFile(String sourceFile, String destFile,
                         FilterSetCollection filters, boolean overwrite,
                         boolean preserveLastModified, String encoding) throws IOException {
        copyFile(new File(sourceFile), new File(destFile), filters,
                 overwrite, preserveLastModified, encoding);
    }

    // CheckStyle:ParameterNumberCheck OFF - bc
    /**
     * Convenience method to copy a file from a source to a
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
     * @param filters the collection of filters to apply to this copy.
     * @param filterChains filterChains to apply during the copy.
     * @param overwrite Whether or not the destination file should be
     *                  overwritten if it already exists.
     * @param preserveLastModified Whether or not the last modified time of
     *                             the resulting file should be set to that
     *                             of the source file.
     * @param encoding the encoding used to read and write the files.
     * @param project the project instance.
     *
     * @throws IOException if the copying fails.
     *
     * @since Ant 1.5
     */
    public void copyFile(String sourceFile, String destFile,
                         FilterSetCollection filters, Vector<FilterChain> filterChains,
                         boolean overwrite, boolean preserveLastModified,
                         String encoding, Project project) throws IOException {
        copyFile(new File(sourceFile), new File(destFile), filters, filterChains, overwrite,
                preserveLastModified, encoding, project);
    }

    /**
     * Convenience method to copy a file from a source to a destination specifying if token
     * filtering must be used, if filter chains must be used, if source files may overwrite newer
     * destination files and the last modified time of <code>destFile</code> file should be made
     * equal to the last modified time of <code>sourceFile</code>.
     *
     * @param sourceFile Name of file to copy from. Must not be <code>null</code>.
     * @param destFile Name of file to copy to. Must not be <code>null</code>.
     * @param filters the collection of filters to apply to this copy.
     * @param filterChains filterChains to apply during the copy.
     * @param overwrite Whether or not the destination file should be overwritten if it already
     *            exists.
     * @param preserveLastModified Whether or not the last modified time of the resulting file
     *            should be set to that of the source file.
     * @param inputEncoding the encoding used to read the files.
     * @param outputEncoding the encoding used to write the files.
     * @param project the project instance.
     *
     * @throws IOException if the copying fails.
     *
     * @since Ant 1.6
     */
    public void copyFile(String sourceFile, String destFile,
                         FilterSetCollection filters, Vector<FilterChain> filterChains,
                         boolean overwrite, boolean preserveLastModified,
                         String inputEncoding, String outputEncoding,
                         Project project) throws IOException {
        copyFile(new File(sourceFile), new File(destFile), filters, filterChains, overwrite,
                preserveLastModified, inputEncoding, outputEncoding, project);
    }

    /**
     * Convenience method to copy a file from a source to a destination. No filtering is performed.
     *
     * @param sourceFile the file to copy from. Must not be <code>null</code>.
     * @param destFile the file to copy to. Must not be <code>null</code>.
     *
     * @throws IOException if the copying fails.
     */
    public void copyFile(File sourceFile, File destFile) throws IOException {
        copyFile(sourceFile, destFile, null, false, false);
    }

    /**
     * Convenience method to copy a file from a source to a destination
     * specifying if token filtering must be used.
     *
     * @param sourceFile the file to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile the file to copy to.
     *                 Must not be <code>null</code>.
     * @param filters the collection of filters to apply to this copy.
     *
     * @throws IOException if the copying fails.
     */
    public void copyFile(File sourceFile, File destFile, FilterSetCollection filters)
            throws IOException {
        copyFile(sourceFile, destFile, filters, false, false);
    }

    /**
     * Convenience method to copy a file from a source to a
     * destination specifying if token filtering must be used and if
     * source files may overwrite newer destination files.
     *
     * @param sourceFile the file to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile the file to copy to.
     *                 Must not be <code>null</code>.
     * @param filters the collection of filters to apply to this copy.
     * @param overwrite Whether or not the destination file should be
     *                  overwritten if it already exists.
     *
     * @throws IOException if the copying fails.
     */
    public void copyFile(File sourceFile, File destFile, FilterSetCollection filters,
                         boolean overwrite) throws IOException {
        copyFile(sourceFile, destFile, filters, overwrite, false);
    }

    /**
     * Convenience method to copy a file from a source to a
     * destination specifying if token filtering must be used, if
     * source files may overwrite newer destination files and the
     * last modified time of <code>destFile</code> file should be made equal
     * to the last modified time of <code>sourceFile</code>.
     *
     * @param sourceFile the file to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile the file to copy to.
     *                 Must not be <code>null</code>.
     * @param filters the collection of filters to apply to this copy.
     * @param overwrite Whether or not the destination file should be
     *                  overwritten if it already exists.
     * @param preserveLastModified Whether or not the last modified time of
     *                             the resulting file should be set to that
     *                             of the source file.
     *
     * @throws IOException if the copying fails.
     */
    public void copyFile(File sourceFile, File destFile, FilterSetCollection filters,
                         boolean overwrite, boolean preserveLastModified) throws IOException {
        copyFile(sourceFile, destFile, filters, overwrite, preserveLastModified, null);
    }

    /**
     * Convenience method to copy a file from a source to a destination specifying if token
     * filtering must be used, if source files may overwrite newer destination files, the last
     * modified time of <code>destFile</code> file should be made equal to the last modified time
     * of <code>sourceFile</code> and which character encoding to assume.
     *
     * @param sourceFile the file to copy from. Must not be <code>null</code>.
     * @param destFile the file to copy to. Must not be <code>null</code>.
     * @param filters the collection of filters to apply to this copy.
     * @param overwrite Whether or not the destination file should be overwritten if it already
     *            exists.
     * @param preserveLastModified Whether or not the last modified time of the resulting file
     *            should be set to that of the source file.
     * @param encoding the encoding used to read and write the files.
     *
     * @throws IOException if the copying fails.
     *
     * @since Ant 1.5
     */
    public void copyFile(File sourceFile, File destFile,
                         FilterSetCollection filters, boolean overwrite,
                         boolean preserveLastModified, String encoding) throws IOException {
        copyFile(sourceFile, destFile, filters, null, overwrite,
                 preserveLastModified, encoding, null);
    }

    /**
     * Convenience method to copy a file from a source to a
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
     * @param filters the collection of filters to apply to this copy.
     * @param filterChains filterChains to apply during the copy.
     * @param overwrite Whether or not the destination file should be
     *                  overwritten if it already exists.
     * @param preserveLastModified Whether or not the last modified time of
     *                             the resulting file should be set to that
     *                             of the source file.
     * @param encoding the encoding used to read and write the files.
     * @param project the project instance.
     *
     * @throws IOException if the copying fails.
     *
     * @since Ant 1.5
     */
    public void copyFile(File sourceFile, File destFile,
                         FilterSetCollection filters, Vector<FilterChain> filterChains,
                         boolean overwrite, boolean preserveLastModified,
                         String encoding, Project project) throws IOException {
        copyFile(sourceFile, destFile, filters, filterChains,
                 overwrite, preserveLastModified, encoding, encoding, project);
    }

    /**
     * Convenience method to copy a file from a source to a
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
     * @param filters the collection of filters to apply to this copy.
     * @param filterChains filterChains to apply during the copy.
     * @param overwrite Whether or not the destination file should be
     *                  overwritten if it already exists.
     * @param preserveLastModified Whether or not the last modified time of
     *                             the resulting file should be set to that
     *                             of the source file.
     * @param inputEncoding the encoding used to read the files.
     * @param outputEncoding the encoding used to write the files.
     * @param project the project instance.
     *
     *
     * @throws IOException if the copying fails.
     *
     * @since Ant 1.6
     */
    public void copyFile(File sourceFile, File destFile,
            FilterSetCollection filters, Vector<FilterChain> filterChains,
            boolean overwrite, boolean preserveLastModified,
            String inputEncoding, String outputEncoding,
            Project project) throws IOException {
        copyFile(sourceFile, destFile, filters, filterChains, overwrite, preserveLastModified,
                false, inputEncoding, outputEncoding, project);
    }

    /**
     * Convenience method to copy a file from a source to a
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
     * @param filters the collection of filters to apply to this copy.
     * @param filterChains filterChains to apply during the copy.
     * @param overwrite Whether or not the destination file should be
     *                  overwritten if it already exists.
     * @param preserveLastModified Whether or not the last modified time of
     *                             the resulting file should be set to that
     *                             of the source file.
     * @param append whether to append to the destination file.
     * @param inputEncoding the encoding used to read the files.
     * @param outputEncoding the encoding used to write the files.
     * @param project the project instance.
     *
     *
     * @throws IOException if the copying fails.
     *
     * @since Ant 1.8
     */
    public void copyFile(File sourceFile, File destFile,
                         FilterSetCollection filters, Vector<FilterChain> filterChains,
                         boolean overwrite, boolean preserveLastModified,
                         boolean append,
                         String inputEncoding, String outputEncoding,
                         Project project) throws IOException {
        copyFile(sourceFile, destFile, filters, filterChains, overwrite,
                 preserveLastModified, append, inputEncoding, outputEncoding,
                 project, /* force: */ false);
    }

    /**
     * Convenience method to copy a file from a source to a
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
     * @param filters the collection of filters to apply to this copy.
     * @param filterChains filterChains to apply during the copy.
     * @param overwrite Whether or not the destination file should be
     *                  overwritten if it already exists.
     * @param preserveLastModified Whether or not the last modified time of
     *                             the resulting file should be set to that
     *                             of the source file.
     * @param append whether to append to the destination file.
     * @param inputEncoding the encoding used to read the files.
     * @param outputEncoding the encoding used to write the files.
     * @param project the project instance.
     * @param force whether to overwrite read-only destination files.
     *
     * @throws IOException if the copying fails.
     *
     * @since Ant 1.8.2
     */
    public void copyFile(File sourceFile, File destFile,
                         FilterSetCollection filters, Vector<FilterChain> filterChains,
                         boolean overwrite, boolean preserveLastModified,
                         boolean append,
                         String inputEncoding, String outputEncoding,
                         Project project, boolean force) throws IOException {
        ResourceUtils.copyResource(new FileResource(sourceFile),
                                   new FileResource(destFile),
                                   filters, filterChains, overwrite,
                                   preserveLastModified, append, inputEncoding,
                                   outputEncoding, project, force);
    }

    // CheckStyle:ParameterNumberCheck ON

    /**
     * Calls File.setLastModified(long time). Originally written to
     * to dynamically bind to that call on Java1.2+.
     *
     * @param file the file whose modified time is to be set
     * @param time the time to which the last modified time is to be set.
     *             if this is -1, the current time is used.
     */
    public void setFileLastModified(File file, long time) {
        ResourceUtils.setLastModified(new FileResource(file), time);
    }

    /**
     * Interpret the filename as a file relative to the given file
     * unless the filename already represents an absolute filename.
     * Differs from <code>new File(file, filename)</code> in that
     * the resulting File's path will always be a normalized,
     * absolute pathname.  Also, if it is determined that
     * <code>filename</code> is context-relative, <code>file</code>
     * will be discarded and the reference will be resolved using
     * available context/state information about the filesystem.
     *
     * @param file the "reference" file for relative paths. This
     * instance must be an absolute file and must not contain
     * &quot;./&quot; or &quot;../&quot; sequences (same for \ instead
     * of /).  If it is null, this call is equivalent to
     * <code>new java.io.File(filename).getAbsoluteFile()</code>.
     *
     * @param filename a file name.
     *
     * @return an absolute file.
     * @throws java.lang.NullPointerException if filename is null.
     */
    public File resolveFile(File file, String filename) {
        if (!isAbsolutePath(filename)) {
            char sep = File.separatorChar;
            filename = filename.replace('/', sep).replace('\\', sep);
            if (isContextRelativePath(filename)) {
                file = null;
                // on cygwin, our current directory can be a UNC;
                // assume user.dir is absolute or all hell breaks loose...
                String udir = System.getProperty("user.dir");
                if (filename.charAt(0) == sep && udir.charAt(0) == sep) {
                    filename = dissect(udir)[0] + filename.substring(1);
                }
            }
            filename = new File(file, filename).getAbsolutePath();
        }
        return normalize(filename);
    }

    /**
     * On DOS and NetWare, the evaluation of certain file
     * specifications is context-dependent.  These are filenames
     * beginning with a single separator (relative to current root directory)
     * and filenames with a drive specification and no intervening separator
     * (relative to current directory of the specified root).
     * @param filename the filename to evaluate.
     * @return true if the filename is relative to system context.
     * @throws java.lang.NullPointerException if filename is null.
     * @since Ant 1.7
     */
    public static boolean isContextRelativePath(String filename) {
        if (!(ON_DOS || ON_NETWARE) || filename.isEmpty()) {
            return false;
        }
        char sep = File.separatorChar;
        filename = filename.replace('/', sep).replace('\\', sep);
        char c = filename.charAt(0);
        int len = filename.length();
        return (c == sep && (len == 1 || filename.charAt(1) != sep))
                || (Character.isLetter(c) && len > 1
                && filename.charAt(1) == ':'
                && (len == 2 || filename.charAt(2) != sep));
    }

    /**
     * Verifies that the specified filename represents an absolute path.
     * Differs from new java.io.File("filename").isAbsolute() in that a path
     * beginning with a double file separator--signifying a Windows UNC--must
     * at minimum match "\\a\b" to be considered an absolute path.
     * @param filename the filename to be checked.
     * @return true if the filename represents an absolute path.
     * @throws java.lang.NullPointerException if filename is null.
     * @since Ant 1.6.3
     */
    public static boolean isAbsolutePath(String filename) {
        if (filename.isEmpty()) {
            return false;
        }
        int len = filename.length();
        char sep = File.separatorChar;
        filename = filename.replace('/', sep).replace('\\', sep);
        char c = filename.charAt(0);
        if (!ON_DOS && !ON_NETWARE) {
            return c == sep;
        }
        if (c == sep) {
            // CheckStyle:MagicNumber OFF
            if (!ON_DOS || len <= 4 || filename.charAt(1) != sep) {
                return false;
            }
            // CheckStyle:MagicNumber ON
            int nextsep = filename.indexOf(sep, 2);
            return nextsep > 2 && nextsep + 1 < len;
        }
        int colon = filename.indexOf(':');
        return (Character.isLetter(c) && colon == 1
                && filename.length() > 2 && filename.charAt(2) == sep)
                || (ON_NETWARE && colon > 0);
    }

    /**
     * Translate a path into its native (platform specific) format.
     * <p>
     * This method uses PathTokenizer to separate the input path
     * into its components. This handles DOS style paths in a relatively
     * sensible way. The file separators are then converted to their platform
     * specific versions.
     *
     * @param toProcess The path to be translated.
     *                  May be <code>null</code>.
     *
     * @return the native version of the specified path or
     *         an empty string if the path is <code>null</code> or empty.
     *
     * @since ant 1.7
     * @see PathTokenizer
     */
    public static String translatePath(String toProcess) {
        if (toProcess == null || toProcess.isEmpty()) {
            return "";
        }
        StringBuilder path = new StringBuilder(toProcess.length() + EXPAND_SPACE);
        PathTokenizer tokenizer = new PathTokenizer(toProcess);
        while (tokenizer.hasMoreTokens()) {
            String pathComponent = tokenizer.nextToken();
            pathComponent = pathComponent.replace('/', File.separatorChar);
            pathComponent = pathComponent.replace('\\', File.separatorChar);
            if (path.length() > 0) {
                path.append(File.pathSeparatorChar);
            }
            path.append(pathComponent);
        }
        return path.toString();
    }

    /**
     * &quot;Normalize&quot; the given absolute path.
     *
     * <p>This includes:
     * <ul>
     *   <li>Uppercase the drive letter if there is one.</li>
     *   <li>Remove redundant slashes after the drive spec.</li>
     *   <li>Resolve all ./, .\, ../ and ..\ sequences.</li>
     *   <li>DOS style paths that start with a drive letter will have
     *     \ as the separator.</li>
     * </ul>
     * <p>Unlike {@link File#getCanonicalPath()} this method
     * specifically does not resolve symbolic links.</p>
     *
     * <p>If the path tries to go beyond the file system root (i.e. it
     * contains more ".." segments than can be travelled up) the
     * method will return the original path unchanged.</p>
     *
     * @param path the path to be normalized.
     * @return the normalized version of the path.
     *
     * @throws java.lang.NullPointerException if path is null.
     */
    public File normalize(final String path) {
        Stack<String> s = new Stack<>();
        String[] dissect = dissect(path);
        s.push(dissect[0]);

        StringTokenizer tok = new StringTokenizer(dissect[1], File.separator);
        while (tok.hasMoreTokens()) {
            String thisToken = tok.nextToken();
            if (".".equals(thisToken)) {
                continue;
            }
            if ("..".equals(thisToken)) {
                if (s.size() < 2) {
                    // Cannot resolve it, so skip it.
                    return new File(path);
                }
                s.pop();
            } else { // plain component
                s.push(thisToken);
            }
        }
        StringBuilder sb = new StringBuilder();
        final int size = s.size();
        for (int i = 0; i < size; i++) {
            if (i > 1) {
                // not before the filesystem root and not after it, since root
                // already contains one
                sb.append(File.separatorChar);
            }
            sb.append(s.elementAt(i));
        }
        return new File(sb.toString());
    }

    /**
     * Dissect the specified absolute path.
     * @param path the path to dissect.
     * @return String[] {root, remaining path}.
     * @throws java.lang.NullPointerException if path is null.
     * @since Ant 1.7
     */
    public String[] dissect(String path) {
        char sep = File.separatorChar;
        path = path.replace('/', sep).replace('\\', sep);

        // make sure we are dealing with an absolute path
        if (!isAbsolutePath(path)) {
            throw new BuildException(path + " is not an absolute path");
        }
        String root;
        int colon = path.indexOf(':');
        if (colon > 0 && (ON_DOS || ON_NETWARE)) {

            int next = colon + 1;
            root = path.substring(0, next);
            char[] ca = path.toCharArray();
            root += sep;
            //remove the initial separator; the root has it.
            next = (ca[next] == sep) ? next + 1 : next;

            final StringBuilder sbPath = new StringBuilder();
            // Eliminate consecutive slashes after the drive spec:
            for (int i = next; i < ca.length; i++) {
                if (ca[i] != sep || ca[i - 1] != sep) {
                    sbPath.append(ca[i]);
                }
            }
            path = sbPath.toString();
        } else if (path.length() > 1 && path.charAt(1) == sep) {
            // UNC drive
            int nextsep = path.indexOf(sep, 2);
            nextsep = path.indexOf(sep, nextsep + 1);
            root = (nextsep > 2) ? path.substring(0, nextsep + 1) : path;
            path = path.substring(root.length());
        } else {
            root = File.separator;
            path = path.substring(1);
        }
        return new String[] {root, path};
    }

    /**
     * Returns a VMS String representation of a <code>File</code> object.
     * This is useful since the JVM by default internally converts VMS paths
     * to Unix style.
     * The returned String is always an absolute path.
     *
     * @param f The <code>File</code> to get the VMS path for.
     * @return The absolute VMS path to <code>f</code>.
     */
    public String toVMSPath(File f) {
        // format: "DEVICE:[DIR.SUBDIR]FILE"
        String osPath;
        String path = normalize(f.getAbsolutePath()).getPath();
        String name = f.getName();
        boolean isAbsolute = path.charAt(0) == File.separatorChar;
        // treat directories specified using .DIR syntax as files
        // CheckStyle:MagicNumber OFF
        boolean isDirectory = f.isDirectory()
                && !name.regionMatches(true, name.length() - 4, ".DIR", 0, 4);
        // CheckStyle:MagicNumber ON
        String device = null;
        StringBuilder directory = null;
        String file = null;

        int index = 0;

        if (isAbsolute) {
            index = path.indexOf(File.separatorChar, 1);
            if (index == -1) {
                return path.substring(1) + ":[000000]";
            }
            device = path.substring(1, index++);
        }
        if (isDirectory) {
            directory = new StringBuilder(path.substring(index).replace(File.separatorChar, '.'));
        } else {
            int dirEnd = path.lastIndexOf(File.separatorChar);
            if (dirEnd == -1 || dirEnd < index) {
                file = path.substring(index);
            } else {
                directory = new StringBuilder(path.substring(index, dirEnd)
                        .replace(File.separatorChar, '.'));
                index = dirEnd + 1;
                if (path.length() > index) {
                    file = path.substring(index);
                }
            }
        }
        if (!isAbsolute && directory != null) {
            directory.insert(0, '.');
        }
        osPath = ((device != null) ? device + ":" : "")
                + ((directory != null) ? "[" + directory + "]" : "")
                + ((file != null) ? file : "");
        return osPath;
    }

    /**
     * Create a File object for a temporary file in a given directory. Without
     * actually creating the file.
     *
     * <p>
     * The file denoted by the returned abstract pathname did not exist before
     * this method was invoked, any subsequent invocation of this method will
     * yield a different file name.
     * </p>
     *
     * <p>If the filesystem where the temporary file is created
     * supports POSIX permissions, the file will only be readable and
     * writable by the current user.</p>
     *
     * @param prefix file name prefix.
     * @param suffix
     *            file extension; include the '.'.
     * @param parentDir
     *            Directory to create the temporary file in; java.io.tmpdir used
     *            if not specified.
     *
     * @deprecated since ant 1.7.1 use createTempFile(Project, String, String, File,
     * boolean, boolean) instead.
     * @return a File reference to the new, nonexistent temporary file.
     */
    @Deprecated
    public File createTempFile(String prefix, String suffix, File parentDir) {
        return createTempFile(prefix, suffix, parentDir, false, false);
    }

    /**
     * Create a temporary file in a given directory.
     *
     * <p>The file denoted by the returned abstract pathname did not
     * exist before this method was invoked, any subsequent invocation
     * of this method will yield a different file name.</p>
     *
     * <p>If the filesystem where the temporary file is created
     * supports POSIX permissions, the file will only be readable and
     * writable by the current user.</p>
     *
     * @param prefix file name prefix.
     * @param suffix file extension; include the '.'.
     * @param parentDir Directory to create the temporary file in;
     * java.io.tmpdir used if not specified.
     * @param deleteOnExit whether to set the tempfile for deletion on
     *        normal VM exit.
     * @param createFile true if the file must actually be created. If false
     * chances exist that a file with the same name is created in the time
     * between invoking this method and the moment the file is actually created.
     * If possible set to true.
     *
     * @return a File reference to the new temporary file.
     * @since Ant 1.7.1
     * @deprecated since Ant 1.10.8 use createTempFile(Project, String, String, File,
     * boolean, boolean) instead.
     */
    @Deprecated
    public File createTempFile(String prefix, String suffix, File parentDir,
            boolean deleteOnExit, boolean createFile) {
        return createTempFile(null, prefix, suffix, parentDir, deleteOnExit, createFile);
    }

    private static final String NULL_PLACEHOLDER = "null";

    /**
     * Create a temporary file in a given directory.
     *
     * <p>The file denoted by the returned abstract pathname did not
     * exist before this method was invoked, any subsequent invocation
     * of this method will yield a different file name.</p>
     *
     * <p>If the filesystem where the temporary file is created
     * supports POSIX permissions, the file will only be readable and
     * writable by the current user.</p>
     *
     * @param project reference to the current Ant project.
     * @param prefix file name prefix.
     * @param suffix file extension; include the '.'.
     * @param parentDir Directory to create the temporary file in;
     *        if not specified and {@code project} is not null then the value
     *        of the property {@code ant.tmpdir} is used if set;
     *        otherwise {@code java.io.tmpdir} is used.
     * @param deleteOnExit whether to set the tempfile for deletion on
     *        normal VM exit.
     * @param createFile true if the file must actually be created. If false
     * chances exist that a file with the same name is created in the time
     * between invoking this method and the moment the file is actually created.
     * If possible set to true.
     *
     * @return a File reference to the new temporary file.
     * @since Ant 1.9.15
     */
    public File createTempFile(final Project project, String prefix, String suffix,
            final File parentDir, final boolean deleteOnExit, final boolean createFile) {
        File result;
        String p = null;
        if (parentDir != null) {
            p = parentDir.getPath();
        } else if (project != null && project.getProperty(MagicNames.TMPDIR) != null) {
            p = project.getProperty(MagicNames.TMPDIR);
        } else if (project != null && deleteOnExit) {
            if (project.getProperty(MagicNames.AUTO_TMPDIR) != null) {
                p = project.getProperty(MagicNames.AUTO_TMPDIR);
            } else {
                final Path systemTempDirPath =
                    new File(System.getProperty("java.io.tmpdir")).toPath();
                final PosixFileAttributeView systemTempDirPosixAttributes =
                    Files.getFileAttributeView(systemTempDirPath, PosixFileAttributeView.class);
                if (systemTempDirPosixAttributes != null) {
                    // no reason to create an extra temp dir if we cannot set permissions
                    try {
                        final File projectTempDir = Files.createTempDirectory(systemTempDirPath,
                            "ant", TMPDIR_ATTRIBUTES)
                            .toFile();
                        projectTempDir.deleteOnExit();
                        p = projectTempDir.getAbsolutePath();
                        project.setProperty(MagicNames.AUTO_TMPDIR, p);
                    } catch (IOException ex) {
                        // silently fall back to system temp directory
                    }
                }
            }
        }
        final String parent = p != null ? p : System.getProperty("java.io.tmpdir");
        if (prefix == null) {
            prefix = NULL_PLACEHOLDER;
        }
        if (suffix == null) {
            suffix = NULL_PLACEHOLDER;
        }

        if (createFile) {
            try {
                final Path parentPath = new File(parent).toPath();
                final PosixFileAttributeView parentPosixAttributes =
                    Files.getFileAttributeView(parentPath, PosixFileAttributeView.class);
                result = Files.createTempFile(parentPath, prefix, suffix,
                    parentPosixAttributes != null ? TMPFILE_ATTRIBUTES : NO_TMPFILE_ATTRIBUTES)
                    .toFile();
            } catch (IOException e) {
                throw new BuildException("Could not create tempfile in "
                        + parent, e);
            }
        } else {
            DecimalFormat fmt = new DecimalFormat("#####");
            synchronized (rand) {
                do {
                    result = new File(parent, prefix
                            + fmt.format(rand.nextInt(Integer.MAX_VALUE)) + suffix);
                } while (result.exists());
            }
        }

        if (deleteOnExit) {
            result.deleteOnExit();
        }
        return result;
    }

    /**
     * Create a File object for a temporary file in a given directory. Without
     * actually creating the file.
     *
     * <p>
     * The file denoted by the returned abstract pathname did not exist before
     * this method was invoked, any subsequent invocation of this method will
     * yield a different file name.
     * </p>
     *
     * <p>If the filesystem where the temporary file is created
     * supports POSIX permissions, the file will only be readable and
     * writable by the current user.</p>
     *
     * @param prefix file name prefix.
     * @param suffix
     *            file extension; include the '.'.
     * @param parentDir
     *            Directory to create the temporary file in; java.io.tmpdir used
     *            if not specified.
     * @param deleteOnExit
     *            whether to set the tempfile for deletion on normal VM exit.
     *
     * @deprecated since ant 1.7.1 use createTempFile(Project, String, String, File,
     * boolean, boolean) instead.
     * @return a File reference to the new, nonexistent temporary file.
     */
    @Deprecated
    public File createTempFile(String prefix, String suffix,
            File parentDir, boolean deleteOnExit) {
        return createTempFile(prefix, suffix, parentDir, deleteOnExit, false);
    }

    /**
     * Compares the contents of two files.
     *
     * @param f1 the file whose content is to be compared.
     * @param f2 the other file whose content is to be compared.
     *
     * @return true if the content of the files is the same.
     *
     * @throws IOException if the files cannot be read.
     */
    public boolean contentEquals(File f1, File f2) throws IOException {
        return contentEquals(f1, f2, false);
    }

    /**
     * Compares the contents of two files.
     *
     * @param f1 the file whose content is to be compared.
     * @param f2 the other file whose content is to be compared.
     * @param textfile true if the file is to be treated as a text file and
     *        differences in kind of line break are to be ignored.
     *
     * @return true if the content of the files is the same.
     *
     * @throws IOException if the files cannot be read.
     * @since Ant 1.6.3
     */
    public boolean contentEquals(File f1, File f2, boolean textfile) throws IOException {
        return ResourceUtils.contentEquals(new FileResource(f1), new FileResource(f2), textfile);
    }

    /**
     * This was originally an emulation of {@link File#getParentFile} for JDK 1.1, but it is now
     * implemented using that method (Ant 1.6.3 onwards).
     *
     * @param f the file whose parent is required.
     * @return the given file's parent, or null if the file does not have a parent.
     * @since 1.10
     * @deprecated since 1.7. Just use {@link File#getParentFile} directly.
     */
    @Deprecated
    public File getParentFile(File f) {
        return f == null ? null : f.getParentFile();
    }

    /**
     * Read from reader till EOF.
     * @param rdr the reader from which to read.
     * @return the contents read out of the given reader.
     *
     * @throws IOException if the contents could not be read out from the
     *         reader.
     */
    public static String readFully(Reader rdr) throws IOException {
        return readFully(rdr, BUF_SIZE);
    }

    /**
     * Read from reader till EOF.
     *
     * @param rdr the reader from which to read.
     * @param bufferSize the buffer size to use when reading.
     *
     * @return the contents read out of the given reader.
     *
     * @throws IOException if the contents could not be read out from the
     *         reader.
     */
    public static String readFully(Reader rdr, int bufferSize)
        throws IOException {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException(
                "Buffer size must be greater than 0");
        }
        final char[] buffer = new char[bufferSize];
        int bufferLength = 0;
        StringBuilder textBuffer = new StringBuilder();
        while (bufferLength != -1) {
            bufferLength = rdr.read(buffer);
            if (bufferLength > 0) {
                textBuffer.append(buffer, 0, bufferLength);
            }
        }
        return (textBuffer.length() == 0) ? null : textBuffer.toString();
    }

    /**
     * Safe read fully - do not return a null for an empty reader.
     * @param reader the input to read from.
     * @return the string.
     * @throws IOException if unable to read from reader.
     * @since Ant 1.7.1
     */
    public static String safeReadFully(Reader reader) throws IOException {
        String ret = readFully(reader);
        return ret == null ? "" : ret;
    }

    /**
     * This was originally an emulation of File.createNewFile for JDK 1.1,
     * but it is now implemented using that method (Ant 1.6.3 onwards).
     *
     * <p>This method has historically <strong>not</strong> guaranteed that the
     * operation was atomic. In its current implementation it is.
     *
     * @param f the file to be created.
     * @return true if the file did not exist already.
     * @throws IOException on error.
     * @since Ant 1.5
     */
    public boolean createNewFile(File f) throws IOException {
        return f.createNewFile();
    }

    /**
     * Create a new file, optionally creating parent directories.
     *
     * @param f the file to be created.
     * @param mkdirs <code>boolean</code> whether to create parent directories.
     * @return true if the file did not exist already.
     * @throws IOException on error.
     * @since Ant 1.6.3
     */
    public boolean createNewFile(File f, boolean mkdirs) throws IOException {
        File parent = f.getParentFile();
        if (mkdirs && !(parent.exists())) {
            parent.mkdirs();
        }
        return f.createNewFile();
    }

    /**
     * Checks whether a given file is a symbolic link.
     *
     * <p>It doesn't really test for symbolic links but whether the
     * canonical and absolute paths of the file are identical--this
     * may lead to false positives on some platforms.</p>
     *
     * @param parent the parent directory of the file to test
     * @param name the name of the file to test.
     *
     * @return true if the file is a symbolic link.
     * @throws IOException on error.
     * @since Ant 1.5
     * @deprecated use {@link Files#isSymbolicLink(Path)} instead
     */
    @Deprecated
    public boolean isSymbolicLink(final File parent, final String name)
        throws IOException {
        if (parent == null) {
            return Files.isSymbolicLink(Paths.get(name));
        }
        return Files.isSymbolicLink(Paths.get(parent.toPath().toString(), name));
    }

    /**
     * Removes a leading path from a second path.
     *
     * <p>This method uses {@link #normalize} under the covers and
     * does not resolve symbolic links.</p>
     *
     * @param leading The leading path, must not be null, must be absolute.
     * @param path The path to remove from, must not be null, must be absolute.
     *
     * @return path's normalized absolute if it doesn't start with
     * leading; path's path with leading's path removed otherwise.
     *
     * @since Ant 1.5
     */
    public String removeLeadingPath(File leading, File path) {
        String l = normalize(leading.getAbsolutePath()).getAbsolutePath();
        String p = normalize(path.getAbsolutePath()).getAbsolutePath();
        if (l.equals(p)) {
            return "";
        }
        // ensure that l ends with a /
        // so we never think /foo was a parent directory of /foobar
        if (!l.endsWith(File.separator)) {
            l += File.separator;
        }
        return (p.startsWith(l)) ? p.substring(l.length()) : p;
    }

    /**
     * Learn whether one path "leads" another.
     *
     * <p>This method uses {@link #normalize} under the covers and
     * does not resolve symbolic links.</p>
     *
     * <p>If either path tries to go beyond the file system root
     * (i.e. it contains more ".." segments than can be travelled up)
     * the method will return false.</p>
     *
     * @param leading The leading path, must not be null, must be absolute.
     * @param path The path to check, must not be null, must be absolute.
     * @return true if path starts with leading; false otherwise.
     * @since Ant 1.7
     */
    public boolean isLeadingPath(File leading, File path) {
        String l = normalize(leading.getAbsolutePath()).getAbsolutePath();
        String p = normalize(path.getAbsolutePath()).getAbsolutePath();
        if (l.equals(p)) {
            return true;
        }
        // ensure that l ends with a /
        // so we never think /foo was a parent directory of /foobar
        if (!l.endsWith(File.separator)) {
            l += File.separator;
        }
        // ensure "/foo/"  is not considered a parent of "/foo/../../bar"
        String up = File.separator + ".." + File.separator;
        if (l.contains(up) || p.contains(up) || (p + File.separator).contains(up)) {
            return false;
        }
        return p.startsWith(l);
    }

    /**
     * Learn whether one path "leads" another.
     *
     * @param leading The leading path, must not be null, must be absolute.
     * @param path The path to check, must not be null, must be absolute.
     * @param resolveSymlinks whether symbolic links shall be resolved
     * prior to comparing the paths.
     * @return true if path starts with leading; false otherwise.
     * @since Ant 1.10.5
     * @throws IOException if resolveSymlinks is true and invoking
     * getCanonicaPath on either argument throws an exception
     */
    public boolean isLeadingPath(File leading, File path, boolean resolveSymlinks)
        throws IOException {
        if (!resolveSymlinks) {
            return isLeadingPath(leading, path);
        }
        final File l = leading.getCanonicalFile();
        File p = path.getCanonicalFile();
        do {
            if (l.equals(p)) {
                return true;
            }
            p = p.getParentFile();
        } while (p != null);
        return false;
    }

    /**
     * Constructs a <code>file:</code> URI that represents the
     * external form of the given pathname.
     *
     * <p>Will be an absolute URI if the given path is absolute.</p>
     *
     * <p>This code encodes non ASCII characters too.</p>
     *
     * <p>The coding of the output is the same as what File.toURI().toASCIIString() produces</p>
     *
     * See <a href="https://www.w3.org/TR/xml11/#dt-sysid">dt-sysid</a>
     * which makes some mention of how
     * characters not supported by URI Reference syntax should be escaped.
     *
     * @param path the path in the local file system.
     * @return the URI version of the local path.
     * @since Ant 1.6
     */
    public String toURI(String path) {
        return new File(path).toURI().toASCIIString();
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
        synchronized (cacheFromUriLock) {
            if (uri.equals(cacheFromUriRequest)) {
                return cacheFromUriResponse;
            }
            String path = Locator.fromURI(uri);
            String ret = isAbsolutePath(path) ? normalize(path).getAbsolutePath() : path;
            cacheFromUriRequest = uri;
            cacheFromUriResponse = ret;
            return ret;
        }
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
        return normalize(f1.getAbsolutePath()).getAbsolutePath().equals(
                normalize(f2.getAbsolutePath()).getAbsolutePath());
    }

    /**
     * Are the two File instances pointing to the same object on the
     * file system?
     *
     * @param f1 File
     * @param f2 File
     * @return boolean
     * @throws IOException if file name canonicalization fails
     * @since Ant 1.8.2
     */
    public boolean areSame(File f1, File f2) throws IOException {
        if (f1 == null && f2 == null) {
            return true;
        }
        if (f1 == null || f2 == null) {
            return false;
        }
        return fileNameEquals(f1, f2) || isSameFile(f1, f2);
    }

    private boolean isSameFile(File f1, File f2) throws IOException {
        if (f1.exists()) {
            try {
                return f2.exists() && Files.isSameFile(f1.toPath(), f2.toPath());
            } catch (NoSuchFileException e) {
                // file has been removed between exists check and isSameFile?
                return false;
            }
        }
        File f1Normalized = normalize(f1.getAbsolutePath());
        File f2Normalized = normalize(f2.getAbsolutePath());
        return f1Normalized.getCanonicalFile().equals(f2Normalized
                                                      .getCanonicalFile());
    }

    /**
     * Renames a file, even if that involves crossing file system boundaries.
     *
     * <p>This will remove <code>to</code> (if it exists), ensure that
     * <code>to</code>'s parent directory exists and move
     * <code>from</code>, which involves deleting <code>from</code> as
     * well.</p>
     *
     * @param from the file to move.
     * @param to the new file name.
     * @throws IOException if anything bad happens during this
     * process.  Note that <code>to</code> may have been deleted
     * already when this happens.
     * @since Ant 1.6
     */
    public void rename(File from, File to) throws IOException {
        // identical logic lives in Move.renameFile():
        from = normalize(from.getAbsolutePath()).getCanonicalFile();
        to = normalize(to.getAbsolutePath());
        if (!from.exists()) {
            System.err.println("Cannot rename nonexistent file " + from);
            return;
        }
        if (from.getAbsolutePath().equals(to.getAbsolutePath())) {
            System.err.println("Rename of " + from + " to " + to + " is a no-op.");
            return;
        }
        if (to.exists() && !(areSame(from, to) || tryHardToDelete(to))) {
            throw new IOException("Failed to delete " + to + " while trying to rename " + from);
        }
        File parent = to.getParentFile();
        if (parent != null && !parent.isDirectory()
            && !(parent.mkdirs() || parent.isDirectory())) {
            throw new IOException("Failed to create directory " + parent
                                  + " while trying to rename " + from);
        }
        if (!from.renameTo(to)) {
            copyFile(from, to);
            if (!tryHardToDelete(from)) {
                throw new IOException("Failed to delete " + from + " while trying to rename it.");
            }
        }
    }

    /**
     * Get the granularity of file timestamps. The choice is made based on OS, which is
     * incorrect--it should really be by filesystem. We do not have an easy way to probe for file
     * systems, however, so this heuristic gives us a decent default.
     *
     * @return the difference, in milliseconds, which two file timestamps must have in order for the
     *         two files to be considered to have different timestamps.
     */
    public long getFileTimestampGranularity() {
        if (ON_WIN9X) {
            return FAT_FILE_TIMESTAMP_GRANULARITY;
        }
        if (ON_WINDOWS) {
            return NTFS_FILE_TIMESTAMP_GRANULARITY;
        }
        if (ON_DOS) {
            return FAT_FILE_TIMESTAMP_GRANULARITY;
        }
        return UNIX_FILE_TIMESTAMP_GRANULARITY;
    }

    /**
     * test whether a file or directory exists, with an error in the
     * upper/lower case spelling of the name.
     * Using this method is only interesting on case insensitive file systems
     * (Windows).
     * <p>
     * It will return true only if 3 conditions are met:
     * </p>
     * <ul>
     *   <li>operating system is case insensitive</li>
     *   <li>file exists</li>
     *   <li>actual name from directory reading is different from the
     *       supplied argument</li>
     * </ul>
     * <p>
     * The purpose is to identify files or directories on case-insensitive
     * filesystems whose case is not what is expected.
     * </p>
     * Possibly to rename them afterwards to the desired upper/lowercase
     * combination.
     *
     * @param localFile file to test
     * @return true if the file exists and the case of the actual file
     *              is not the case of the parameter
     * @since Ant 1.7.1
     */
    public boolean hasErrorInCase(File localFile) {
        localFile = normalize(localFile.getAbsolutePath());
        if (!localFile.exists()) {
            return false;
        }
        final String localFileName = localFile.getName();
        FilenameFilter ff = (dir, name) -> name.equalsIgnoreCase(localFileName)
            && (!name.equals(localFileName));
        String[] names = localFile.getParentFile().list(ff);
        return names != null && names.length == 1;
    }

    /**
     * Returns true if the source is older than the dest.
     * If the dest file does not exist, then the test returns false; it is
     * implicitly not up do date.
     * @param source source file (should be the older).
     * @param dest dest file (should be the newer).
     * @param granularity an offset added to the source time.
     * @return true if the source is older than the dest after accounting
     *              for granularity.
     * @since Ant 1.6.3
     */
    public boolean isUpToDate(File source, File dest, long granularity) {
        //do a check for the destination file existing
        if (!dest.exists()) {
            //if it does not, then the file is not up to date.
            return false;
        }
        long sourceTime = source.lastModified();
        long destTime = dest.lastModified();
        return isUpToDate(sourceTime, destTime, granularity);
    }

    /**
     * Returns true if the source is older than the dest.
     * @param source source file (should be the older).
     * @param dest dest file (should be the newer).
     * @return true if the source is older than the dest, taking the granularity into account.
     * @since Ant 1.6.3
     */
    public boolean isUpToDate(File source, File dest) {
        return isUpToDate(source, dest, getFileTimestampGranularity());
    }

    /**
     * Compare two timestamps for being up to date using
     * the specified granularity.
     *
     * @param sourceTime timestamp of source file.
     * @param destTime timestamp of dest file.
     * @param granularity os/filesys granularity.
     * @return true if the dest file is considered up to date.
     */
    public boolean isUpToDate(long sourceTime, long destTime, long granularity) {
        return destTime != -1 && destTime >= sourceTime + granularity;
    }

    /**
     * Compare two timestamps for being up to date using the
     * current granularity.
     *
     * @param sourceTime  timestamp of source file.
     * @param destTime    timestamp of dest file.
     * @return true if the dest file is considered up to date.
     */
    public boolean isUpToDate(long sourceTime, long destTime) {
        return isUpToDate(sourceTime, destTime, getFileTimestampGranularity());
    }

    /**
     * Close a Writer without throwing any exception if something went wrong.
     * Do not attempt to close it if the argument is null.
     * @param device output writer, can be null.
     */
    public static void close(Writer device) {
        close((AutoCloseable) device);
    }

    /**
     * Close a Reader without throwing any exception if something went wrong.
     * Do not attempt to close it if the argument is null.
     *
     * @param device Reader, can be null.
     */
    public static void close(Reader device) {
        close((AutoCloseable) device);
    }

    /**
     * Close a stream without throwing any exception if something went wrong.
     * Do not attempt to close it if the argument is null.
     *
     * @param device stream, can be null.
     */
    public static void close(OutputStream device) {
        close((AutoCloseable) device);
    }

    /**
     * Close a stream without throwing any exception if something went wrong.
     * Do not attempt to close it if the argument is null.
     *
     * @param device stream, can be null.
     */
    public static void close(InputStream device) {
        close((AutoCloseable) device);
    }

    /**
     * Close a Channel without throwing any exception if something went wrong.
     * Do not attempt to close it if the argument is null.
     *
     * @param device channel, can be null.
     * @since Ant 1.8.0
     */
    public static void close(Channel device) {
        close((AutoCloseable) device);
    }

    /**
     * Closes an URLConnection if its concrete implementation provides
     * a way to close it that Ant knows of.
     *
     * @param conn connection, can be null
     * @since Ant 1.8.0
     */
    public static void close(URLConnection conn) {
        if (conn != null) {
            try {
                if (conn instanceof JarURLConnection) {
                    JarURLConnection juc = (JarURLConnection) conn;
                    JarFile jf = juc.getJarFile();
                    jf.close();
                } else if (conn instanceof HttpURLConnection) {
                    ((HttpURLConnection) conn).disconnect();
                }
            } catch (IOException exc) {
                //ignore
            }
        }
    }

    /**
     * Close an {@link AutoCloseable} without throwing any exception
     * if something went wrong.  Do not attempt to close it if the
     * argument is null.
     *
     * @param ac AutoCloseable, can be null.
     * @since Ant 1.10.0
     */
    public static void close(AutoCloseable ac) {
        if (null != ac) {
            try {
                ac.close();
            } catch (Exception e) {
                //ignore
            }
        }
    }

    /**
     * Delete the file with {@link File#delete()} if the argument is not null.
     * Do nothing on a null argument.
     * @param file file to delete.
     */
    public static void delete(File file) {
        if (file != null) {
            file.delete();
        }
    }

    /**
     * Accommodate Windows bug encountered in both Sun and IBM JDKs.
     * Others possible. If the delete does not work, call System.gc(),
     * wait a little and try again.
     *
     * @param f File
     * @return whether deletion was successful
     * @since Ant 1.8.0
     */
    public boolean tryHardToDelete(File f) {
        return tryHardToDelete(f, ON_WINDOWS);
    }

    /**
     * If delete does not work, call System.gc() if asked to, wait a
     * little and try again.
     *
     * @param f File
     * @param runGC boolean
     * @return whether deletion was successful
     * @since Ant 1.8.3
     */
    public boolean tryHardToDelete(File f, boolean runGC) {
        if (!f.delete()) {
            if (runGC) {
                System.gc();
            }
            try {
                Thread.sleep(DELETE_RETRY_SLEEP_MILLIS);
            } catch (InterruptedException ex) {
                // Ignore Exception
            }
            return f.delete();
        }
        return true;
    }

    /**
     * Calculates the relative path between two files.
     * <p>
     * Implementation note:<br>This function may throw an IOException if an I/O error occurs
     * because its use of the canonical pathname may require filesystem queries.
     * </p>
     *
     * @param fromFile the <code>File</code> to calculate the path from
     * @param toFile the <code>File</code> to calculate the path to
     * @return the relative path between the files
     * @throws Exception for undocumented reasons
     * @see File#getCanonicalPath()
     *
     * @since Ant 1.7
     */
    public static String getRelativePath(File fromFile, File toFile) throws Exception { //NOSONAR
        String fromPath = fromFile.getCanonicalPath();
        String toPath = toFile.getCanonicalPath();

        // build the path stack info to compare
        String[] fromPathStack = getPathStack(fromPath);
        String[] toPathStack = getPathStack(toPath);

        if (0 < toPathStack.length && 0 < fromPathStack.length) {
            if (!fromPathStack[0].equals(toPathStack[0])) {
                // not the same device (would be "" on Linux/Unix)
                return getPath(Arrays.asList(toPathStack));
            }
        } else {
            // no comparison possible
            return getPath(Arrays.asList(toPathStack));
        }

        // get index of parts which are equal
        int minLength = Math.min(fromPathStack.length, toPathStack.length);
        int same = 1;
        while (same < minLength && fromPathStack[same].equals(toPathStack[same])) {
            same++;
        }

        List<String> relativePathStack = new ArrayList<>();

        // if "from" part is longer, fill it up with ".."
        // to reach path which is equal to both paths
        for (int i = same; i < fromPathStack.length; i++) {
            relativePathStack.add("..");
        }

        // fill it up path with parts which were not equal
        relativePathStack.addAll(Arrays.asList(toPathStack).subList(same, toPathStack.length));

        return getPath(relativePathStack);
    }

    /**
     * Gets all names of the path as an array of <code>String</code>s.
     *
     * @param path to get names from
     * @return <code>String</code>s, never <code>null</code>
     *
     * @since Ant 1.7
     */
    public static String[] getPathStack(String path) {
        String normalizedPath = path.replace(File.separatorChar, '/');

        return normalizedPath.split("/");
    }

    /**
     * Gets path from a <code>List</code> of <code>String</code>s.
     *
     * @param pathStack <code>List</code> of <code>String</code>s to be concatenated as a path.
     * @return <code>String</code>, never <code>null</code>
     *
     * @since Ant 1.7
     */
    public static String getPath(List<String> pathStack) {
        // can safely use '/' because Windows understands '/' as separator
        return getPath(pathStack, '/');
    }

    /**
     * Gets path from a <code>List</code> of <code>String</code>s.
     *
     * @param pathStack <code>List</code> of <code>String</code>s to be concatenated as a path.
     * @param separatorChar <code>char</code> to be used as separator between names in path
     * @return <code>String</code>, never <code>null</code>
     *
     * @since Ant 1.7
     */
    public static String getPath(final List<String> pathStack, final char separatorChar) {
        return pathStack.stream().collect(Collectors.joining(Character.toString(separatorChar)));
    }

    /**
     * Get the default encoding.
     * This is done by opening an InputStreamReader on
     * a dummy InputStream and getting the encoding.
     * Could use System.getProperty("file.encoding"), but cannot
     * see where this is documented.
     * @return the default file encoding.
     */
    public String getDefaultEncoding() {
        InputStreamReader is = new InputStreamReader(
            new InputStream() { //NOSONAR
                @Override
                public int read() {
                    return -1;
                }
            });
        try {
            return is.getEncoding();
        } finally {
            close(is);
        }
    }

    /**
     * Opens a new OutputStream for the given Path.
     * @param path the path of the file
     * @param append whether to append to or a replace an existing file
     * @return a stream ready to write to the file
     * @throws IOException if stream creation fails
     * @since Ant 1.10.2
     */
    public static OutputStream newOutputStream(Path path, boolean append) throws IOException {
        if (append) {
            return Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND,
                StandardOpenOption.WRITE);
        } else {
            return Files.newOutputStream(path);
        }
    }

    /**
     * Tries to determine the case sensitivity of the filesystem corresponding to the
     * {@code path}. While doing so, this method might create a temporary file under
     * the directory represented by the {@code path}, if it's a directory or in the
     * parent directory of the {@code path}, if it's a file.
     * <p>
     * This method works on a best effort basis and will return an {@link Optional#empty()}
     * if it cannot determine the case sensitivity, either due to exception or for any other
     * reason.
     * </p>
     * @param path The path whose filesystem case sensitivity needs to be checked
     * @return Returns true if the filesystem corresponding to the passed {@code path}
     *          is case sensitive. Else returns false. If the case sensitivity
     *          cannot be determined for whatever reason, this method returns an
     *          {@link Optional#empty()}
     * @throws IllegalArgumentException If the passed path is null
     * @since Ant 1.10.6
     */
    public static Optional<Boolean> isCaseSensitiveFileSystem(final Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        final String mixedCaseFileNamePrefix = "aNt";
        Path mixedCaseTmpFile = null;
        boolean caseSensitive;
        try {
            if (Files.isRegularFile(path)) {
                mixedCaseTmpFile = Files.createTempFile(path.getParent(), mixedCaseFileNamePrefix, null);
            } else if (Files.isDirectory(path)) {
                mixedCaseTmpFile = Files.createTempFile(path, mixedCaseFileNamePrefix, null);
            } else {
                // we can only do our tricks to figure out whether the filesystem is
                // case sensitive, only if the path is a directory or a file.
                // In other cases (like the path being non-existent), we don't
                // have a way to determine that detail
                return Optional.empty();
            }
            final Path lowerCasePath = Paths.get(mixedCaseTmpFile.toString().toLowerCase(Locale.US));
            try {
                caseSensitive = !Files.isSameFile(mixedCaseTmpFile, lowerCasePath);
            } catch (NoSuchFileException nsfe) {
                // a NSFE implies that the "lowerCasePath" file wasn't considered to be present
                // even if the different cased file exists. That effectively means this is a
                // case sensitive filesystem
                caseSensitive = true;
            }
        } catch (IOException ioe) {
            System.err.println("Could not determine the case sensitivity of the " +
                    "filesystem for path " + path + " due to " + ioe);
            return Optional.empty();
        } finally {
            // delete the tmp file
            if (mixedCaseTmpFile != null) {
                FileUtils.delete(mixedCaseTmpFile.toFile());
            }
        }
        return Optional.of(caseSensitive);
    }
}
