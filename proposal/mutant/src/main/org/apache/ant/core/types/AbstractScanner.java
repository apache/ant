/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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

package org.apache.ant.core.types;

import java.io.*;
import java.util.*;
import org.apache.ant.core.execution.*;

/**
 * The abstract FileSetInfo performs all of the name matching and mapping operations
 * common to FileSetInfo classes.
 */
public abstract class AbstractScanner implements FileSetScanner {
    /** The list of patternSets to process on this directory */
    List patternSets;
    
    /** Indicator for whether default excludes should be applied. */
    boolean useDefaultExcludes;      
    
    /*
     * The patterns for the files that should be included.
     */
    private String[] includes;

    /**
     * The patterns for the files that should be excluded.
     */
    private String[] excludes;

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
        "**/SCCS/**"
    };

    public AbstractScanner(List patternSets, 
                           boolean useDefaultExcludes) throws ExecutionException {
        this.patternSets = patternSets;
        this.useDefaultExcludes = useDefaultExcludes; 
        
        //convert patternsets into excludes
        PatternSet combinedSet = new PatternSet();
        for (Iterator i = patternSets.iterator(); i.hasNext(); ) {
            PatternSet set = (PatternSet)i.next();
            combinedSet.append(set);
        }
        
        String[] includes = combinedSet.getIncludePatterns();
        if (includes == null) {
            // No includes supplied, so set it to 'matches all'
            includes = new String[1];
            includes[0] = "**";
        }
        String[] excludes = combinedSet.getExcludePatterns();
        if (excludes == null) {
            excludes = new String[0];
        }

        setIncludes(includes);
        setExcludes(excludes);
        if (useDefaultExcludes) {
            addDefaultExcludes();
        }
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
    protected void setIncludes(String[] includes) {
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
    protected void setExcludes(String[] excludes) {
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
     * Does the path match the start of this pattern up to the first "**".
     +
     * <p>This is not a general purpose test and should only be used if you
     * can live with false positives.</p>
     *
     * <p><code>pattern=**\\a</code> and <code>str=b</code> will yield true.
     *
     * @param pattern the (non-null) pattern to match against
     * @param str     the (non-null) string (path) to match
     */
    protected static boolean matchPatternStart(String pattern, String str) {
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
            if (!match(patDir,(String)strDirs.elementAt(strIdxStart))) {
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
            if (!match(patDir,(String)strDirs.elementAt(strIdxStart))) {
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
            if (!match(patDir,(String)strDirs.elementAt(strIdxEnd))) {
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
                    if (!match(subPat,subStr)) {
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
                if (ch != '?' && ch != strArr[i]) {
                    return false; // Character mismatch
                }
            }
            return true; // String matches against pattern
        }

        if (patIdxEnd == 0) {
            return true; // Pattern contains only '*', which matches anything
        }

        // Process characters before first star
        while((ch = patArr[patIdxStart]) != '*' && strIdxStart <= strIdxEnd) {
            if (ch != '?' && ch != strArr[strIdxStart]) {
                return false;
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
            if (ch != '?' && ch != strArr[strIdxEnd]) {
                return false;
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
                    if (ch != '?' && ch != strArr[strIdxStart+i+j]) {
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
     * Tests whether a name matches against at least one include pattern.
     *
     * @param name the name to match
     * @return <code>true</code> when the name matches against at least one
     *         include pattern, <code>false</code> otherwise.
     */
    protected boolean isIncluded(String name) {
        for (int i = 0; i < includes.length; i++) {
            if (matchPath(includes[i],name)) {
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
            if (matchPatternStart(includes[i],name)) {
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
            if (matchPath(excludes[i],name)) {
                return true;
            }
        }
        return false;
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
