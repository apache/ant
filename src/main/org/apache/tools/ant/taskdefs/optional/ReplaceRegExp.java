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
package org.apache.tools.ant.taskdefs.optional;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.RegularExpression;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.Substitution;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.regexp.Regexp;
import org.apache.tools.ant.util.regexp.RegexpUtil;

/**
 * Performs regular expression string replacements in a text
 * file.  The input file(s) must be able to be properly processed by
 * a Reader instance.  That is, they must be text only, no binary.
 *
 * The syntax of the regular expression depends on the implementation that
 * you choose to use. The system property <code>ant.regexp.regexpimpl</code>
 * will be the classname of the implementation that will be used (the default
 * is <code>org.apache.tools.ant.util.regexp.JakartaOroRegexp</code> and
 * requires the Jakarta Oro Package).
 *
 * <pre>
 * Available implementations:
 *
 *   org.apache.tools.ant.util.regexp.Jdk14RegexpRegexp (default)
 *        Uses Java's built-in regular expression package
 *
 *   org.apache.tools.ant.util.regexp.JakartaOroRegexp
 *        Requires  the jakarta-oro package
 *
 *   org.apache.tools.ant.util.regexp.JakartaRegexpRegexp
 *        Requires the jakarta-regexp package
 *
 * Usage:
 *
 *   Call Syntax:
 *
 *     &lt;replaceregexp file="file"
 *                    match="pattern"
 *                    replace="pattern"
 *                    flags="options"?
 *                    byline="true|false"? &gt;
 *       regexp?
 *       substitution?
 *       fileset*
 *     &lt;/replaceregexp&gt;
 *
 *    NOTE: You must have either the file attribute specified, or at least one fileset subelement
 *    to operation on.  You may not have the file attribute specified if you nest fileset elements
 *    inside this task.  Also, you cannot specify both match and a regular expression subelement at
 *    the same time, nor can you specify the replace attribute and the substitution subelement at
 *    the same time.
 *
 *   Attributes:
 *
 *     file    --&gt; A single file to operation on (mutually exclusive
 *                    with the fileset subelements)
 *     match   --&gt; The Regular expression to match
 *     replace --&gt; The Expression replacement string
 *     flags   --&gt; The options to give to the replacement
 *                 g = Substitute all occurrences. default is to replace only the first one
 *                 i = Case insensitive match
 *
 *     byline  --&gt; Should this file be processed a single line at a time (default is false)
 *                 "true" indicates to perform replacement on a line by line basis
 *                 "false" indicates to perform replacement on the whole file at once.
 *
 *  Example:
 *
 *     The following call could be used to replace an old property name in a ".properties"
 *     file with a new name.  In the replace attribute, you can refer to any part of the
 *     match expression in parenthesis using backslash followed by a number like '\1'.
 *
 *     &lt;replaceregexp file="test.properties"
 *                    match="MyProperty=(.*)"
 *                    replace="NewProperty=\1"
 *                    byline="true" /&gt;
 *
 * </pre>
 *
 */
public class ReplaceRegExp extends Task {

    private File file;
    private String flags;
    private boolean byline;
    private Union resources;
    private RegularExpression regex;
    private Substitution subs;

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private boolean preserveLastModified = false;

    /**
     * Encoding to assume for the files
     */
    private String encoding = null;

    /** Default Constructor  */
    public ReplaceRegExp() {
        super();
        this.file = null;
        this.flags = "";
        this.byline = false;

        this.regex = null;
        this.subs = null;
    }

    /**
     * file for which the regular expression should be replaced;
     * required unless a nested fileset is supplied.
     *
     * @param file The file for which the reg exp should be replaced.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * the regular expression pattern to match in the file(s);
     * required if no nested &lt;regexp&gt; is used
     *
     * @param match the match attribute.
     */
    public void setMatch(String match) {
        if (regex != null) {
            throw new BuildException("Only one regular expression is allowed");
        }

        regex = new RegularExpression();
        regex.setPattern(match);
    }

    /**
     * The substitution pattern to place in the file(s) in place
     * of the regular expression.
     * Required if no nested &lt;substitution&gt; is used
     *
     * @param replace the replace attribute
     */

    public void setReplace(String replace) {
        if (subs != null) {
            throw new BuildException(
                "Only one substitution expression is allowed");
        }

        subs = new Substitution();
        subs.setExpression(replace);
    }

    /**
     * The flags to use when matching the regular expression.  For more
     * information, consult the Perl5 syntax.
     * <ul>
     *  <li>g : Global replacement.  Replace all occurrences found</li>
     *  <li>i : Case Insensitive.  Do not consider case in the match</li>
     *  <li>m : Multiline.  Treat the string as multiple lines of input,
     *         using "^" and "$" as the start or end of any line, respectively,
     *         rather than start or end of string.</li>
     *  <li>s : Singleline.  Treat the string as a single line of input, using
     *        "." to match any character, including a newline, which normally,
     *        it would not match.</li>
     * </ul>
     *
     * @param flags the flags attribute
     */
    public void setFlags(String flags) {
        this.flags = flags;
    }

    /**
     * Process the file(s) one line at a time, executing the replacement
     * on one line at a time.  This is useful if you
     * want to only replace the first occurrence of a regular expression on
     * each line, which is not easy to do when processing the file as a whole.
     * Defaults to <i>false</i>.
     *
     * @param byline the byline attribute as a string
     * @deprecated since 1.6.x.
     *             Use setByLine(boolean).
     */
    @Deprecated
    public void setByLine(String byline) {
        this.byline = Boolean.parseBoolean(byline);
    }

    /**
     * Process the file(s) one line at a time, executing the replacement
     * on one line at a time.  This is useful if you
     * want to only replace the first occurrence of a regular expression on
     * each line, which is not easy to do when processing the file as a whole.
     * Defaults to <i>false</i>.
     *
     * @param byline the byline attribute
     */
    public void setByLine(boolean byline) {
        this.byline = byline;
    }

    /**
     * Specifies the encoding Ant expects the files to be in -
     * defaults to the platforms default encoding.
     *
     * @param encoding the encoding attribute
     * @since Ant 1.6
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * list files to apply the replacement to
     *
     * @param set the fileset element
     */
    public void addFileset(FileSet set) {
        addConfigured(set);
    }

    /**
     * Support arbitrary file system based resource collections.
     *
     * @param rc ResourceCollection
     * @since Ant 1.8.0
     */
    public void addConfigured(ResourceCollection rc) {
        if (!rc.isFilesystemOnly()) {
            throw new BuildException("only filesystem resources are supported");
        }
        if (resources == null) {
            resources = new Union();
        }
        resources.add(rc);
    }

    /**
     * A regular expression.
     * You can use this element to refer to a previously
     * defined regular expression datatype instance
     * @return the regular expression object to be configured as an element
     */
    public RegularExpression createRegexp() {
        if (regex != null) {
            throw new BuildException("Only one regular expression is allowed.");
        }

        regex = new RegularExpression();
        return regex;
    }


    /**
     * A substitution pattern.  You can use this element to refer to a previously
     * defined substitution pattern datatype instance.
     *
     * @return the substitution pattern object to be configured as an element
     */
    public Substitution createSubstitution() {
        if (subs != null) {
            throw new BuildException(
                "Only one substitution expression is allowed");
        }

        subs = new Substitution();
        return subs;
    }

    /**
     * Whether the file timestamp shall be preserved even if the file
     * is modified.
     *
     * @param b boolean
     * @since Ant 1.8.0
     */
    public void setPreserveLastModified(boolean b) {
        preserveLastModified = b;
    }

    /**
     * Invoke a regular expression (r) on a string (input) using
     * substitutions (s) for a matching regex.
     *
     * @param r a regular expression
     * @param s a Substitution
     * @param input the string to do the replacement on
     * @param options The options for the regular expression
     * @return the replacement result
     */
    protected String doReplace(RegularExpression r,
                               Substitution s,
                               String input,
                               int options) {
        String res = input;
        Regexp regexp = r.getRegexp(getProject());

        if (regexp.matches(input, options)) {
            log("Found match; substituting", Project.MSG_DEBUG);
            res = regexp.substitute(input, s.getExpression(getProject()),
                                    options);
        }

        return res;
    }

    /**
     * Perform the replacement on a file
     *
     * @param f the file to perform the replacement on
     * @param options the regular expressions options
     * @exception IOException if an error occurs
     */
    protected void doReplace(File f, int options)
         throws IOException {
        File temp = FILE_UTILS.createTempFile(getProject(), "replace", ".txt", null, true, true);
        try {
            boolean changes = false;

            final Charset charset = encoding == null ? Charset.defaultCharset() : Charset.forName(encoding);
            try (InputStream is = Files.newInputStream(f.toPath());
                 OutputStream os = Files.newOutputStream(temp.toPath())) {
                Reader r = null;
                Writer w = null;
                try {
                    r = new InputStreamReader(is, charset);
                    w = new OutputStreamWriter(os, charset);
                    log("Replacing pattern '" + regex.getPattern(getProject())
                        + "' with '" + subs.getExpression(getProject())
                        + "' in '" + f.getPath() + "'" + (byline ? " by line" : "")
                        + (flags.isEmpty() ? "" : " with flags: '" + flags + "'")
                        + ".", Project.MSG_VERBOSE);

                    if (byline) {
                        r = new BufferedReader(r);
                        w = new BufferedWriter(w);

                        StringBuilder linebuf = new StringBuilder();
                        int c;
                        boolean hasCR = false;

                        do {
                            c = r.read();

                            if (c == '\r') {
                                if (hasCR) {
                                    // second CR -> EOL + possibly empty line
                                    changes |= replaceAndWrite(linebuf.toString(),
                                                               w, options);
                                    w.write('\r');

                                    linebuf = new StringBuilder();
                                    // hasCR is still true (for the second one)
                                } else {
                                    // first CR in this line
                                    hasCR = true;
                                }
                            } else if (c == '\n') {
                                // LF -> EOL
                                changes |= replaceAndWrite(linebuf.toString(),
                                                           w, options);
                                if (hasCR) {
                                    w.write('\r');
                                    hasCR = false;
                                }
                                w.write('\n');

                                linebuf = new StringBuilder();
                            } else { // any other char
                                if (hasCR || c < 0) {
                                    // Mac-style linebreak or EOF (or both)
                                    changes |= replaceAndWrite(linebuf.toString(),
                                                               w, options);
                                    if (hasCR) {
                                        w.write('\r');
                                        hasCR = false;
                                    }

                                    linebuf = new StringBuilder();
                                }

                                if (c >= 0) {
                                    linebuf.append((char) c);
                                }
                            }
                        } while (c >= 0);

                    } else {
                        changes = multilineReplace(r, w, options);
                    }
                } finally {
                    FileUtils.close(r);
                    FileUtils.close(w);
                }
            }
            if (changes) {
                log("File has changed; saving the updated file", Project.MSG_VERBOSE);
                try {
                    long origLastModified = f.lastModified();
                    FILE_UTILS.rename(temp, f);
                    if (preserveLastModified) {
                        FILE_UTILS.setFileLastModified(f, origLastModified);
                    }
                    temp = null;
                } catch (IOException e) {
                    throw new BuildException("Couldn't rename temporary file "
                                             + temp, e, getLocation());
                }
            } else {
                log("No change made", Project.MSG_DEBUG);
            }
        } finally {
            if (temp != null) {
                temp.delete();
            }
        }
    }

    /**
     * Execute the task
     *
     * @throws BuildException is there is a problem in the task execution.
     */
    @Override
    public void execute() throws BuildException {
        if (regex == null) {
            throw new BuildException("No expression to match.");
        }
        if (subs == null) {
            throw new BuildException("Nothing to replace expression with.");
        }

        if (file != null && resources != null) {
            throw new BuildException(
                "You cannot supply the 'file' attribute and resource collections at the same time.");
        }

        int options = RegexpUtil.asOptions(flags);

        if (file != null && file.exists()) {
            try {
                doReplace(file, options);
            } catch (IOException e) {
                log("An error occurred processing file: '"
                    + file.getAbsolutePath() + "': " + e.toString(),
                    Project.MSG_ERR);
            }
        } else if (file != null) {
            log("The following file is missing: '"
                + file.getAbsolutePath() + "'", Project.MSG_ERR);
        }

        if (resources != null) {
            for (Resource r : resources) {
                File f = r.as(FileProvider.class).getFile();

                if (f.exists()) {
                    try {
                        doReplace(f, options);
                    } catch (Exception e) {
                        log("An error occurred processing file: '"
                            + f.getAbsolutePath() + "': " + e.toString(),
                            Project.MSG_ERR);
                    }
                } else {
                    log("The following file is missing: '"
                        + f.getAbsolutePath() + "'", Project.MSG_ERR);
                }
            }
        }
    }

    private boolean multilineReplace(Reader r, Writer w, int options)
        throws IOException {
        return replaceAndWrite(FileUtils.safeReadFully(r), w, options);
    }

    private boolean replaceAndWrite(String s, Writer w, int options)
        throws IOException {
        String res = doReplace(regex, subs, s, options);
        w.write(res);
        return !res.equals(s);
    }
}
