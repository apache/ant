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
package org.apache.tools.ant.taskdefs.optional;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.RegularExpression;
import org.apache.tools.ant.types.Substitution;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.regexp.Regexp;

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
 * For jdk  &lt;= 1.3, there are two available implementations:
 *   org.apache.tools.ant.util.regexp.JakartaOroRegexp (the default)
 *        Requires  the jakarta-oro package
 *
 *   org.apache.tools.ant.util.regexp.JakartaRegexpRegexp
 *        Requires the jakarta-regexp package
 *
 * For jdk &gt;= 1.4 an additional implementation is available:
 *   org.apache.tools.ant.util.regexp.Jdk14RegexpRegexp
 *        Requires the jdk 1.4 built in regular expression package.
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
    private Vector filesets; // Keep jdk 1.1 compliant so others can use this
    private RegularExpression regex;
    private Substitution subs;

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /**
     * Encoding to assume for the files
     */
    private String encoding = null;

    /** Default Constructor  */
    public ReplaceRegExp() {
        super();
        this.file = null;
        this.filesets = new Vector();
        this.flags = "";
        this.byline = false;

        this.regex = null;
        this.subs = null;
    }


    /**
     * file for which the regular expression should be replaced;
     * required unless a nested fileset is supplied.
     * @param file The file for which the reg exp should be replaced.
     */
    public void setFile(File file) {
        this.file = file;
    }


    /**
     * the regular expression pattern to match in the file(s);
     * required if no nested &lt;regexp&gt; is used
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
     * @param replace the replace attribute
     */

    public void setReplace(String replace) {
        if (subs != null) {
            throw new BuildException("Only one substitution expression is "
                                     + "allowed");
        }

        subs = new Substitution();
        subs.setExpression(replace);
    }

    /**
     * The flags to use when matching the regular expression.  For more
     * information, consult the Perl5 syntax.
     * <ul>
     *  <li>g : Global replacement.  Replace all occurrences found
     *  <li>i : Case Insensitive.  Do not consider case in the match
     *  <li>m : Multiline.  Treat the string as multiple lines of input,
     *         using "^" and "$" as the start or end of any line, respectively,
     *         rather than start or end of string.
     *  <li> s : Singleline.  Treat the string as a single line of input, using
     *        "." to match any character, including a newline, which normally,
     *        it would not match.
     *</ul>
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
     * Defaults to <i>false</i>.</td>
     * @param byline the byline attribute as a string
     * @deprecated since 1.6.x.
     *             Use setByLine(boolean).
     */
    public void setByLine(String byline) {
        Boolean res = Boolean.valueOf(byline);

        if (res == null) {
            res = Boolean.FALSE;
        }
        this.byline = res.booleanValue();
    }

    /**
     * Process the file(s) one line at a time, executing the replacement
     * on one line at a time.  This is useful if you
     * want to only replace the first occurrence of a regular expression on
     * each line, which is not easy to do when processing the file as a whole.
     * Defaults to <i>false</i>.</td>
     * @param byline the byline attribute
     */
    public void setByLine(boolean byline) {
        this.byline = byline;
    }


    /**
     * Specifies the encoding Ant expects the files to be in -
     * defaults to the platforms default encoding.
     * @param encoding the encoding attribute
     *
     * @since Ant 1.6
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * list files to apply the replacement to
     * @param set the fileset element
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
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
     * @return the substitution pattern object to be configured as an element
     */
    public Substitution createSubstitution() {
        if (subs != null) {
            throw new BuildException("Only one substitution expression is "
                                     + "allowed");
        }

        subs = new Substitution();
        return subs;
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
     *  Perform the replacement on a file
     *
     * @param f the file to perform the relacement on
     * @param options the regular expressions options
     * @exception IOException if an error occurs
     */
    protected void doReplace(File f, int options)
         throws IOException {
        File temp = FILE_UTILS.createTempFile("replace", ".txt", null);
        temp.deleteOnExit();

        Reader r = null;
        Writer w = null;

        try {
            if (encoding == null) {
                r = new FileReader(f);
                w = new FileWriter(temp);
            } else {
                r = new InputStreamReader(new FileInputStream(f), encoding);
                w = new OutputStreamWriter(new FileOutputStream(temp),
                                           encoding);
            }

            BufferedReader br = new BufferedReader(r);
            BufferedWriter bw = new BufferedWriter(w);
            PrintWriter pw = new PrintWriter(bw);

            boolean changes = false;

            log("Replacing pattern '" + regex.getPattern(getProject())
                + "' with '" + subs.getExpression(getProject())
                + "' in '" + f.getPath() + "'" + (byline ? " by line" : "")
                + (flags.length() > 0 ? " with flags: '" + flags + "'" : "")
                + ".", Project.MSG_VERBOSE);

            if (byline) {
                StringBuffer linebuf = new StringBuffer();
                String line = null;
                String res = null;
                int c;
                boolean hasCR = false;

                do {
                    c = br.read();

                    if (c == '\r') {
                        if (hasCR) {
                            // second CR -> EOL + possibly empty line
                            line = linebuf.toString();
                            res  = doReplace(regex, subs, line, options);

                            if (!res.equals(line)) {
                                changes = true;
                            }

                            pw.print(res);
                            pw.print('\r');

                            linebuf = new StringBuffer();
                            // hasCR is still true (for the second one)
                        } else {
                            // first CR in this line
                            hasCR = true;
                        }
                    } else if (c == '\n') {
                        // LF -> EOL
                        line = linebuf.toString();
                        res  = doReplace(regex, subs, line, options);

                        if (!res.equals(line)) {
                            changes = true;
                        }

                        pw.print(res);
                        if (hasCR) {
                            pw.print('\r');
                            hasCR = false;
                        }
                        pw.print('\n');

                        linebuf = new StringBuffer();
                    } else { // any other char
                        if ((hasCR) || (c < 0)) {
                            // Mac-style linebreak or EOF (or both)
                            line = linebuf.toString();
                            res  = doReplace(regex, subs, line, options);

                            if (!res.equals(line)) {
                                changes = true;
                            }

                            pw.print(res);
                            if (hasCR) {
                                pw.print('\r');
                                hasCR = false;
                            }

                            linebuf = new StringBuffer();
                        }

                        if (c >= 0) {
                            linebuf.append((char) c);
                        }
                    }
                } while (c >= 0);

                pw.flush();
            } else {
                String buf = FileUtils.readFully(br);
                if (buf == null) {
                    buf = "";
                }

                String res = doReplace(regex, subs, buf, options);

                if (!res.equals(buf)) {
                    changes = true;
                }

                pw.print(res);
                pw.flush();
            }

            r.close();
            r = null;
            w.close();
            w = null;

            if (changes) {
                log("File has changed; saving the updated file", Project.MSG_VERBOSE);
                try {
                    FILE_UTILS.rename(temp, f);
                    temp = null;
                } catch (IOException e) {
                    throw new BuildException("Couldn't rename temporary file "
                                             + temp, getLocation());
                }
            } else {
                log("No change made", Project.MSG_DEBUG);
            }
        } finally {
            FileUtils.close(r);
            FileUtils.close(w);
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
    public void execute() throws BuildException {
        if (regex == null) {
            throw new BuildException("No expression to match.");
        }
        if (subs == null) {
            throw new BuildException("Nothing to replace expression with.");
        }

        if (file != null && filesets.size() > 0) {
            throw new BuildException("You cannot supply the 'file' attribute "
                                     + "and filesets at the same time.");
        }

        int options = 0;

        if (flags.indexOf('g') != -1) {
            options |= Regexp.REPLACE_ALL;
        }

        if (flags.indexOf('i') != -1) {
            options |= Regexp.MATCH_CASE_INSENSITIVE;
        }

        if (flags.indexOf('m') != -1) {
            options |= Regexp.MATCH_MULTILINE;
        }

        if (flags.indexOf('s') != -1) {
            options |= Regexp.MATCH_SINGLELINE;
        }

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

        int sz = filesets.size();

        for (int i = 0; i < sz; i++) {
            FileSet fs = (FileSet) (filesets.elementAt(i));
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());

            String[] files = ds.getIncludedFiles();

            for (int j = 0; j < files.length; j++) {
                File f = new File(fs.getDir(getProject()), files[j]);

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

}


