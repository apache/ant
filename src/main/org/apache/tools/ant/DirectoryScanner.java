/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant;

import java.io.*;
import java.util.*;

/**
 * Class for scanning a directory for files/directories that match a certain
 * criteria.
 * <p>
 * These criteria consist of a set of include and exclude patterns. With these
 * patterns, you can select which files you want to have included, and which
 * files you want to have excluded.
 * <p>
 * The idea is simple. A given directory is recursively scanned for all files
 * and directories. Each file/directory is matched against a set of include
 * and exclude patterns. Only files/directories that match at least one
 * pattern of the include pattern list, and don't match a pattern of the
 * exclude pattern list will be placed in the list of files/directories found.
 * <p>
 * When no list of include patterns is supplied, "**" will be used, which
 * means that everything will be matched. When no list of exclude patterns is
 * supplied, an empty list is used, such that nothing will be excluded.
 * <p>
 * The pattern matching is done as follows:
 * The name to be matched is split up in path segments. A path segment is the
 * name of a directory or file, which is bounded by
 * <code>File.separator</code> ('/' under UNIX, '\' under Windows).
 * E.g. "abc/def/ghi/xyz.java" is split up in the segments "abc", "def", "ghi"
 * and "xyz.java".
 * The same is done for the pattern against which should be matched.
 * <p>
 * Then the segments of the name and the pattern will be matched against each
 * other. When '**' is used for a path segment in the pattern, then it matches
 * zero or more path segments of the name.
 * <p>
 * There are special case regarding the use of <code>File.separator</code>s at
 * the beginningof the pattern and the string to match:<br>
 * When a pattern starts with a <code>File.separator</code>, the string
 * to match must also start with a <code>File.separator</code>.
 * When a pattern does not start with a <code>File.separator</code>, the
 * string to match may not start with a <code>File.separator</code>.
 * When one of these rules is not obeyed, the string will not
 * match.
 * <p>
 * When a name path segment is matched against a pattern path segment, the
 * following special characters can be used:
 * '*' matches zero or more characters,
 * '?' matches one character.
 * <p>
 * Examples:
 * <p>
 * "**\*.class" matches all .class files/dirs in a directory tree.
 * <p>
 * "test\a??.java" matches all files/dirs which start with an 'a', then two
 * more characters and then ".java", in a directory called test.
 * <p>
 * "**" matches everything in a directory tree.
 * <p>
 * "**\test\**\XYZ*" matches all files/dirs that start with "XYZ" and where
 * there is a parent directory called test (e.g. "abc\test\def\ghi\XYZ123").
 * <p>
 * Case sensitivity may be turned off if necessary.  By default, it is
 * turned on.
 * <p>
 * Example of usage:
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
 *   for (int i = 0; i < files.length;i++) {
 *     System.out.println(files[i]);
 *   }
 * </pre>
 * This will scan a directory called test for .class files, but excludes all
 * .class files in all directories under a directory called "modules"
 *
 * @author Arnout J. Kuiper <a href="mailto:ajkuiper@wxs.nl">ajkuiper@wxs.nl</a>
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 */
public class DirectoryScanner implements FileScanner {

    /**
     * Patterns that should be excluded by default.
     *
     * @see #addDefaultExcludes()
     */
    protected final static String[] DEFAULTEXCLUDES = {
        "**/*~",
        "**/#*#",
        "**/.#*",
        "**/%*%",
        "**/CVS",
        "**/CVS/**",
        "**/.cvsignore",
        "**/SCCS",
        "**/SCCS/**",
        "**/vssver.scc"
    };

    /**
     * The base directory which should be scanned.
     */
    protected File basedir;

    /**
     * The patterns for the files that should be included.
     */
    protected String[] includes;

    /**
     * The patterns for the files that should be excluded.
     */
    protected String[] excludes;

    /**
     * The files that where found and matched at least one includes, and matched
     * no excludes.
     */
    protected Vector filesIncluded;

    /**
     * The files that where found and did not match any includes.
     */
    protected Vector filesNotIncluded;

    /**
     * The files that where found and matched at least one includes, and also
     * matched at least one excludes.
     */
    protected Vector filesExcluded;

    /**
     * The directories that where found and matched at least one includes, and
     * matched no excludes.
     */
    protected Vector dirsIncluded;

    /**
     * The directories that where found and did not match any includes.
     */
    protected Vector dirsNotIncluded;

    /**
     * The files that where found and matched at least one includes, and also
     * matched at least one excludes.
     */
    protected Vector dirsExcluded;

    /**
     * Have the Vectors holding our results been built by a slow scan?
     */
    protected boolean haveSlowResults = false;

    /**
     * Should the file system be treated as a case sensitive one?
     */
    protected boolean isCaseSensitive = true;

    /**
     * Constructor.
     */
    public DirectoryScanner() {
    }


    /**
     * Does the path match the start of this pattern up to the first "**".
     *
     * <p>This is not a general purpose test and should only be used if you
     * can live with false positives.</p>
     *
     * <p><code>pattern=**\\a</code> and <code>str=b</code> will yield true.
     *
     * @param pattern the (non-null) pattern to match against
     * @param str     the (non-null) string (path) to match
     */
    protected static boolean matchPatternStart(String pattern, String str) {
        return matchPatternStart(pattern, str, true);
    }

    /**
     * Does the path match the start of this pattern up to the first "**".
     *
     * <p>This is not a general purpose test and should only be used if you
     * can live with false positives.</p>
     *
     * <p><code>pattern=**\\a</code> and <code>str=b</code> will yield true.
     *
     * @param pattern             the (non-null) pattern to match against
     * @param str                 the (non-null) string (path) to match
     * @param isCaseSensitive     must matches be case sensitive?
     */
    protected static boolean matchPatternStart(String pattern, String str,
                                               boolean isCaseSensitive) {
        // When str starts with a File.separator, pattern has to start with a
        // File.separator.
        // When pattern starts with a File.separator, str has to start with a
        // File.separator.
        if (str.startsWith(File.separator) !=
            pattern.startsWith(File.separator)) {
            return false;
        }

        Vector patDirs = new Vector();
        StringTokenizer st = new StringTokenizer(pattern,File.separator);
        while (st.hasMoreTokens()) {
            patDirs.addElement(st.nextToken());
        }

        Vector strDirs = new Vector();
        st = new StringTokenizer(str,File.separator);
        while (st.hasMoreTokens()) {
            strDirs.addElement(st.nextToken());
        }

        int patIdxStart = 0;
        int patIdxEnd   = patDirs.size()-1;
        int strIdxStart = 0;
        int strIdxEnd   = strDirs.size()-1;

        // up to first '**'
        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            String patDir = (String)patDirs.elementAt(patIdxStart);
            if (patDir.equals("**")) {
                break;
            }
            if (!match(patDir,(String)strDirs.elementAt(strIdxStart), isCaseSensitive)) {
                return false;
            }
            patIdxStart++;
            strIdxStart++;
        }

        if (strIdxStart > strIdxEnd) {
            // String is exhausted
            return true;
        } else if (patIdxStart > patIdxEnd) {
            // String not exhausted, but pattern is. Failure.
            return false;
        } else {
            // pattern now holds ** while string is not exhausted
            // this will generate false positives but we can live with that.
            return true;
        }
    }

    /**
     * Matches a path against a pattern.
     *
     * @param pattern the (non-null) pattern to match against
     * @param str     the (non-null) string (path) to match
     *
     * @return <code>true</code> when the pattern matches against the string.
     *         <code>false</code> otherwise.
     */
    protected static boolean matchPath(String pattern, String str) {
        return matchPath(pattern, str, true);
    }

    /**
     * Matches a path against a pattern.
     *
     * @param pattern            the (non-null) pattern to match against
     * @param str                the (non-null) string (path) to match
     * @param isCaseSensitive    must a case sensitive match be done?
     *
     * @return <code>true</code> when the pattern matches against the string.
     *         <code>false</code> otherwise.
     */
    protected static boolean matchPath(String pattern, String str, boolean isCaseSensitive) {
        // When str starts with a File.separator, pattern has to start with a
        // File.separator.
        // When pattern starts with a File.separator, str has to start with a
        // File.separator.
        if (str.startsWith(File.separator) !=
            pattern.startsWith(File.separator)) {
            return false;
        }

        Vector patDirs = new Vector();
        StringTokenizer st = new StringTokenizer(pattern,File.separator);
        while (st.hasMoreTokens()) {
            patDirs.addElement(st.nextToken());
        }

        Vector strDirs = new Vector();
        st = new StringTokenizer(str,File.separator);
        while (st.hasMoreTokens()) {
            strDirs.addElement(st.nextToken());
        }

        int patIdxStart = 0;
        int patIdxEnd   = patDirs.size()-1;
        int strIdxStart = 0;
        int strIdxEnd   = strDirs.size()-1;

        // up to first '**'
        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            String patDir = (String)patDirs.elementAt(patIdxStart);
            if (patDir.equals("**")) {
                break;
            }
            if (!match(patDir,(String)strDirs.elementAt(strIdxStart), isCaseSensitive)) {
                return false;
            }
            patIdxStart++;
            strIdxStart++;
        }
        if (strIdxStart > strIdxEnd) {
            // String is exhausted
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (!patDirs.elementAt(i).equals("**")) {
                    return false;
                }
            }
            return true;
        } else {
            if (patIdxStart > patIdxEnd) {
                // String not exhausted, but pattern is. Failure.
                return false;
            }
        }

        // up to last '**'
        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            String patDir = (String)patDirs.elementAt(patIdxEnd);
            if (patDir.equals("**")) {
                break;
            }
            if (!match(patDir,(String)strDirs.elementAt(strIdxEnd), isCaseSensitive)) {
                return false;
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if (strIdxStart > strIdxEnd) {
            // String is exhausted
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (!patDirs.elementAt(i).equals("**")) {
                    return false;
                }
            }
            return true;
        }

        while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
            int patIdxTmp = -1;
            for (int i = patIdxStart+1; i <= patIdxEnd; i++) {
                if (patDirs.elementAt(i).equals("**")) {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == patIdxStart+1) {
                // '**/**' situation, so skip one
                patIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = (patIdxTmp-patIdxStart-1);
            int strLength = (strIdxEnd-strIdxStart+1);
            int foundIdx  = -1;
strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    String subPat = (String)patDirs.elementAt(patIdxStart+j+1);
                    String subStr = (String)strDirs.elementAt(strIdxStart+i+j);
                    if (!match(subPat,subStr, isCaseSensitive)) {
                        continue strLoop;
                    }
                }

                foundIdx = strIdxStart+i;
                break;
            }

            if (foundIdx == -1) {
                return false;
            }

            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx+patLength;
        }

        for (int i = patIdxStart; i <= patIdxEnd; i++) {
            if (!patDirs.elementAt(i).equals("**")) {
                return false;
            }
        }

        return true;
    }


    /**
     * Matches a string against a pattern. The pattern contains two special
     * characters:
     * '*' which means zero or more characters,
     * '?' which means one and only one character.
     *
     * @param pattern the (non-null) pattern to match against
     * @param str     the (non-null) string that must be matched against the
     *                pattern
     *
     * @return <code>true</code> when the string matches against the pattern,
     *         <code>false</code> otherwise.
     */
    protected static boolean match(String pattern, String str) {
        return match(pattern, str, true);
    }


    /**
     * Matches a string against a pattern. The pattern contains two special
     * characters:
     * '*' which means zero or more characters,
     * '?' which means one and only one character.
     *
     * @param pattern the (non-null) pattern to match against
     * @param str     the (non-null) string that must be matched against the
     *                pattern
     *
     * @return <code>true</code> when the string matches against the pattern,
     *         <code>false</code> otherwise.
     */
    protected static boolean match(String pattern, String str, boolean isCaseSensitive) {
        char[] patArr = pattern.toCharArray();
        char[] strArr = str.toCharArray();
        int patIdxStart = 0;
        int patIdxEnd   = patArr.length-1;
        int strIdxStart = 0;
        int strIdxEnd   = strArr.length-1;
        char ch;

        boolean containsStar = false;
        for (int i = 0; i < patArr.length; i++) {
            if (patArr[i] == '*') {
                containsStar = true;
                break;
            }
        }

        if (!containsStar) {
            // No '*'s, so we make a shortcut
            if (patIdxEnd != strIdxEnd) {
                return false; // Pattern and string do not have the same size
            }
            for (int i = 0; i <= patIdxEnd; i++) {
                ch = patArr[i];
                if (ch != '?') {
                    if (isCaseSensitive && ch != strArr[i]) {
                        return false;// Character mismatch
                    }
                    if (!isCaseSensitive && Character.toUpperCase(ch) !=
                        Character.toUpperCase(strArr[i])) {
                        return false; // Character mismatch
                    }
                }
            }
            return true; // String matches against pattern
        }
        
        if (patIdxEnd == 0) {
            return true; // Pattern contains only '*', which matches anything
        }

        // Process characters before first star
        while((ch = patArr[patIdxStart]) != '*' && strIdxStart <= strIdxEnd) {
            if (ch != '?') {
                if (isCaseSensitive && ch != strArr[strIdxStart]) {
                    return false;// Character mismatch
                }
                if (!isCaseSensitive && Character.toUpperCase(ch) !=
                    Character.toUpperCase(strArr[strIdxStart])) {
                    return false;// Character mismatch
                }
            }
            patIdxStart++;
            strIdxStart++;
        }
        if (strIdxStart > strIdxEnd) {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (patArr[i] != '*') {
                    return false;
                }
            }
            return true;
        }

        // Process characters after last star
        while((ch = patArr[patIdxEnd]) != '*' && strIdxStart <= strIdxEnd) {
            if (ch != '?') {
                if (isCaseSensitive && ch != strArr[strIdxEnd]) {
                    return false;// Character mismatch
                }
                if (!isCaseSensitive && Character.toUpperCase(ch) !=
                    Character.toUpperCase(strArr[strIdxEnd])) {
                    return false;// Character mismatch
                }
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if (strIdxStart > strIdxEnd) {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (patArr[i] != '*') {
                    return false;
                }
            }
            return true;
        }

        // process pattern between stars. padIdxStart and patIdxEnd point
        // always to a '*'.
        while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
            int patIdxTmp = -1;
            for (int i = patIdxStart+1; i <= patIdxEnd; i++) {
                if (patArr[i] == '*') {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == patIdxStart+1) {
                // Two stars next to each other, skip the first one.
                patIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = (patIdxTmp-patIdxStart-1);
            int strLength = (strIdxEnd-strIdxStart+1);
            int foundIdx  = -1;
            strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    ch = patArr[patIdxStart+j+1];
                    if (ch != '?') {
                        if (isCaseSensitive && ch != strArr[strIdxStart+i+j]) {
                            continue strLoop;
                        }
                        if (!isCaseSensitive && Character.toUpperCase(ch) !=
                            Character.toUpperCase(strArr[strIdxStart+i+j])) {
                            continue strLoop;
                        }
                    }
                }

                foundIdx = strIdxStart+i;
                break;
            }

            if (foundIdx == -1) {
                return false;
            }

            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx+patLength;
        }

        // All characters in the string are used. Check if only '*'s are left
        // in the pattern. If so, we succeeded. Otherwise failure.
        for (int i = patIdxStart; i <= patIdxEnd; i++) {
            if (patArr[i] != '*') {
                return false;
            }
        }
        return true;
    }



    /**
     * Sets the basedir for scanning. This is the directory that is scanned
     * recursively. All '/' and '\' characters are replaced by
     * <code>File.separatorChar</code>. So the separator used need not match
     * <code>File.separatorChar</code>.
     *
     * @param basedir the (non-null) basedir for scanning
     */
    public void setBasedir(String basedir) {
        setBasedir(new File(basedir.replace('/',File.separatorChar).replace('\\',File.separatorChar)));
    }



    /**
     * Sets the basedir for scanning. This is the directory that is scanned
     * recursively.
     *
     * @param basedir the basedir for scanning
     */
    public void setBasedir(File basedir) {
        this.basedir = basedir;
    }



    /**
     * Gets the basedir that is used for scanning. This is the directory that
     * is scanned recursively.
     *
     * @return the basedir that is used for scanning
     */
    public File getBasedir() {
        return basedir;
    }



    /**
     * Sets the case sensitivity of the file system
     *
     * @param specifies if the filesystem is case sensitive
     */
    public void setCaseSensitive(boolean isCaseSensitive) {
        this.isCaseSensitive = isCaseSensitive;
    }

    /**
     * Sets the set of include patterns to use. All '/' and '\' characters are
     * replaced by <code>File.separatorChar</code>. So the separator used need
     * not match <code>File.separatorChar</code>.
     * <p>
     * When a pattern ends with a '/' or '\', "**" is appended.
     *
     * @param includes list of include patterns
     */
    public void setIncludes(String[] includes) {
        if (includes == null) {
            this.includes = null;
        } else {
            this.includes = new String[includes.length];
            for (int i = 0; i < includes.length; i++) {
                String pattern;
                pattern = includes[i].replace('/',File.separatorChar).replace('\\',File.separatorChar);
                if (pattern.endsWith(File.separator)) {
                    pattern += "**";
                }
                this.includes[i] = pattern;
            }
        }
    }



    /**
     * Sets the set of exclude patterns to use. All '/' and '\' characters are
     * replaced by <code>File.separatorChar</code>. So the separator used need
     * not match <code>File.separatorChar</code>.
     * <p>
     * When a pattern ends with a '/' or '\', "**" is appended.
     *
     * @param excludes list of exclude patterns
     */
    public void setExcludes(String[] excludes) {
        if (excludes == null) {
            this.excludes = null;
        } else {
            this.excludes = new String[excludes.length];
            for (int i = 0; i < excludes.length; i++) {
                String pattern;
                pattern = excludes[i].replace('/',File.separatorChar).replace('\\',File.separatorChar);
                if (pattern.endsWith(File.separator)) {
                    pattern += "**";
                }
                this.excludes[i] = pattern;
            }
        }
    }



    /**
     * Scans the base directory for files that match at least one include
     * pattern, and don't match any exclude patterns.
     *
     * @exception IllegalStateException when basedir was set incorrecly
     */
    public void scan() {
        if (basedir == null) {
            throw new IllegalStateException("No basedir set");
        }
        if (!basedir.exists()) {
            throw new IllegalStateException("basedir " + basedir
                                            + " does not exist");
        }
        if (!basedir.isDirectory()) {
            throw new IllegalStateException("basedir " + basedir
                                            + " is not a directory");
        }

        if (includes == null) {
            // No includes supplied, so set it to 'matches all'
            includes = new String[1];
            includes[0] = "**";
        }
        if (excludes == null) {
            excludes = new String[0];
        }

        filesIncluded    = new Vector();
        filesNotIncluded = new Vector();
        filesExcluded    = new Vector();
        dirsIncluded     = new Vector();
        dirsNotIncluded  = new Vector();
        dirsExcluded     = new Vector();

        if (isIncluded("")) {
            if (!isExcluded("")) {
                dirsIncluded.addElement("");
            } else {
                dirsExcluded.addElement("");
            }
        } else {
            dirsNotIncluded.addElement("");
        }
        scandir(basedir, "", true);
    }

    /**
     * Toplevel invocation for the scan.
     *
     * <p>Returns immediately if a slow scan has already been requested.
     */
    protected void slowScan() {
        if (haveSlowResults) {
            return;
        }

        String[] excl = new String[dirsExcluded.size()];
        dirsExcluded.copyInto(excl);

        String[] notIncl = new String[dirsNotIncluded.size()];
        dirsNotIncluded.copyInto(notIncl);

        for (int i=0; i<excl.length; i++) {
            if (!couldHoldIncluded(excl[i])) {
                scandir(new File(basedir, excl[i]), 
                        excl[i]+File.separator, false);
            }
        }
        
        for (int i=0; i<notIncl.length; i++) {
            if (!couldHoldIncluded(notIncl[i])) {
                scandir(new File(basedir, notIncl[i]), 
                        notIncl[i]+File.separator, false);
            }
        }

        haveSlowResults  = true;
    }


    /**
     * Scans the passed dir for files and directories. Found files and
     * directories are placed in their respective collections, based on the
     * matching of includes and excludes. When a directory is found, it is
     * scanned recursively.
     *
     * @param dir   the directory to scan
     * @param vpath the path relative to the basedir (needed to prevent
     *              problems with an absolute path when using dir)
     *
     * @see #filesIncluded
     * @see #filesNotIncluded
     * @see #filesExcluded
     * @see #dirsIncluded
     * @see #dirsNotIncluded
     * @see #dirsExcluded
     */
    protected void scandir(File dir, String vpath, boolean fast) {
        String[] newfiles = dir.list();

        if (newfiles == null) {
            /*
             * two reasons are mentioned in the API docs for File.list
             * (1) dir is not a directory. This is impossible as
             *     we wouldn't get here in this case.
             * (2) an IO error occurred (why doesn't it throw an exception 
             *     then???)
             */
            throw new BuildException("IO error scanning directory "
                                     + dir.getAbsolutePath());
        }

        for (int i = 0; i < newfiles.length; i++) {
            String name = vpath+newfiles[i];
            File   file = new File(dir,newfiles[i]);
            if (file.isDirectory()) {
                if (isIncluded(name)) {
                    if (!isExcluded(name)) {
                        dirsIncluded.addElement(name);
                        if (fast) {
                            scandir(file, name+File.separator, fast);
                        }
                    } else {
                        dirsExcluded.addElement(name);
                        if (fast && couldHoldIncluded(name)) {
                            scandir(file, name+File.separator, fast);
                        }
                    }
                } else {
                    dirsNotIncluded.addElement(name);
                    if (fast && couldHoldIncluded(name)) {
                        scandir(file, name+File.separator, fast);
                    }
                }
                if (!fast) {
                    scandir(file, name+File.separator, fast);
                }
            } else if (file.isFile()) {
                if (isIncluded(name)) {
                    if (!isExcluded(name)) {
                        filesIncluded.addElement(name);
                    } else {
                        filesExcluded.addElement(name);
                    }
                } else {
                    filesNotIncluded.addElement(name);
                }
            }
        }
    }



    /**
     * Tests whether a name matches against at least one include pattern.
     *
     * @param name the name to match
     * @return <code>true</code> when the name matches against at least one
     *         include pattern, <code>false</code> otherwise.
     */
    protected boolean isIncluded(String name) {
        for (int i = 0; i < includes.length; i++) {
            if (matchPath(includes[i],name, isCaseSensitive)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests whether a name matches the start of at least one include pattern.
     *
     * @param name the name to match
     * @return <code>true</code> when the name matches against at least one
     *         include pattern, <code>false</code> otherwise.
     */
    protected boolean couldHoldIncluded(String name) {
        for (int i = 0; i < includes.length; i++) {
            if (matchPatternStart(includes[i],name, isCaseSensitive)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests whether a name matches against at least one exclude pattern.
     *
     * @param name the name to match
     * @return <code>true</code> when the name matches against at least one
     *         exclude pattern, <code>false</code> otherwise.
     */
    protected boolean isExcluded(String name) {
        for (int i = 0; i < excludes.length; i++) {
            if (matchPath(excludes[i],name, isCaseSensitive)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Get the names of the files that matched at least one of the include
     * patterns, and matched none of the exclude patterns.
     * The names are relative to the basedir.
     *
     * @return the names of the files
     */
    public String[] getIncludedFiles() {
        int count = filesIncluded.size();
        String[] files = new String[count];
        for (int i = 0; i < count; i++) {
            files[i] = (String)filesIncluded.elementAt(i);
        }
        return files;
    }



    /**
     * Get the names of the files that matched at none of the include patterns.
     * The names are relative to the basedir.
     *
     * @return the names of the files
     */
    public String[] getNotIncludedFiles() {
        slowScan();
        int count = filesNotIncluded.size();
        String[] files = new String[count];
        for (int i = 0; i < count; i++) {
            files[i] = (String)filesNotIncluded.elementAt(i);
        }
        return files;
    }



    /**
     * Get the names of the files that matched at least one of the include
     * patterns, an matched also at least one of the exclude patterns.
     * The names are relative to the basedir.
     *
     * @return the names of the files
     */
    public String[] getExcludedFiles() {
        slowScan();
        int count = filesExcluded.size();
        String[] files = new String[count];
        for (int i = 0; i < count; i++) {
            files[i] = (String)filesExcluded.elementAt(i);
        }
        return files;
    }



    /**
     * Get the names of the directories that matched at least one of the include
     * patterns, an matched none of the exclude patterns.
     * The names are relative to the basedir.
     *
     * @return the names of the directories
     */
    public String[] getIncludedDirectories() {
        int count = dirsIncluded.size();
        String[] directories = new String[count];
        for (int i = 0; i < count; i++) {
            directories[i] = (String)dirsIncluded.elementAt(i);
        }
        return directories;
    }



    /**
     * Get the names of the directories that matched at none of the include
     * patterns.
     * The names are relative to the basedir.
     *
     * @return the names of the directories
     */
    public String[] getNotIncludedDirectories() {
        slowScan();
        int count = dirsNotIncluded.size();
        String[] directories = new String[count];
        for (int i = 0; i < count; i++) {
            directories[i] = (String)dirsNotIncluded.elementAt(i);
        }
        return directories;
    }



    /**
     * Get the names of the directories that matched at least one of the include
     * patterns, an matched also at least one of the exclude patterns.
     * The names are relative to the basedir.
     *
     * @return the names of the directories
     */
    public String[] getExcludedDirectories() {
        slowScan();
        int count = dirsExcluded.size();
        String[] directories = new String[count];
        for (int i = 0; i < count; i++) {
            directories[i] = (String)dirsExcluded.elementAt(i);
        }
        return directories;
    }



    /**
     * Adds the array with default exclusions to the current exclusions set.
     *
     */
    public void addDefaultExcludes() {
        int excludesLength = excludes == null ? 0 : excludes.length;
        String[] newExcludes;
        newExcludes = new String[excludesLength + DEFAULTEXCLUDES.length];
        if (excludesLength > 0) {
            System.arraycopy(excludes,0,newExcludes,0,excludesLength);
        }
        for (int i = 0; i < DEFAULTEXCLUDES.length; i++) {
            newExcludes[i+excludesLength] = DEFAULTEXCLUDES[i].replace('/',File.separatorChar).replace('\\',File.separatorChar);
        }
        excludes = newExcludes;
    }

}
