/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.types.selectors;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.SymbolicLinkUtils;

/**
 * Container for a path that has been split into its components.
 * @since 1.8.0
 */
public class TokenizedPath {

    /**
     * Instance that holds no tokens at all.
     */
    public static final TokenizedPath EMPTY_PATH =
        new TokenizedPath("", new String[0]);

    /** Helper. */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    /** Helper. */
    private static final SymbolicLinkUtils SYMLINK_UTILS =
        SymbolicLinkUtils.getSymbolicLinkUtils();
    /** iterations for case-sensitive scanning. */
    private static final boolean[] CS_SCAN_ONLY = new boolean[] {true};
    /** iterations for non-case-sensitive scanning. */
    private static final boolean[] CS_THEN_NON_CS = new boolean[] {true, false};

    private final String path;
    private final String[] tokenizedPath;

    /**
    * Initialize the TokenizedPath by parsing it.
    * @param path The path to tokenize. Must not be
    *                <code>null</code>.
    */
    public TokenizedPath(String path) {
        this(path, SelectorUtils.tokenizePathAsArray(path));
    }

    /**
     * Creates a new path as a child of another path.
     *
     * @param parent the parent path
     * @param child the child, must not contain the file separator
     */
    public TokenizedPath(TokenizedPath parent, String child) {
        if (parent.path.length() > 0
            && parent.path.charAt(parent.path.length() - 1)
               != File.separatorChar) {
            path = parent.path + File.separatorChar + child;
        } else {
            path = parent.path + child;
        }
        tokenizedPath = new String[parent.tokenizedPath.length + 1];
        System.arraycopy(parent.tokenizedPath, 0, tokenizedPath, 0,
                         parent.tokenizedPath.length);
        tokenizedPath[parent.tokenizedPath.length] = child;
    }

    /* package */ TokenizedPath(String path, String[] tokens) {
        this.path = path;
        this.tokenizedPath = tokens;
    }

    /**
     * @return The original path String
     */
    @Override
    public String toString() {
        return path;
    }

    /**
     * The depth (or length) of a path.
     */
    public int depth() {
        return tokenizedPath.length;
    }

    /* package */ String[] getTokens() {
        return tokenizedPath;
    }

    /**
     * From <code>base</code> traverse the filesystem in order to find
     * a file that matches the given name.
     *
     * @param base base File (dir).
     * @param cs whether to scan case-sensitively.
     * @return File object that points to the file in question or null.
     */
    public File findFile(File base, final boolean cs) {
        String[] tokens = tokenizedPath;
        if (FileUtils.isAbsolutePath(path)) {
            if (base == null) {
                String[] s = FILE_UTILS.dissect(path);
                base = new File(s[0]);
                tokens = SelectorUtils.tokenizePathAsArray(s[1]);
            } else {
                File f = FILE_UTILS.normalize(path);
                String s = FILE_UTILS.removeLeadingPath(base, f);
                if (s.equals(f.getAbsolutePath())) {
                    //removing base from path yields no change; path
                    //not child of base
                    return null;
                }
                tokens = SelectorUtils.tokenizePathAsArray(s);
            }
        }
        return findFile(base, tokens, cs);
    }

    /**
     * Do we have to traverse a symlink when trying to reach path from
     * basedir?
     * @param base base File (dir).
     */
    public boolean isSymlink(File base) {
        for (int i = 0; i < tokenizedPath.length; i++) {
            try {
                if ((base != null
                     && SYMLINK_UTILS.isSymbolicLink(base, tokenizedPath[i]))
                    ||
                    (base == null
                     && SYMLINK_UTILS.isSymbolicLink(tokenizedPath[i]))
                    ) {
                    return true;
                }
                base = new File(base, tokenizedPath[i]);
            } catch (IOException ioe) {
                String msg = "IOException caught while checking "
                    + "for links, couldn't get canonical path!";
                // will be caught and redirected to Ant's logging system
                System.err.println(msg);
            }
        }
        return false;
    }

    /**
     * true if the original paths are equal.
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof TokenizedPath
            && path.equals(((TokenizedPath) o).path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    /**
     * From <code>base</code> traverse the filesystem in order to find
     * a file that matches the given stack of names.
     *
     * @param base base File (dir) - must not be null.
     * @param pathElements array of path elements (dirs...file).
     * @param cs whether to scan case-sensitively.
     * @return File object that points to the file in question or null.
     */
    private static File findFile(File base, final String[] pathElements,
                                 final boolean cs) {
        for (int current = 0; current < pathElements.length; current++) {
            if (!base.isDirectory()) {
                return null;
            }
            String[] files = base.list();
            if (files == null) {
                throw new BuildException("IO error scanning directory %s",
                    base.getAbsolutePath());
            }
            boolean found = false;
            boolean[] matchCase = cs ? CS_SCAN_ONLY : CS_THEN_NON_CS;
            for (int i = 0; !found && i < matchCase.length; i++) {
                for (int j = 0; !found && j < files.length; j++) {
                    if (matchCase[i]
                        ? files[j].equals(pathElements[current])
                        : files[j].equalsIgnoreCase(pathElements[current])) {
                        base = new File(base, files[j]);
                        found = true;
                    }
                }
            }
            if (!found) {
                return null;
            }
        }
        return pathElements.length == 0 && !base.isDirectory() ? null : base;
    }

    /**
     * Creates a TokenizedPattern from the same tokens that make up
     * this path.
     */
    public TokenizedPattern toPattern() {
        return new TokenizedPattern(path, tokenizedPath);
    }

}
