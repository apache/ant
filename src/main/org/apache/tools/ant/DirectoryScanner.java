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

package org.apache.tools.ant;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceFactory;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.selectors.FileSelector;
import org.apache.tools.ant.types.selectors.SelectorScanner;
import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.apache.tools.ant.types.selectors.TokenizedPath;
import org.apache.tools.ant.types.selectors.TokenizedPattern;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.VectorSet;

/**
 * Class for scanning a directory for files/directories which match certain
 * criteria.
 * <p>
 * These criteria consist of selectors and patterns which have been specified.
 * With the selectors you can select which files you want to have included.
 * Files which are not selected are excluded. With patterns you can include
 * or exclude files based on their filename.
 * </p>
 * <p>
 * The idea is simple. A given directory is recursively scanned for all files
 * and directories. Each file/directory is matched against a set of selectors,
 * including special support for matching against filenames with include and
 * and exclude patterns. Only files/directories which match at least one
 * pattern of the include pattern list or other file selector, and don't match
 * any pattern of the exclude pattern list or fail to match against a required
 * selector will be placed in the list of files/directories found.
 * </p>
 * <p>
 * When no list of include patterns is supplied, "**" will be used, which
 * means that everything will be matched. When no list of exclude patterns is
 * supplied, an empty list is used, such that nothing will be excluded. When
 * no selectors are supplied, none are applied.
 * </p>
 * <p>
 * The filename pattern matching is done as follows:
 * The name to be matched is split up in path segments. A path segment is the
 * name of a directory or file, which is bounded by
 * <code>File.separator</code> ('/' under UNIX, '\' under Windows).
 * For example, "abc/def/ghi/xyz.java" is split up in the segments "abc",
 * "def","ghi" and "xyz.java".
 * The same is done for the pattern against which should be matched.
 * </p>
 * <p>
 * The segments of the name and the pattern are then matched against each
 * other. When '**' is used for a path segment in the pattern, it matches
 * zero or more path segments of the name.
 * </p>
 * <p>
 * There is a special case regarding the use of <code>File.separator</code>s
 * at the beginning of the pattern and the string to match:
 * </p>
 * <ul>
 * <li>When a pattern starts with a <code>File.separator</code>, the string
 * to match must also start with a <code>File.separator</code>.</li>
 * <li>When a pattern does not start with a <code>File.separator</code>, the
 * string to match may not start with a <code>File.separator</code>.</li>
 * <li>When one of the above rules is not obeyed, the string will not
 * match.</li>
 * </ul>
 * <p>
 * When a name path segment is matched against a pattern path segment, the
 * following special characters can be used:<br>
 * '*' matches zero or more characters<br>
 * '?' matches one character.
 * </p>
 * <p>
 * Examples:
 * </p>
 * <p>
 * "**\*.class" matches all .class files/dirs in a directory tree.
 * </p>
 * <p>
 * "test\a??.java" matches all files/dirs which start with an 'a', then two
 * more characters and then ".java", in a directory called test.
 * </p>
 * <p>
 * "**" matches everything in a directory tree.
 * </p>
 * <p>
 * "**\test\**\XYZ*" matches all files/dirs which start with "XYZ" and where
 * there is a parent directory called test (e.g. "abc\test\def\ghi\XYZ123").
 * </p>
 * <p>
 * Case sensitivity may be turned off if necessary. By default, it is
 * turned on.
 * </p>
 * <p>
 * Example of usage:
 * </p>
 * <pre>
 *   String[] includes = {"**\\*.class"};
 *   String[] excludes = {"modules\\*\\**"};
 *   ds.setIncludes(includes);
 *   ds.setExcludes(excludes);
 *   ds.setBasedir(new File("test"));
 *   ds.setCaseSensitive(true);
 *   ds.scan();
 *
 *   System.out.println("FILES:");
 *   String[] files = ds.getIncludedFiles();
 *   for (int i = 0; i &lt; files.length; i++) {
 *     System.out.println(files[i]);
 *   }
 * </pre>
 * This will scan a directory called test for .class files, but excludes all
 * files in all proper subdirectories of a directory called "modules".
 *
 */
public class DirectoryScanner
       implements FileScanner, SelectorScanner, ResourceFactory {

    /** Is OpenVMS the operating system we're running on? */
    private static final boolean ON_VMS = Os.isFamily("openvms");

    /**
     * Patterns which should be excluded by default.
     *
     * <p>Note that you can now add patterns to the list of default
     * excludes.  Added patterns will not become part of this array
     * that has only been kept around for backwards compatibility
     * reasons.</p>
     *
     * @deprecated since 1.6.x.
     *             Use the {@link #getDefaultExcludes getDefaultExcludes}
     *             method instead.
     */
    @Deprecated
    protected static final String[] DEFAULTEXCLUDES = { //NOSONAR
        // Miscellaneous typical temporary files
        SelectorUtils.DEEP_TREE_MATCH + "/*~",
        SelectorUtils.DEEP_TREE_MATCH + "/#*#",
        SelectorUtils.DEEP_TREE_MATCH + "/.#*",
        SelectorUtils.DEEP_TREE_MATCH + "/%*%",
        SelectorUtils.DEEP_TREE_MATCH + "/._*",

        // CVS
        SelectorUtils.DEEP_TREE_MATCH + "/CVS",
        SelectorUtils.DEEP_TREE_MATCH + "/CVS/" + SelectorUtils.DEEP_TREE_MATCH,
        SelectorUtils.DEEP_TREE_MATCH + "/.cvsignore",

        // SCCS
        SelectorUtils.DEEP_TREE_MATCH + "/SCCS",
        SelectorUtils.DEEP_TREE_MATCH + "/SCCS/" + SelectorUtils.DEEP_TREE_MATCH,

        // Visual SourceSafe
        SelectorUtils.DEEP_TREE_MATCH + "/vssver.scc",

        // Subversion
        SelectorUtils.DEEP_TREE_MATCH + "/.svn",
        SelectorUtils.DEEP_TREE_MATCH + "/.svn/" + SelectorUtils.DEEP_TREE_MATCH,

        // Git
        SelectorUtils.DEEP_TREE_MATCH + "/.git",
        SelectorUtils.DEEP_TREE_MATCH + "/.git/" + SelectorUtils.DEEP_TREE_MATCH,
        SelectorUtils.DEEP_TREE_MATCH + "/.gitattributes",
        SelectorUtils.DEEP_TREE_MATCH + "/.gitignore",
        SelectorUtils.DEEP_TREE_MATCH + "/.gitmodules",

        // Mercurial
        SelectorUtils.DEEP_TREE_MATCH + "/.hg",
        SelectorUtils.DEEP_TREE_MATCH + "/.hg/" + SelectorUtils.DEEP_TREE_MATCH,
        SelectorUtils.DEEP_TREE_MATCH + "/.hgignore",
        SelectorUtils.DEEP_TREE_MATCH + "/.hgsub",
        SelectorUtils.DEEP_TREE_MATCH + "/.hgsubstate",
        SelectorUtils.DEEP_TREE_MATCH + "/.hgtags",

        // Bazaar
        SelectorUtils.DEEP_TREE_MATCH + "/.bzr",
        SelectorUtils.DEEP_TREE_MATCH + "/.bzr/" + SelectorUtils.DEEP_TREE_MATCH,
        SelectorUtils.DEEP_TREE_MATCH + "/.bzrignore",

        // Mac
        SelectorUtils.DEEP_TREE_MATCH + "/.DS_Store"
    };

    /**
     * default value for {@link #maxLevelsOfSymlinks maxLevelsOfSymlinks}
     * @since Ant 1.8.0
     */
    public static final int MAX_LEVELS_OF_SYMLINKS = 5;
    /**
     * The end of the exception message if something that should be
     * there doesn't exist.
     */
    public static final String DOES_NOT_EXIST_POSTFIX = " does not exist.";

    /** Helper. */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /**
     * Patterns which should be excluded by default.
     *
     * @see #addDefaultExcludes()
     */
    private static final Set<String> defaultExcludes = new HashSet<>();
    static {
        resetDefaultExcludes();
    }

    // CheckStyle:VisibilityModifier OFF - bc

    /** The base directory to be scanned. */
    protected File basedir;

    /** The patterns for the files to be included. */
    protected String[] includes;

    /** The patterns for the files to be excluded. */
    protected String[] excludes;

    /** Selectors that will filter which files are in our candidate list. */
    protected FileSelector[] selectors = null;

    /**
     * The files which matched at least one include and no excludes
     * and were selected.
     */
    protected Vector<String> filesIncluded;

    /** The files which did not match any includes or selectors. */
    protected Vector<String> filesNotIncluded;

    /**
     * The files which matched at least one include and at least
     * one exclude.
     */
    protected Vector<String> filesExcluded;

    /**
     * The directories which matched at least one include and no excludes
     * and were selected.
     */
    protected Vector<String> dirsIncluded;

    /** The directories which were found and did not match any includes. */
    protected Vector<String> dirsNotIncluded;

    /**
     * The directories which matched at least one include and at least one
     * exclude.
     */
    protected Vector<String> dirsExcluded;

    /**
     * The files which matched at least one include and no excludes and
     * which a selector discarded.
     */
    protected Vector<String> filesDeselected;

    /**
     * The directories which matched at least one include and no excludes
     * but which a selector discarded.
     */
    protected Vector<String> dirsDeselected;

    /** Whether or not our results were built by a slow scan. */
    protected boolean haveSlowResults = false;

    /**
     * Whether or not the file system should be treated as a case sensitive
     * one.
     */
    protected boolean isCaseSensitive = true;

    /**
     * Whether a missing base directory is an error.
     * @since Ant 1.7.1
     */
    protected boolean errorOnMissingDir = true;

    /**
     * Whether or not symbolic links should be followed.
     *
     * @since Ant 1.5
     */
    private boolean followSymlinks = true;

    /** Whether or not everything tested so far has been included. */
    protected boolean everythingIncluded = true;

    // CheckStyle:VisibilityModifier ON

    /**
     * List of all scanned directories.
     *
     * @since Ant 1.6
     */
    private final Set<String> scannedDirs = new HashSet<>();

    /**
     * Map of all include patterns that are full file names and don't
     * contain any wildcards.
     *
     * <p>Maps pattern string to TokenizedPath.</p>
     *
     * <p>If this instance is not case sensitive, the file names get
     * turned to upper case.</p>
     *
     * <p>Gets lazily initialized on the first invocation of
     * isIncluded or isExcluded and cleared at the end of the scan
     * method (cleared in clearCaches, actually).</p>
     *
     * @since Ant 1.8.0
     */
    private final Map<String, TokenizedPath> includeNonPatterns = new HashMap<>();

    /**
     * Map of all exclude patterns that are full file names and don't
     * contain any wildcards.
     *
     * <p>Maps pattern string to TokenizedPath.</p>
     *
     * <p>If this instance is not case sensitive, the file names get
     * turned to upper case.</p>
     *
     * <p>Gets lazily initialized on the first invocation of
     * isIncluded or isExcluded and cleared at the end of the scan
     * method (cleared in clearCaches, actually).</p>
     *
     * @since Ant 1.8.0
     */
    private final Map<String, TokenizedPath> excludeNonPatterns = new HashMap<>();

    /**
     * Array of all include patterns that contain wildcards.
     *
     * <p>Gets lazily initialized on the first invocation of
     * isIncluded or isExcluded and cleared at the end of the scan
     * method (cleared in clearCaches, actually).</p>
     */
    private TokenizedPattern[] includePatterns;

    /**
     * Array of all exclude patterns that contain wildcards.
     *
     * <p>Gets lazily initialized on the first invocation of
     * isIncluded or isExcluded and cleared at the end of the scan
     * method (cleared in clearCaches, actually).</p>
     */
    private TokenizedPattern[] excludePatterns;

    /**
     * Have the non-pattern sets and pattern arrays for in- and
     * excludes been initialized?
     *
     * @since Ant 1.6.3
     */
    private boolean areNonPatternSetsReady = false;

    /**
     * Scanning flag.
     *
     * @since Ant 1.6.3
     */
    private boolean scanning = false;

    /**
     * Scanning lock.
     *
     * @since Ant 1.6.3
     */
    private final Object scanLock = new Object();

    /**
     * Slow scanning flag.
     *
     * @since Ant 1.6.3
     */
    private boolean slowScanning = false;

    /**
     * Slow scanning lock.
     *
     * @since Ant 1.6.3
     */
    private final Object slowScanLock = new Object();

    /**
     * Exception thrown during scan.
     *
     * @since Ant 1.6.3
     */
    private IllegalStateException illegal = null;

    /**
     * The maximum number of times a symbolic link may be followed
     * during a scan.
     *
     * @since Ant 1.8.0
     */
    private int maxLevelsOfSymlinks = MAX_LEVELS_OF_SYMLINKS;


    /**
     * Absolute paths of all symlinks that haven't been followed but
     * would have been if followsymlinks had been true or
     * maxLevelsOfSymlinks had been higher.
     *
     * @since Ant 1.8.0
     */
    private final Set<String> notFollowedSymlinks = new HashSet<>();

    /**
     * Test whether or not a given path matches the start of a given
     * pattern up to the first "**".
     * <p>
     * This is not a general purpose test and should only be used if you
     * can live with false positives. For example, <code>pattern=**\a</code>
     * and <code>str=b</code> will yield <code>true</code>.
     *
     * @param pattern The pattern to match against. Must not be
     *                <code>null</code>.
     * @param str     The path to match, as a String. Must not be
     *                <code>null</code>.
     *
     * @return whether or not a given path matches the start of a given
     * pattern up to the first "**".
     */
    protected static boolean matchPatternStart(final String pattern, final String str) {
        return SelectorUtils.matchPatternStart(pattern, str);
    }

    /**
     * Test whether or not a given path matches the start of a given
     * pattern up to the first "**".
     * <p>
     * This is not a general purpose test and should only be used if you
     * can live with false positives. For example, <code>pattern=**\a</code>
     * and <code>str=b</code> will yield <code>true</code>.
     *
     * @param pattern The pattern to match against. Must not be
     *                <code>null</code>.
     * @param str     The path to match, as a String. Must not be
     *                <code>null</code>.
     * @param isCaseSensitive Whether or not matching should be performed
     *                        case sensitively.
     *
     * @return whether or not a given path matches the start of a given
     * pattern up to the first "**".
     */
    protected static boolean matchPatternStart(final String pattern, final String str,
                                               final boolean isCaseSensitive) {
        return SelectorUtils.matchPatternStart(pattern, str, isCaseSensitive);
    }

    /**
     * Test whether or not a given path matches a given pattern.
     *
     * @param pattern The pattern to match against. Must not be
     *                <code>null</code>.
     * @param str     The path to match, as a String. Must not be
     *                <code>null</code>.
     *
     * @return <code>true</code> if the pattern matches against the string,
     *         or <code>false</code> otherwise.
     */
    protected static boolean matchPath(final String pattern, final String str) {
        return SelectorUtils.matchPath(pattern, str);
    }

    /**
     * Test whether or not a given path matches a given pattern.
     *
     * @param pattern The pattern to match against. Must not be
     *                <code>null</code>.
     * @param str     The path to match, as a String. Must not be
     *                <code>null</code>.
     * @param isCaseSensitive Whether or not matching should be performed
     *                        case sensitively.
     *
     * @return <code>true</code> if the pattern matches against the string,
     *         or <code>false</code> otherwise.
     */
    protected static boolean matchPath(final String pattern, final String str,
                                       final boolean isCaseSensitive) {
        return SelectorUtils.matchPath(pattern, str, isCaseSensitive);
    }

    /**
     * Test whether or not a string matches against a pattern.
     * The pattern may contain two special characters:<br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     *
     * @param pattern The pattern to match against.
     *                Must not be <code>null</code>.
     * @param str     The string which must be matched against the pattern.
     *                Must not be <code>null</code>.
     *
     * @return <code>true</code> if the string matches against the pattern,
     *         or <code>false</code> otherwise.
     */
    public static boolean match(final String pattern, final String str) {
        return SelectorUtils.match(pattern, str);
    }

    /**
     * Test whether or not a string matches against a pattern.
     * The pattern may contain two special characters:<br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     *
     * @param pattern The pattern to match against.
     *                Must not be <code>null</code>.
     * @param str     The string which must be matched against the pattern.
     *                Must not be <code>null</code>.
     * @param isCaseSensitive Whether or not matching should be performed
     *                        case sensitively.
     *
     *
     * @return <code>true</code> if the string matches against the pattern,
     *         or <code>false</code> otherwise.
     */
    protected static boolean match(final String pattern, final String str,
                                   final boolean isCaseSensitive) {
        return SelectorUtils.match(pattern, str, isCaseSensitive);
    }


    /**
     * Get the list of patterns that should be excluded by default.
     *
     * @return An array of <code>String</code> based on the current
     *         contents of the <code>defaultExcludes</code>
     *         <code>Set</code>.
     *
     * @since Ant 1.6
     */
    public static String[] getDefaultExcludes() {
        synchronized (defaultExcludes) {
            return defaultExcludes.toArray(new String[0]);
        }
    }

    /**
     * Add a pattern to the default excludes unless it is already a
     * default exclude.
     *
     * @param s   A string to add as an exclude pattern.
     * @return    <code>true</code> if the string was added;
     *            <code>false</code> if it already existed.
     *
     * @since Ant 1.6
     */
    public static boolean addDefaultExclude(final String s) {
        synchronized (defaultExcludes) {
            return defaultExcludes.add(s);
        }
    }

    /**
     * Remove a string if it is a default exclude.
     *
     * @param s   The string to attempt to remove.
     * @return    <code>true</code> if <code>s</code> was a default
     *            exclude (and thus was removed);
     *            <code>false</code> if <code>s</code> was not
     *            in the default excludes list to begin with.
     *
     * @since Ant 1.6
     */
    public static boolean removeDefaultExclude(final String s) {
        synchronized (defaultExcludes) {
            return defaultExcludes.remove(s);
        }
    }

    /**
     * Go back to the hardwired default exclude patterns.
     *
     * @since Ant 1.6
     */
    public static void resetDefaultExcludes() {
        synchronized (defaultExcludes) {
            defaultExcludes.clear();
            Collections.addAll(defaultExcludes, DEFAULTEXCLUDES);
        }
    }

    /**
     * Set the base directory to be scanned. This is the directory which is
     * scanned recursively. All '/' and '\' characters are replaced by
     * <code>File.separatorChar</code>, so the separator used need not match
     * <code>File.separatorChar</code>.
     *
     * @param basedir The base directory to scan.
     */
    @Override
    public void setBasedir(final String basedir) {
        setBasedir(basedir == null ? null
            : new File(basedir.replace('/', File.separatorChar).replace(
            '\\', File.separatorChar)));
    }

    /**
     * Set the base directory to be scanned. This is the directory which is
     * scanned recursively.
     *
     * @param basedir The base directory for scanning.
     */
    @Override
    public synchronized void setBasedir(final File basedir) {
        this.basedir = basedir;
    }

    /**
     * Return the base directory to be scanned.
     * This is the directory which is scanned recursively.
     *
     * @return the base directory to be scanned.
     */
    @Override
    public synchronized File getBasedir() {
        return basedir;
    }

    /**
     * Find out whether include exclude patterns are matched in a
     * case sensitive way.
     * @return whether or not the scanning is case sensitive.
     * @since Ant 1.6
     */
    public synchronized boolean isCaseSensitive() {
        return isCaseSensitive;
    }

    /**
     * Set whether or not include and exclude patterns are matched
     * in a case sensitive way.
     *
     * @param isCaseSensitive whether or not the file system should be
     *                        regarded as a case sensitive one.
     */
    @Override
    public synchronized void setCaseSensitive(final boolean isCaseSensitive) {
        this.isCaseSensitive = isCaseSensitive;
    }

    /**
     * Sets whether or not a missing base directory is an error
     *
     * @param errorOnMissingDir whether or not a missing base directory
     *                        is an error
     * @since Ant 1.7.1
     */
    public void setErrorOnMissingDir(final boolean errorOnMissingDir) {
        this.errorOnMissingDir = errorOnMissingDir;
    }

    /**
     * Get whether or not a DirectoryScanner follows symbolic links.
     *
     * @return flag indicating whether symbolic links should be followed.
     *
     * @since Ant 1.6
     */
    public synchronized boolean isFollowSymlinks() {
        return followSymlinks;
    }

    /**
     * Set whether or not symbolic links should be followed.
     *
     * @param followSymlinks whether or not symbolic links should be followed.
     */
    public synchronized void setFollowSymlinks(final boolean followSymlinks) {
        this.followSymlinks = followSymlinks;
    }

    /**
     * The maximum number of times a symbolic link may be followed
     * during a scan.
     *
     * @param max int
     * @since Ant 1.8.0
     */
    public void setMaxLevelsOfSymlinks(final int max) {
        maxLevelsOfSymlinks = max;
    }

    /**
     * Set the list of include patterns to use. All '/' and '\' characters
     * are replaced by <code>File.separatorChar</code>, so the separator used
     * need not match <code>File.separatorChar</code>.
     * <p>
     * When a pattern ends with a '/' or '\', "**" is appended.
     *
     * @param includes A list of include patterns.
     *                 May be <code>null</code>, indicating that all files
     *                 should be included. If a non-<code>null</code>
     *                 list is given, all elements must be
     *                 non-<code>null</code>.
     */
    @Override
    public synchronized void setIncludes(final String[] includes) {
        if (includes == null) {
            this.includes = null;
        } else {
            this.includes = Stream.of(includes)
                .map(DirectoryScanner::normalizePattern).toArray(String[]::new);
        }
    }

    /**
     * Set the list of exclude patterns to use. All '/' and '\' characters
     * are replaced by <code>File.separatorChar</code>, so the separator used
     * need not match <code>File.separatorChar</code>.
     * <p>
     * When a pattern ends with a '/' or '\', "**" is appended.
     *
     * @param excludes A list of exclude patterns.
     *                 May be <code>null</code>, indicating that no files
     *                 should be excluded. If a non-<code>null</code> list is
     *                 given, all elements must be non-<code>null</code>.
     */
    @Override
    public synchronized void setExcludes(final String[] excludes) {
        if (excludes == null) {
            this.excludes = null;
        } else {
            this.excludes = Stream.of(excludes).map(DirectoryScanner::normalizePattern)
                    .toArray(String[]::new);
        }
    }

    /**
     * Add to the list of exclude patterns to use. All '/' and '\'
     * characters are replaced by <code>File.separatorChar</code>, so
     * the separator used need not match <code>File.separatorChar</code>.
     * <p>
     * When a pattern ends with a '/' or '\', "**" is appended.
     *
     * @param excludes A list of exclude patterns.
     *                 May be <code>null</code>, in which case the
     *                 exclude patterns don't get changed at all.
     *
     * @since Ant 1.6.3
     */
    public synchronized void addExcludes(final String[] excludes) {
        if (excludes != null && excludes.length > 0) {
            if (this.excludes == null || this.excludes.length == 0) {
                setExcludes(excludes);
            } else {
                this.excludes = Stream.concat(Stream.of(this.excludes),
                        Stream.of(excludes).map(DirectoryScanner::normalizePattern))
                        .toArray(String[]::new);
            }
        }
    }

    /**
     * All '/' and '\' characters are replaced by
     * <code>File.separatorChar</code>, so the separator used need not
     * match <code>File.separatorChar</code>.
     *
     * <p>When a pattern ends with a '/' or '\', "**" is appended.</p>
     *
     * @since Ant 1.6.3
     */
    private static String normalizePattern(final String p) {
        String pattern = p.replace('/', File.separatorChar)
            .replace('\\', File.separatorChar);
        if (pattern.endsWith(File.separator)) {
            pattern += SelectorUtils.DEEP_TREE_MATCH;
        }
        return pattern;
    }

    /**
     * Set the selectors that will select the filelist.
     *
     * @param selectors specifies the selectors to be invoked on a scan.
     */
    @Override
    public synchronized void setSelectors(final FileSelector[] selectors) {
        this.selectors = selectors;
    }

    /**
     * Return whether or not the scanner has included all the files or
     * directories it has come across so far.
     *
     * @return <code>true</code> if all files and directories which have
     *         been found so far have been included.
     */
    public synchronized boolean isEverythingIncluded() {
        return everythingIncluded;
    }

    /**
     * Scan for files which match at least one include pattern and don't match
     * any exclude patterns. If there are selectors then the files must pass
     * muster there, as well.  Scans under basedir, if set; otherwise the
     * include patterns without leading wildcards specify the absolute paths of
     * the files that may be included.
     *
     * @exception IllegalStateException if the base directory was set
     *            incorrectly (i.e. if it doesn't exist or isn't a directory).
     */
    @Override
    public void scan() throws IllegalStateException {
        synchronized (scanLock) {
            if (scanning) {
                while (scanning) {
                    try {
                        scanLock.wait();
                    } catch (final InterruptedException ignored) {
                    }
                }
                if (illegal != null) {
                    throw illegal;
                }
                return;
            }
            scanning = true;
        }
        final File savedBase = basedir;
        try {
            synchronized (this) {
                illegal = null;
                clearResults();

                // set in/excludes to reasonable defaults if needed:
                final boolean nullIncludes = includes == null;
                includes = nullIncludes ? new String[] {SelectorUtils.DEEP_TREE_MATCH} : includes;
                final boolean nullExcludes = excludes == null;
                excludes = nullExcludes ? new String[0] : excludes;

                if (basedir != null && !followSymlinks
                    && Files.isSymbolicLink(basedir.toPath())) {
                    notFollowedSymlinks.add(basedir.getAbsolutePath());
                    basedir = null;
                }

                if (basedir == null) {
                    // if no basedir and no includes, nothing to do:
                    if (nullIncludes) {
                        return;
                    }
                } else {
                    if (!basedir.exists()) {
                        if (errorOnMissingDir) {
                            illegal = new IllegalStateException("basedir "
                                                                + basedir
                                                                + DOES_NOT_EXIST_POSTFIX);
                        } else {
                            // Nothing to do - basedir does not exist
                            return;
                        }
                    } else if (!basedir.isDirectory()) {
                        illegal = new IllegalStateException("basedir "
                                                            + basedir
                                                            + " is not a directory.");
                    }
                    if (illegal != null) {
                        throw illegal;
                    }
                }
                if (isIncluded(TokenizedPath.EMPTY_PATH)) {
                    if (isExcluded(TokenizedPath.EMPTY_PATH)) {
                        dirsExcluded.addElement("");
                    } else if (isSelected("", basedir)) {
                        dirsIncluded.addElement("");
                    } else {
                        dirsDeselected.addElement("");
                    }
                } else {
                    dirsNotIncluded.addElement("");
                }
                checkIncludePatterns();
                clearCaches();
                includes = nullIncludes ? null : includes;
                excludes = nullExcludes ? null : excludes;
            }
        } finally {
            basedir = savedBase;
            synchronized (scanLock) {
                scanning = false;
                scanLock.notifyAll();
            }
        }
    }

    /**
     * This routine is actually checking all the include patterns in
     * order to avoid scanning everything under base dir.
     * @since Ant 1.6
     */
    private void checkIncludePatterns() {
        ensureNonPatternSetsReady();
        final Map<TokenizedPath, String> newroots = new HashMap<>();

        // put in the newroots map the include patterns without
        // wildcard tokens
        for (TokenizedPattern includePattern : includePatterns) {
            final String pattern = includePattern.toString();
            if (!shouldSkipPattern(pattern)) {
                newroots.put(includePattern.rtrimWildcardTokens(), pattern);
            }
        }
        for (final Map.Entry<String, TokenizedPath> entry : includeNonPatterns
            .entrySet()) {
            final String pattern = entry.getKey();
            if (!shouldSkipPattern(pattern)) {
                newroots.put(entry.getValue(), pattern);
            }
        }

        if (newroots.containsKey(TokenizedPath.EMPTY_PATH)
            && basedir != null) {
            // we are going to scan everything anyway
            scandir(basedir, "", true);
        } else {
            File canonBase = null;
            if (basedir != null) {
                try {
                    canonBase = basedir.getCanonicalFile();
                } catch (final IOException ex) {
                    throw new BuildException(ex);
                }
            }
            // only scan directories that can include matched files or
            // directories
            for (final Map.Entry<TokenizedPath, String> entry : newroots.entrySet()) {
                TokenizedPath currentPath = entry.getKey();
                String currentelement = currentPath.toString();
                if (basedir == null && !FileUtils.isAbsolutePath(currentelement)) {
                    continue;
                }
                File myfile = new File(basedir, currentelement);

                if (myfile.exists()) {
                    // may be on a case insensitive file system.  We want
                    // the results to show what's really on the disk, so
                    // we need to double check.
                    try {
                        final String path = (basedir == null)
                            ? myfile.getCanonicalPath()
                            : FILE_UTILS.removeLeadingPath(canonBase,
                                         myfile.getCanonicalFile());
                        if (!path.equals(currentelement) || ON_VMS) {
                            myfile = currentPath.findFile(basedir, true);
                            if (myfile != null && basedir != null) {
                                currentelement = FILE_UTILS.removeLeadingPath(
                                    basedir, myfile);
                                if (!currentPath.toString().equals(currentelement)) {
                                    currentPath = new TokenizedPath(currentelement);
                                }
                            }
                        }
                    } catch (final IOException ex) {
                        throw new BuildException(ex);
                    }
                }

                if ((myfile == null || !myfile.exists()) && !isCaseSensitive()) {
                    final File f = currentPath.findFile(basedir, false);
                    if (f != null && f.exists()) {
                        // adapt currentelement to the case we've
                        // actually found
                        currentelement = (basedir == null)
                            ? f.getAbsolutePath()
                            : FILE_UTILS.removeLeadingPath(basedir, f);
                        myfile = f;
                        currentPath = new TokenizedPath(currentelement);
                    }
                }

                if (myfile != null && myfile.exists()) {
                    if (!followSymlinks && currentPath.isSymlink(basedir)) {
                        accountForNotFollowedSymlink(currentPath, myfile);
                        continue;
                    }
                    if (myfile.isDirectory()) {
                        if (isIncluded(currentPath) && !currentelement.isEmpty()) {
                            accountForIncludedDir(currentPath, myfile, true);
                        }  else {
                            scandir(myfile, currentPath, true);
                        }
                    } else if (myfile.isFile()) {
                        final String originalpattern = entry.getValue();
                        final boolean included = isCaseSensitive()
                            ? originalpattern.equals(currentelement)
                            : originalpattern.equalsIgnoreCase(currentelement);
                        if (included) {
                            accountForIncludedFile(currentPath, myfile);
                        }
                    }
                }
            }
        }
    }

    /**
     * true if the pattern specifies a relative path without basedir
     * or an absolute path not inside basedir.
     *
     * @since Ant 1.8.0
     */
    private boolean shouldSkipPattern(final String pattern) {
        if (FileUtils.isAbsolutePath(pattern)) {
            //skip abs. paths not under basedir, if set:
            return !(basedir == null || SelectorUtils.matchPatternStart(pattern,
                    basedir.getAbsolutePath(), isCaseSensitive()));
        }

        return basedir == null;
    }

    /**
     * Clear the result caches for a scan.
     */
    protected synchronized void clearResults() {
        filesIncluded    = new VectorSet<>();
        filesNotIncluded = new VectorSet<>();
        filesExcluded    = new VectorSet<>();
        filesDeselected  = new VectorSet<>();
        dirsIncluded     = new VectorSet<>();
        dirsNotIncluded  = new VectorSet<>();
        dirsExcluded     = new VectorSet<>();
        dirsDeselected   = new VectorSet<>();
        everythingIncluded = (basedir != null);
        scannedDirs.clear();
        notFollowedSymlinks.clear();
    }

    /**
     * Top level invocation for a slow scan. A slow scan builds up a full
     * list of excluded/included files/directories, whereas a fast scan
     * will only have full results for included files, as it ignores
     * directories which can't possibly hold any included files/directories.
     * <p>
     * Returns immediately if a slow scan has already been completed.
     */
    protected void slowScan() {
        synchronized (slowScanLock) {
            if (haveSlowResults) {
                return;
            }
            if (slowScanning) {
                while (slowScanning) {
                    try {
                        slowScanLock.wait();
                    } catch (final InterruptedException e) {
                        // Empty
                    }
                }
                return;
            }
            slowScanning = true;
        }
        try {
            synchronized (this) {

                // set in/excludes to reasonable defaults if needed:
                final boolean nullIncludes = (includes == null);
                includes = nullIncludes ? new String[] {SelectorUtils.DEEP_TREE_MATCH} : includes;
                final boolean nullExcludes = (excludes == null);
                excludes = nullExcludes ? new String[0] : excludes;

                final String[] excl = new String[dirsExcluded.size()];
                dirsExcluded.copyInto(excl);

                final String[] notIncl = new String[dirsNotIncluded.size()];
                dirsNotIncluded.copyInto(notIncl);

                ensureNonPatternSetsReady();

                processSlowScan(excl);
                processSlowScan(notIncl);
                clearCaches();
                includes = nullIncludes ? null : includes;
                excludes = nullExcludes ? null : excludes;
            }
        } finally {
            synchronized (slowScanLock) {
                haveSlowResults = true;
                slowScanning = false;
                slowScanLock.notifyAll();
            }
        }
    }

    private void processSlowScan(final String[] arr) {
        for (String element : arr) {
            final TokenizedPath path  = new TokenizedPath(element);
            if (!couldHoldIncluded(path) || contentsExcluded(path)) {
                scandir(new File(basedir, element), path, false);
            }
        }
    }

    /**
     * Scan the given directory for files and directories. Found files and
     * directories are placed in their respective collections, based on the
     * matching of includes, excludes, and the selectors.  When a directory
     * is found, it is scanned recursively.
     *
     * @param dir   The directory to scan. Must not be <code>null</code>.
     * @param vpath The path relative to the base directory (needed to
     *              prevent problems with an absolute path when using
     *              dir). Must not be <code>null</code>.
     * @param fast  Whether or not this call is part of a fast scan.
     *
     * @see #filesIncluded
     * @see #filesNotIncluded
     * @see #filesExcluded
     * @see #dirsIncluded
     * @see #dirsNotIncluded
     * @see #dirsExcluded
     * @see #slowScan
     */
    protected void scandir(final File dir, final String vpath, final boolean fast) {
        scandir(dir, new TokenizedPath(vpath), fast);
    }

    /**
     * Scan the given directory for files and directories. Found files and
     * directories are placed in their respective collections, based on the
     * matching of includes, excludes, and the selectors.  When a directory
     * is found, it is scanned recursively.
     *
     * @param dir   The directory to scan. Must not be <code>null</code>.
     * @param path The path relative to the base directory (needed to
     *              prevent problems with an absolute path when using
     *              dir). Must not be <code>null</code>.
     * @param fast  Whether or not this call is part of a fast scan.
     *
     * @see #filesIncluded
     * @see #filesNotIncluded
     * @see #filesExcluded
     * @see #dirsIncluded
     * @see #dirsNotIncluded
     * @see #dirsExcluded
     * @see #slowScan
     */
    private void scandir(final File dir, final TokenizedPath path, final boolean fast) {
        if (dir == null) {
            throw new BuildException("dir must not be null.");
        }
        final String[] newfiles = dir.list();
        if (newfiles == null) {
            if (!dir.exists()) {
                throw new BuildException(dir + DOES_NOT_EXIST_POSTFIX);
            } else if (!dir.isDirectory()) {
                throw new BuildException("%s is not a directory.", dir);
            } else {
                throw new BuildException("IO error scanning directory '%s'",
                    dir.getAbsolutePath());
            }
        }
        scandir(dir, path, fast, newfiles, new LinkedList<>());
    }

    private void scandir(final File dir, final TokenizedPath path, final boolean fast,
                         String[] newFiles, final Deque<String> directoryNamesFollowed) {
        String vpath = path.toString();
        if (!vpath.isEmpty() && !vpath.endsWith(File.separator)) {
            vpath += File.separator;
        }

        // avoid double scanning of directories, can only happen in fast mode
        if (fast && hasBeenScanned(vpath)) {
            return;
        }
        if (!followSymlinks) {
            final ArrayList<String> noLinks = new ArrayList<>();
            for (final String newFile : newFiles) {
                final Path filePath;
                if (dir == null) {
                    filePath = Paths.get(newFile);
                } else {
                    filePath = Paths.get(dir.toPath().toString(), newFile);
                }
                if (Files.isSymbolicLink(filePath)) {
                    final String name = vpath + newFile;
                    final File file = new File(dir, newFile);
                    if (file.isDirectory()) {
                        dirsExcluded.addElement(name);
                    } else if (file.isFile()) {
                        filesExcluded.addElement(name);
                    }
                    accountForNotFollowedSymlink(name, file);
                } else {
                    noLinks.add(newFile);
                }
            }
            newFiles = noLinks.toArray(new String[0]);
        } else {
            directoryNamesFollowed.addFirst(dir.getName());
        }

        for (String newFile : newFiles) {
            final String name = vpath + newFile;
            final TokenizedPath newPath = new TokenizedPath(path, newFile);
            final File file = new File(dir, newFile);
            final String[] children = file.list();
            if (children == null || (children.length == 0 && file.isFile())) {
                if (isIncluded(newPath)) {
                    accountForIncludedFile(newPath, file);
                } else {
                    everythingIncluded = false;
                    filesNotIncluded.addElement(name);
                }
            } else if (file.isDirectory()) { // dir

                if (followSymlinks
                        && causesIllegalSymlinkLoop(newFile, dir, directoryNamesFollowed)) {
                    // will be caught and redirected to Ant's logging system
                    System.err.println("skipping symbolic link "
                                       + file.getAbsolutePath()
                                       + " -- too many levels of symbolic"
                                       + " links.");
                    notFollowedSymlinks.add(file.getAbsolutePath());
                    continue;
                }

                if (isIncluded(newPath)) {
                    accountForIncludedDir(newPath, file, fast, children,
                                          directoryNamesFollowed);
                } else {
                    everythingIncluded = false;
                    dirsNotIncluded.addElement(name);
                    if (fast && couldHoldIncluded(newPath) && !contentsExcluded(newPath)) {
                        scandir(file, newPath, fast, children, directoryNamesFollowed);
                    }
                }
                if (!fast) {
                    scandir(file, newPath, fast, children, directoryNamesFollowed);
                }
            }
        }

        if (followSymlinks) {
            directoryNamesFollowed.removeFirst();
        }
    }

    /**
     * Process included file.
     * @param name  path of the file relative to the directory of the FileSet.
     * @param file  included File.
     */
    private void accountForIncludedFile(final TokenizedPath name, final File file) {
        processIncluded(name, file, filesIncluded, filesExcluded,
                        filesDeselected);
    }

    /**
     * Process included directory.
     * @param name path of the directory relative to the directory of
     *             the FileSet.
     * @param file directory as File.
     * @param fast whether to perform fast scans.
     */
    private void accountForIncludedDir(final TokenizedPath name, final File file,
                                       final boolean fast) {
        processIncluded(name, file, dirsIncluded, dirsExcluded, dirsDeselected);
        if (fast && couldHoldIncluded(name) && !contentsExcluded(name)) {
            scandir(file, name, fast);
        }
    }

    private void accountForIncludedDir(final TokenizedPath name,
                                       final File file, final boolean fast,
                                       final String[] children,
                                       final Deque<String> directoryNamesFollowed) {
        processIncluded(name, file, dirsIncluded, dirsExcluded, dirsDeselected);
        if (fast && couldHoldIncluded(name) && !contentsExcluded(name)) {
            scandir(file, name, fast, children, directoryNamesFollowed);
        }
    }

    private void accountForNotFollowedSymlink(final String name, final File file) {
        accountForNotFollowedSymlink(new TokenizedPath(name), file);
    }

    private void accountForNotFollowedSymlink(final TokenizedPath name, final File file) {
        if (!isExcluded(name) && (isIncluded(name)
                || (file.isDirectory() && couldHoldIncluded(name) && !contentsExcluded(name)))) {
            notFollowedSymlinks.add(file.getAbsolutePath());
        }
    }

    private void processIncluded(final TokenizedPath path,
                                 final File file, final List<String> inc, final List<String> exc,
                                 final List<String> des) {
        final String name = path.toString();
        if (inc.contains(name) || exc.contains(name) || des.contains(name)) {
            return;
        }
        boolean included = false;
        if (isExcluded(path)) {
            exc.add(name);
        } else if (isSelected(name, file)) {
            included = true;
            inc.add(name);
        } else {
            des.add(name);
        }
        everythingIncluded &= included;
    }

    /**
     * Test whether or not a name matches against at least one include
     * pattern.
     *
     * @param name The path to match. Must not be <code>null</code>.
     * @return <code>true</code> when the name matches against at least one
     *         include pattern, or <code>false</code> otherwise.
     */
    protected boolean isIncluded(final String name) {
        return isIncluded(new TokenizedPath(name));
    }

    /**
     * Test whether or not a name matches against at least one include
     * pattern.
     *
     * @param path The tokenized path to match. Must not be <code>null</code>.
     * @return <code>true</code> when the name matches against at least one
     *         include pattern, or <code>false</code> otherwise.
     */
    private boolean isIncluded(final TokenizedPath path) {
        ensureNonPatternSetsReady();

        String toMatch = path.toString();
        if (!isCaseSensitive()) {
            toMatch = toMatch.toUpperCase();
        }
        return includeNonPatterns.containsKey(toMatch)
            || Stream.of(includePatterns).anyMatch(p -> p.matchPath(path, isCaseSensitive()));
    }

    /**
     * Test whether or not a name matches the start of at least one include
     * pattern.
     *
     * @param name The name to match. Must not be <code>null</code>.
     * @return <code>true</code> when the name matches against the start of at
     *         least one include pattern, or <code>false</code> otherwise.
     */
    protected boolean couldHoldIncluded(final String name) {
        return couldHoldIncluded(new TokenizedPath(name));
    }

    /**
     * Test whether or not a name matches the start of at least one include
     * pattern.
     *
     * @param tokenizedName The name to match. Must not be <code>null</code>.
     * @return <code>true</code> when the name matches against the start of at
     *         least one include pattern, or <code>false</code> otherwise.
     */
    private boolean couldHoldIncluded(final TokenizedPath tokenizedName) {
        return Stream.concat(Stream.of(includePatterns),
                includeNonPatterns.values().stream().map(TokenizedPath::toPattern))
                .anyMatch(pat -> couldHoldIncluded(tokenizedName, pat));
    }

    /**
     * Test whether or not a name matches the start of the given
     * include pattern.
     *
     * @param tokenizedName The name to match. Must not be <code>null</code>.
     * @return <code>true</code> when the name matches against the start of the
     *         include pattern, or <code>false</code> otherwise.
     */
    private boolean couldHoldIncluded(final TokenizedPath tokenizedName,
                                      final TokenizedPattern tokenizedInclude) {
        return tokenizedInclude.matchStartOf(tokenizedName, isCaseSensitive())
            && isMorePowerfulThanExcludes(tokenizedName.toString())
            && isDeeper(tokenizedInclude, tokenizedName);
    }

    /**
     * Verify that a pattern specifies files deeper
     * than the level of the specified file.
     * @param pattern the pattern to check.
     * @param name the name to check.
     * @return whether the pattern is deeper than the name.
     * @since Ant 1.6.3
     */
    private boolean isDeeper(final TokenizedPattern pattern, final TokenizedPath name) {
        return pattern.containsPattern(SelectorUtils.DEEP_TREE_MATCH)
            || pattern.depth() > name.depth();
    }

    /**
     *  Find out whether one particular include pattern is more powerful
     *  than all the excludes.
     *  Note:  the power comparison is based on the length of the include pattern
     *  and of the exclude patterns without the wildcards.
     *  Ideally the comparison should be done based on the depth
     *  of the match; that is to say how many file separators have been matched
     *  before the first ** or the end of the pattern.
     *
     *  IMPORTANT : this function should return false "with care".
     *
     *  @param name the relative path to test.
     *  @return true if there is no exclude pattern more powerful than
     *  this include pattern.
     *  @since Ant 1.6
     */
    private boolean isMorePowerfulThanExcludes(final String name) {
        final String soughtexclude = name + File.separatorChar + SelectorUtils.DEEP_TREE_MATCH;
        return Stream.of(excludePatterns).map(Object::toString)
            .noneMatch(Predicate.isEqual(soughtexclude));
    }

    /**
     * Test whether all contents of the specified directory must be excluded.
     * @param path the path to check.
     * @return whether all the specified directory's contents are excluded.
     */
    /* package */ boolean contentsExcluded(final TokenizedPath path) {
        return Stream.of(excludePatterns)
            .filter(p -> p.endsWith(SelectorUtils.DEEP_TREE_MATCH))
            .map(TokenizedPattern::withoutLastToken)
            .anyMatch(wlt -> wlt.matchPath(path, isCaseSensitive()));
    }

    /**
     * Test whether or not a name matches against at least one exclude
     * pattern.
     *
     * @param name The name to match. Must not be <code>null</code>.
     * @return <code>true</code> when the name matches against at least one
     *         exclude pattern, or <code>false</code> otherwise.
     */
    protected boolean isExcluded(final String name) {
        return isExcluded(new TokenizedPath(name));
    }

    /**
     * Test whether or not a name matches against at least one exclude
     * pattern.
     *
     * @param name The name to match. Must not be <code>null</code>.
     * @return <code>true</code> when the name matches against at least one
     *         exclude pattern, or <code>false</code> otherwise.
     */
    private boolean isExcluded(final TokenizedPath name) {
        ensureNonPatternSetsReady();

        String toMatch = name.toString();
        if (!isCaseSensitive()) {
            toMatch = toMatch.toUpperCase();
        }
        return excludeNonPatterns.containsKey(toMatch)
            || Stream.of(excludePatterns).anyMatch(p -> p.matchPath(name, isCaseSensitive()));
    }

    /**
     * Test whether a file should be selected.
     *
     * @param name the filename to check for selecting.
     * @param file the java.io.File object for this filename.
     * @return <code>false</code> when the selectors says that the file
     *         should not be selected, <code>true</code> otherwise.
     */
    protected boolean isSelected(final String name, final File file) {
        return selectors == null
                || Stream.of(selectors).allMatch(sel -> sel.isSelected(basedir, name, file));
    }

    /**
     * Return the names of the files which matched at least one of the
     * include patterns and none of the exclude patterns.
     * The names are relative to the base directory.
     *
     * @return the names of the files which matched at least one of the
     *         include patterns and none of the exclude patterns.
     */
    @Override
    public String[] getIncludedFiles() {
        String[] files;
        synchronized (this) {
            if (filesIncluded == null) {
                throw new IllegalStateException("Must call scan() first");
            }
            files = filesIncluded.toArray(new String[0]);
        }
        Arrays.sort(files);
        return files;
    }

    /**
     * Return the count of included files.
     * @return <code>int</code>.
     * @since Ant 1.6.3
     */
    public synchronized int getIncludedFilesCount() {
        if (filesIncluded == null) {
            throw new IllegalStateException("Must call scan() first");
        }
        return filesIncluded.size();
    }

    /**
     * Return the names of the files which matched none of the include
     * patterns. The names are relative to the base directory. This involves
     * performing a slow scan if one has not already been completed.
     *
     * @return the names of the files which matched none of the include
     *         patterns.
     *
     * @see #slowScan
     */
    @Override
    public synchronized String[] getNotIncludedFiles() {
        slowScan();
        return filesNotIncluded.toArray(new String[0]);
    }

    /**
     * Return the names of the files which matched at least one of the
     * include patterns and at least one of the exclude patterns.
     * The names are relative to the base directory. This involves
     * performing a slow scan if one has not already been completed.
     *
     * @return the names of the files which matched at least one of the
     *         include patterns and at least one of the exclude patterns.
     *
     * @see #slowScan
     */
    @Override
    public synchronized String[] getExcludedFiles() {
        slowScan();
        return filesExcluded.toArray(new String[0]);
    }

    /**
     * <p>Return the names of the files which were selected out and
     * therefore not ultimately included.</p>
     *
     * <p>The names are relative to the base directory. This involves
     * performing a slow scan if one has not already been completed.</p>
     *
     * @return the names of the files which were deselected.
     *
     * @see #slowScan
     */
    @Override
    public synchronized String[] getDeselectedFiles() {
        slowScan();
        return filesDeselected.toArray(new String[0]);
    }

    /**
     * Return the names of the directories which matched at least one of the
     * include patterns and none of the exclude patterns.
     * The names are relative to the base directory.
     *
     * @return the names of the directories which matched at least one of the
     * include patterns and none of the exclude patterns.
     */
    @Override
    public String[] getIncludedDirectories() {
        String[] directories;
        synchronized (this) {
            if (dirsIncluded == null) {
                throw new IllegalStateException("Must call scan() first");
            }
            directories = dirsIncluded.toArray(new String[0]);
        }
        Arrays.sort(directories);
        return directories;
    }

    /**
     * Return the count of included directories.
     * @return <code>int</code>.
     * @since Ant 1.6.3
     */
    public synchronized int getIncludedDirsCount() {
        if (dirsIncluded == null) {
            throw new IllegalStateException("Must call scan() first");
        }
        return dirsIncluded.size();
    }

    /**
     * Return the names of the directories which matched none of the include
     * patterns. The names are relative to the base directory. This involves
     * performing a slow scan if one has not already been completed.
     *
     * @return the names of the directories which matched none of the include
     * patterns.
     *
     * @see #slowScan
     */
    @Override
    public synchronized String[] getNotIncludedDirectories() {
        slowScan();
        return dirsNotIncluded.toArray(new String[0]);
    }

    /**
     * Return the names of the directories which matched at least one of the
     * include patterns and at least one of the exclude patterns.
     * The names are relative to the base directory. This involves
     * performing a slow scan if one has not already been completed.
     *
     * @return the names of the directories which matched at least one of the
     * include patterns and at least one of the exclude patterns.
     *
     * @see #slowScan
     */
    @Override
    public synchronized String[] getExcludedDirectories() {
        slowScan();
        return dirsExcluded.toArray(new String[0]);
    }

    /**
     * <p>Return the names of the directories which were selected out and
     * therefore not ultimately included.</p>
     *
     * <p>The names are relative to the base directory. This involves
     * performing a slow scan if one has not already been completed.</p>
     *
     * @return the names of the directories which were deselected.
     *
     * @see #slowScan
     */
    @Override
    public synchronized String[] getDeselectedDirectories() {
        slowScan();
        return dirsDeselected.toArray(new String[0]);
    }

    /**
     * Absolute paths of all symbolic links that haven't been followed
     * but would have been followed had followsymlinks been true or
     * maxLevelsOfSymlinks been bigger.
     *
     * @return sorted array of not followed symlinks
     * @since Ant 1.8.0
     * @see #notFollowedSymlinks
     */
    public synchronized String[] getNotFollowedSymlinks() {
        String[] links;
        synchronized (this) {
            links = notFollowedSymlinks.toArray(new String[0]);
        }
        Arrays.sort(links);
        return links;
    }

    /**
     * Add default exclusions to the current exclusions set.
     */
    @Override
    public synchronized void addDefaultExcludes() {
        Stream<String> s = Stream.of(getDefaultExcludes()).map(p -> p.replace('/',
                File.separatorChar).replace('\\', File.separatorChar));
        if (excludes != null) {
            s = Stream.concat(Stream.of(excludes), s);
        }
        excludes = s.toArray(String[]::new);
    }

    /**
     * Get the named resource.
     * @param name path name of the file relative to the dir attribute.
     *
     * @return the resource with the given name.
     * @since Ant 1.5.2
     */
    @Override
    public synchronized Resource getResource(final String name) {
        return new FileResource(basedir, name);
    }

    /**
     * Has the directory with the given path relative to the base
     * directory already been scanned?
     *
     * <p>Registers the given directory as scanned as a side effect.</p>
     *
     * @since Ant 1.6
     */
    private boolean hasBeenScanned(final String vpath) {
        return !scannedDirs.add(vpath);
    }

    /**
     * This method is of interest for testing purposes.  The returned
     * Set is live and should not be modified.
     * @return the Set of relative directory names that have been scanned.
     */
    /* package-private */ Set<String> getScannedDirs() {
        return scannedDirs;
    }

    /**
     * Clear internal caches.
     *
     * @since Ant 1.6
     */
    private synchronized void clearCaches() {
        includeNonPatterns.clear();
        excludeNonPatterns.clear();
        includePatterns = null;
        excludePatterns = null;
        areNonPatternSetsReady = false;
    }

    /**
     * Ensure that the in|exclude &quot;patterns&quot;
     * have been properly divided up.
     *
     * @since Ant 1.6.3
     */
    /* package */ synchronized void ensureNonPatternSetsReady() {
        if (!areNonPatternSetsReady) {
            includePatterns = fillNonPatternSet(includeNonPatterns, includes);
            excludePatterns = fillNonPatternSet(excludeNonPatterns, excludes);
            areNonPatternSetsReady = true;
        }
    }

    /**
     * Add all patterns that are not real patterns (do not contain
     * wildcards) to the set and returns the real patterns.
     *
     * @param map Map to populate.
     * @param patterns String[] of patterns.
     * @since Ant 1.8.0
     */
    private TokenizedPattern[] fillNonPatternSet(final Map<String, TokenizedPath> map,
                                                 final String[] patterns) {
        final List<TokenizedPattern> al = new ArrayList<>(patterns.length);
        for (String pattern : patterns) {
            if (SelectorUtils.hasWildcards(pattern)) {
                al.add(new TokenizedPattern(pattern));
            } else {
                final String s = isCaseSensitive() ? pattern : pattern.toUpperCase();
                map.put(s, new TokenizedPath(s));
            }
        }
        return al.toArray(new TokenizedPattern[0]);
    }

    /**
     * Would following the given directory cause a loop of symbolic
     * links deeper than allowed?
     *
     * <p>Can only happen if the given directory has been seen at
     * least more often than allowed during the current scan and it is
     * a symbolic link and enough other occurrences of the same name
     * higher up are symbolic links that point to the same place.</p>
     *
     * @since Ant 1.8.0
     */
    private boolean causesIllegalSymlinkLoop(final String dirName, final File parent,
                                             final Deque<String> directoryNamesFollowed) {
        try {
            final Path dirPath;
            if (parent == null) {
                dirPath = Paths.get(dirName);
            } else {
                dirPath = Paths.get(parent.toPath().toString(), dirName);
            }
            if (directoryNamesFollowed.size() >= maxLevelsOfSymlinks
                && Collections.frequency(directoryNamesFollowed, dirName) >= maxLevelsOfSymlinks
                && Files.isSymbolicLink(dirPath)) {

                final List<String> files = new ArrayList<>();
                File f = FILE_UTILS.resolveFile(parent, dirName);
                final String target = f.getCanonicalPath();
                files.add(target);

                StringBuilder relPath = new StringBuilder();
                for (final String dir : directoryNamesFollowed) {
                    relPath.append("../");
                    if (dirName.equals(dir)) {
                        f = FILE_UTILS.resolveFile(parent, relPath + dir);
                        files.add(f.getCanonicalPath());
                        if (files.size() > maxLevelsOfSymlinks
                            && Collections.frequency(files, target) > maxLevelsOfSymlinks) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (final IOException ex) {
            throw new BuildException(
                "Caught error while checking for symbolic links", ex);
        }
    }

}
