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
package org.apache.tools.ant.taskdefs.optional;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
 * The syntax of the regular expression depends on the implemtation that
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
 *     file    --&gt; A single file to operation on (mutually exclusive with the fileset subelements)
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
 * @author <a href="mailto:mattinger@mindless.com">Matthew Inger</a>
 */
public class ReplaceRegExp extends Task {

    private File file;
    private String flags;
    private boolean byline;
    private Vector filesets;// Keep jdk 1.1 compliant so others can use this
    private RegularExpression regex;
    private Substitution subs;

    private FileUtils fileUtils = FileUtils.newFileUtils();


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
     */
    public void setFile(File file) {
        this.file = file;
    }


    /**
     * the regular expression pattern to match in the file(s);
     * required if no nested &lt;regexp&gt; is used
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
     *  <li>g : Global replacement.  Replace all occurences found
     *  <li>i : Case Insensitive.  Do not consider case in the match
     *  <li>m : Multiline.  Treat the string as multiple lines of input, 
     *         using "^" and "$" as the start or end of any line, respectively, rather than start or end of string.
     *  <li> s : Singleline.  Treat the string as a single line of input, using
     *        "." to match any character, including a newline, which normally, it would not match.
     *</ul>
     */                     
    public void setFlags(String flags) {
        this.flags = flags;
    }


    /**
     * Process the file(s) one line at a time, executing the replacement
     * on one line at a time.  This is useful if you
     * want to only replace the first occurence of a regular expression on
     * each line, which is not easy to do when processing the file as a whole.
     * Defaults to <i>false</i>.</td>
     */
    public void setByLine(String byline) {
        Boolean res = Boolean.valueOf(byline);

        if (res == null) {
            res = Boolean.FALSE;
        }
        this.byline = res.booleanValue();
    }


    /**
     * list files to apply the replacement to
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }


    /**
     * A regular expression.
     * You can use this element to refer to a previously
     * defined regular expression datatype instance
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
     */
    public Substitution createSubstitution() {
        if (subs != null) {
            throw new BuildException("Only one substitution expression is "
                                     + "allowed");
        }

        subs = new Substitution();
        return subs;
    }


    protected String doReplace(RegularExpression r,
                               Substitution s,
                               String input,
                               int options) {
        String res = input;
        Regexp regexp = r.getRegexp(getProject());

        if (regexp.matches(input, options)) {
            res = regexp.substitute(input, s.getExpression(getProject()), 
                                    options);
        }

        return res;
    }


    /** Perform the replace on the entire file  */
    protected void doReplace(File f, int options)
         throws IOException {
        File parentDir = fileUtils.getParentFile(f);
        File temp = fileUtils.createTempFile("replace", ".txt", parentDir);

        FileReader r = null;
        FileWriter w = null;

        try {
            r = new FileReader(f);
            w = new FileWriter(temp);

            BufferedReader br = new BufferedReader(r);
            BufferedWriter bw = new BufferedWriter(w);
            PrintWriter pw = new PrintWriter(bw);

            boolean changes = false;

            log("Replacing pattern '" + regex.getPattern(getProject()) +
                "' with '" + subs.getExpression(getProject()) +
                "' in '" + f.getPath() + "'" +
                (byline ? " by line" : "") +
                (flags.length() > 0 ? " with flags: '" + flags + "'" : "") +
                ".",
                Project.MSG_VERBOSE);

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

                            linebuf.setLength(0);
                            // hasCR is still true (for the second one)
                        } else {
                            // first CR in this line
                            hasCR = true;
                        }
                    }
                    else if (c == '\n') {
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

                        linebuf.setLength(0);
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

                            linebuf.setLength(0);
                        }

                        if (c >= 0) {
                            linebuf.append((char) c);
                        }
                    }
                } while (c >= 0);

                pw.flush();
            } else {
                int flen = (int) f.length();
                char tmpBuf[] = new char[flen];
                int numread = 0;
                int totread = 0;

                while (numread != -1 && totread < flen) {
                    numread = br.read(tmpBuf, totread, flen);
                    totread += numread;
                }

                String buf = new String(tmpBuf);

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
                if (!f.delete()) {
                    throw new BuildException("Couldn't delete " + f,
                                             getLocation());
                }
                if (!temp.renameTo(f)) {
                    throw new BuildException("Couldn't rename temporary file " 
                                             + temp, getLocation());
                }
                temp = null;
            }
        } finally {
            try {
                if (r != null) {
                    r.close();
                }
            } catch (Exception e) {
            }

            try {
                if (w != null) {
                    w.close();
                }
            } catch (Exception e) {
            }
            if (temp != null) {
                temp.delete();
            }
        }
    }


    public void execute()
         throws BuildException {
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

            String files[] = ds.getIncludedFiles();

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


