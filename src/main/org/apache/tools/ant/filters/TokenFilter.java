/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.filters;

import java.io.IOException;
import java.io.Reader;
import java.util.Vector;
import java.util.Enumeration;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.types.RegularExpression;
import org.apache.tools.ant.types.Substitution;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.regexp.Regexp;

/**
 * This splits up input into tokens and passes
 * the tokens to a sequence of filters.
 *
 * @author Peter Reilly
 * @since Ant 1.6
 * @see BaseFilterReader
 * @see ChainableReader
 * @see DynamicConfigurator
 */
public class TokenFilter
    extends BaseFilterReader
    implements ChainableReader
{
    /**
     * input stream tokenizers implement this interface
     */
    public interface Tokenizer {
        /**
         * get the next token from the input stream
         * @param in the input stream
         * @return the next token, or null for the end
         *         of the stream
         */
        public String getToken(Reader in)
            throws IOException;
        /**
         * return the string between tokens, after the
         * previous token.
         * @return the intra-token string
         */
        public String getPostToken();
    }

    /**
     * string filters implement this interface
     */
    public interface Filter {
        /**
         * filter and/of modify a string
         *
         * @param filter the string to filter
         * @return the modified string or null if the
         *         string did not pass the filter
         */
        public String filter(String string);
    }


    /** string filters */
    private Vector    filters   = new Vector();
    /** the tokenizer to use on the input stream */
    private Tokenizer tokenizer = null;
    /** the output token termination */
    private String    delimOutput = null;
    /** the current string token from the input stream */
    private String    line      = null;
    /** the position in the current string token */
    private int       linePos   = 0;

    /**
     * Constructor for "dummy" instances.
     *
     * @see BaseFilterReader#BaseFilterReader()
     */
    public TokenFilter() {
        super();
    }

    /**
     * Creates a new filtered reader.
     *
     * @param in A Reader object providing the underlying stream.
     *           Must not be <code>null</code>.
     */
    public TokenFilter(final Reader in) {
        super(in);
    }


    /**
     * Returns the next character in the filtered stream, only including
     * lines from the original stream which match all of the specified
     * regular expressions.
     *
     * @return the next character in the resulting stream, or -1
     * if the end of the resulting stream has been reached
     *
     * @exception IOException if the underlying stream throws an IOException
     * during reading
     */

    public int read() throws IOException {
        if (tokenizer == null)
            tokenizer = new LineTokenizer();

        while (line == null || line.length() == 0) {
            line = tokenizer.getToken(in);
            if (line == null)
                return -1;
            for (Enumeration e = filters.elements(); e.hasMoreElements();)
            {
                Filter filter = (Filter) e.nextElement();
                line = filter.filter(line);
                if (line == null)
                    break;
            }
            linePos = 0;
            if (line != null) {
                if (tokenizer.getPostToken().length() != 0) {
                    if (delimOutput != null)
                        line = line + delimOutput;
                    else
                        line = line + tokenizer.getPostToken();
                }
            }
        }
        int ch = line.charAt(linePos);
        linePos ++;
        if (linePos == line.length())
            line = null;
        return ch;
    }

    /**
     * Creates a new TokenFilter using the passed in
     * Reader for instantiation.
     *
     * @param reader A Reader object providing the underlying stream.
     *
     * @return a new filter based on this configuration
     */

    public final Reader chain(final Reader reader) {
        TokenFilter newFilter = new TokenFilter(reader);
        newFilter.filters = filters;
        newFilter.tokenizer = tokenizer;
        newFilter.delimOutput = delimOutput;
        newFilter.setProject(getProject());
        return newFilter;
    }

    /**
     * set the output delimitor.
     * @param delimOutput replaces the delim string returned by the
     *                    tokenizer, it it present.
     */

    public void setDelimOutput(String delimOutput) {
        this.delimOutput = resolveBackSlash(delimOutput);
    }

    // -----------------------------------------
    //  Predefined tokenizers
    // -----------------------------------------

    /**
     * add a line tokenizer - this is the default.
     */

    public void addLineTokenizer(LineTokenizer tokenizer) {
        add(tokenizer);
    }

    /**
     * add a string tokenizer
     */

    public void addStringTokenizer(StringTokenizer tokenizer) {
        add(tokenizer);
    }

    /**
     *  add a file tokenizer
     */
    public void addFileTokenizer(FileTokenizer tokenizer) {
        add(tokenizer);
    }

    /**
     * add a tokenizer
     */

    public void add(Tokenizer tokenizer) {
        if (this.tokenizer != null)
            throw new BuildException("Only one tokenizer allowed");
        this.tokenizer = tokenizer;
    }

    // -----------------------------------------
    //  Predefined filters
    // -----------------------------------------

    /** replace string filter */
    public void addReplaceString(ReplaceString filter) {
        filters.addElement(filter);
    }

    /** contains string filter */
    public void addContainsString(ContainsString filter) {
        filters.addElement(filter);
    }

    /** replace regex filter */
    public void addReplaceRegex(ReplaceRegex filter) {
        filters.addElement(filter);
    }

    /** contains regex filter */
    public void addContainsRegex(ContainsRegex filter) {
        filters.addElement(filter);
    }

    /** trim filter */
    public void addTrim(Trim filter) {
        filters.addElement(filter);
    }

    /** ignore blank filter */
    public void addIgnoreBlank(IgnoreBlank filter) {
        filters.addElement(filter);
    }

    /** delete chars */
    public void addDeleteCharacters(DeleteCharacters filter) {
        filters.addElement(filter);
    }

    public void add(Filter filter) {
        filters.addElement(filter);
    }


    // --------------------------------------------
    //
    //      Tokenizer Classes
    //
    // --------------------------------------------

    /**
     * class to read the complete input into a string
     */
    public static class FileTokenizer
        extends ProjectComponent
        implements Tokenizer
    {
        /**
         * Get the complete input as a string
         *
         * @return the complete input
         */
        public String getToken(Reader in)
            throws IOException
        {
            return FileUtils.readFully(in);
        }

        /**
         * Return an empty string
         *
         * @return an empty string
         */
        public String getPostToken() {
            return "";
        }
    }


    /**
     * class to tokenize the input as lines seperated
     * by \r (mac style), \r\n (dos/windows style) or \n (unix style)
     */
    public static class LineTokenizer
        extends ProjectComponent
        implements Tokenizer
    {
        private String  lineEnd = "";
        private int     pushed = -2;
        private boolean includeDelims = false;

        /**
         * attribute includedelims - whether to include
         * the line ending with the line, or to return
         * it in the posttoken
         */

        public void setIncludeDelims(boolean includeDelims) {
            this.includeDelims = includeDelims;
        }

        public String getToken(Reader in)
            throws IOException
        {
            int ch = -1;
            if (pushed != -2) {
                ch = pushed;
                pushed = -2;
            }
            else
                ch = in.read();
            if (ch == -1) {
                return null;
            }

            lineEnd = "";
            StringBuffer line = new StringBuffer();

            int state = 0;
            while (ch != -1) {
                if (state == 0) {
                    if (ch == '\r') {
                        state = 1;
                    }
                    else if (ch == '\n') {
                        lineEnd = "\n";
                        break;
                    }
                    else {
                        line.append((char) ch);
                    }
                }
                else {
                    state = 0;
                    if (ch == '\n') {
                        lineEnd = "\r\n";
                    }
                    else {
                        pushed = ch;
                        lineEnd = "\r";
                    }
                    break;
                }
                ch = in.read();
            }
            if (ch == -1 && state == 1) {
                lineEnd = "\r";
            }

            if (includeDelims) {
                line.append(lineEnd);
            }
            return line.toString();
        }

        public String getPostToken() {
            if (includeDelims) {
                return "";
            }
            return lineEnd;
        }

    }

    /**
     * class to tokenize the input as areas seperated
     * by white space, or by a specified list of
     * delim characters. Behaves like java.util.StringTokenizer.
     * if the stream starts with delim characters, the first
     * token will be an empty string (unless the treat tokens
     * as delims flag is set).
     */
    public static class StringTokenizer
        extends ProjectComponent
        implements Tokenizer
    {
        private String intraString = "";
        private int    pushed = -2;
        private char[] delims = null;
        private boolean delimsAreTokens = false;
        private boolean suppressDelims = false;
        private boolean includeDelims = false;

        /**
         * attribute delims - the delimeter characters
         */
        public void setDelims(String delims) {
            this.delims = resolveBackSlash(delims).toCharArray();
        }

        /**
         * attribute delimsaretokens - treat delimiters as
         * separate tokens.
         */

        public void setDelimsAreTokens(boolean delimsAreTokens) {
            this.delimsAreTokens = delimsAreTokens;
        }
        /**
         * attribute suppressdelims - suppress delimiters.
         * default - false
         */
        public void setSuppressDelims(boolean suppressDelims) {
            this.suppressDelims = suppressDelims;
        }

        /**
         * attribute includedelims - treat delimiters as part
         * of the token.
         * default - false
         */
        public void setIncludeDelims(boolean includeDelims) {
            this.includeDelims = includeDelims;
        }

        public String getToken(Reader in)
            throws IOException
        {
            int ch = -1;
            if (pushed != -2) {
                ch = pushed;
                pushed = -2;
            }
            else
                ch = in.read();
            if (ch == -1) {
                return null;
            }
            boolean inToken = true;
            intraString = "";
            StringBuffer word = new StringBuffer();
            StringBuffer padding = new StringBuffer();
            while (ch != -1) {
                char c = (char) ch;
                boolean isDelim = isDelim(c);
                if (inToken) {
                    if (isDelim) {
                        if (delimsAreTokens) {
                            if (word.length() == 0) {
                                word.append(c);
                            }
                            else {
                                pushed = ch;
                            }
                            break;
                        }
                        padding.append(c);
                        inToken = false;
                    }
                    else
                        word.append(c);
                }
                else {
                    if (isDelim) {
                        padding.append(c);
                    }
                    else {
                        pushed = ch;
                        break;
                    }
                }
                ch = in.read();
            }
            intraString = padding.toString();
            if (includeDelims) {
                word.append(intraString);
            }
            return word.toString();
        }

        public String getPostToken() {
            if (suppressDelims || includeDelims)
                return "";
            return intraString;
        }

        private boolean isDelim(char ch) {
            if (delims == null)
                return Character.isWhitespace(ch);
            for (int i = 0; i < delims.length; ++i)
                if (delims[i] == ch)
                    return true;
            return false;
        }
    }

    // --------------------------------------------
    //
    //      Filter classes
    //
    // --------------------------------------------

    public static abstract class ChainableReaderFilter
        extends ProjectComponent
        implements ChainableReader, Filter
    {
        private boolean byLine = true;

        public void setByLine(boolean byLine) {
            this.byLine = byLine;
        }

        public Reader chain(Reader reader) {
            TokenFilter tokenFilter = new TokenFilter(reader);
            if (!byLine)
                tokenFilter.add(new FileTokenizer());
            tokenFilter.add(this);
            return tokenFilter;
        }
    }

    /**
     * Simple replace string filter.
     */
    public static class ReplaceString
        extends ChainableReaderFilter
    {
        private String from;
        private String to;

        public void setFrom(String from) {
            this.from = from;
        }
        public void setTo(String to) {
            this.to = to;
        }

        /**
         * CAP from the Replace task
         */
        public String filter(String line) {
            if (from == null)
                throw new BuildException("Missing from in stringreplace");
            StringBuffer ret = new StringBuffer();
            int start = 0;
            int found = line.indexOf(from);
            while (found >= 0) {
                // write everything up to the from
                if (found > start) {
                    ret.append(line.substring(start, found));
                }

                // write the replacement to
                if (to != null) {
                    ret.append(to);
                }

                // search again
                start = found + from.length();
                found = line.indexOf(line, start);
            }

            // write the remaining characters
            if (line.length() > start) {
                ret.append(line.substring(start, line.length()));
            }

            return ret.toString();
        }
    }

    /**
     * Simple filter to filter lines contains strings
     */
    public static class ContainsString
        extends ProjectComponent
        implements Filter
    {
        private String contains;

        public void setContains(String contains) {
            this.contains = contains;
        }

        public String filter(String line) {
            if (contains == null)
                throw new BuildException("Missing contains in containsstring");
            if (line.indexOf(contains) > -1)
                return line;
            return null;
        }
    }

    /**
     * filter to replace regex.
     */
    public static class ReplaceRegex
        extends ChainableReaderFilter
    {
        private String             from;
        private String             to;
        private Project            project;
        private RegularExpression  regularExpression;
        private Substitution       substitution;
        private boolean            initialized = false;
        private String             flags = "";
        private int                options;
        private Regexp             regexp;


        public void setPattern(String from) {
            this.from = from;
        }
        public void setReplace(String to) {
            this.to = to;
        }

        public void setProject(Project p) {
            this.project = p;
        }

        public void setFlags(String flags) {
            this.flags = flags;
        }

        private void initialize() {
            if (initialized)
                return;
            options = convertRegexOptions(flags);
            if (from == null)
                throw new BuildException("Missing pattern in replaceregex");
            regularExpression = new RegularExpression();
            regularExpression.setPattern(from);
            regexp = regularExpression.getRegexp(project);
            if (to == null)
                to = "";
            substitution = new Substitution();
            substitution.setExpression(to);
        }

        public String filter(String line) {
            initialize();

            if (!regexp.matches(line, options)) {
                return line;
            }
            return regexp.substitute(
                line, substitution.getExpression(project), options);
        }
    }

    /**
     * filter to filter tokens matching regular expressions.
     */
    public static class ContainsRegex
        extends ChainableReaderFilter
    {
        private String             from;
        private String             to;
        private Project            project;
        private RegularExpression  regularExpression;
        private Substitution       substitution;
        private boolean            initialized = false;
        private String             flags = "";
        private int                options;
        private Regexp             regexp;


        public void setPattern(String from) {
            this.from = from;
        }
        public void setReplace(String to) {
            this.to = to;
        }

        public void setProject(Project p) {
            this.project = p;
        }

        public void setFlags(String flags) {
            this.flags = flags;
        }

        private void initialize() {
            if (initialized)
                return;
            options = convertRegexOptions(flags);
            if (from == null)
                throw new BuildException("Missing from in containsregex");
            regularExpression = new RegularExpression();
            regularExpression.setPattern(from);
            regexp = regularExpression.getRegexp(project);
            if (to == null)
                return;
            substitution = new Substitution();
            substitution.setExpression(to);
       }

        public String filter(String line) {
            initialize();


            if (!regexp.matches(line, options)) {
                return null;
            }
            if (substitution == null)
                return line;
            return regexp.substitute(
                line, substitution.getExpression(project), options);
        }
    }

    /** Filter to trim white space */
    public static class Trim
        extends ChainableReaderFilter
    {
        public String filter(String line) {
            return line.trim();
        }
    }



    /** Filter remove empty tokens */
    public static class IgnoreBlank
        extends ChainableReaderFilter
    {
        public String filter(String line) {
            if (line.trim().length() == 0)
                return null;
            return line;
        }
    }

    /**
     * Filter to delete characters
     */
    public static class DeleteCharacters
        extends ProjectComponent
        implements Filter, ChainableReader
    {
        // Attributes
        /** the list of characters to remove from the input */
        private String deleteChars = "";

        /** Set the list of characters to delete */
        public void setChars(String deleteChars) {
            this.deleteChars = resolveBackSlash(deleteChars);
        }

        /** remove characters from a string */
        public String filter(String string) {
            StringBuffer output = new StringBuffer(string.length());
            for (int i = 0; i < string.length(); ++i) {
                char ch = string.charAt(i);
                if (! isDeleteCharacter(ch))
                    output.append(ch);
            }
            return output.toString();
        }

        /**
         * factory method to provide a reader that removes
         * the characters from a reader as part of a filter
         * chain
         */
        public Reader chain(Reader reader) {
            return new BaseFilterReader(reader) {
                public int read()
                    throws IOException
                {
                    while (true) {
                        int c = in.read();
                        if (c == -1)
                            return c;
                        if (! isDeleteCharacter((char) c))
                            return c;
                    }
                }
            };
        }

        /** check if the character c is to be deleted */
        private boolean isDeleteCharacter(char c) {
            for (int d = 0; d < deleteChars.length(); ++d) {
                if (deleteChars.charAt(d) ==  c) {
                    return true;
                }
            }
            return false;
        }
    }

    // --------------------------------------------------------
    //  static utility methods - could be placed somewhere else
    // --------------------------------------------------------

    /**
     * xml does not do "c" like interpetation of strings.
     * i.e. \n\r\t etc.
     * this methid processes \n, \r, \t, \f, \\
     * also subs \s -> " \n\r\t\f"
     * a trailing '\' will be ignored
     *
     * @param input raw string with possible embedded '\'s
     * @return converted string
     */
    public static String resolveBackSlash(String input) {
        StringBuffer b = new StringBuffer();
        boolean backSlashSeen = false;
        for (int i = 0; i < input.length(); ++i) {
            char c = input.charAt(i);
            if (! backSlashSeen) {
                if (c == '\\')
                    backSlashSeen = true;
                else
                    b.append(c);
            }
            else {
                switch (c) {
                    case '\\':
                        b.append((char) '\\');
                        break;
                    case 'n':
                        b.append((char) '\n');
                        break;
                    case 'r':
                        b.append((char) '\r');
                        break;
                    case 't':
                        b.append((char) '\t');
                        break;
                    case 'f':
                        b.append((char) '\f');
                        break;
                    case 's':
                        b.append(" \t\n\r\f");
                        break;
                    default:
                        b.append(c);
                }
                backSlashSeen = false;
            }
        }
        return b.toString();
    }

    /**
     * convert regex option flag characters to regex options
     * <dl>
     *   <li>g -  Regexp.REPLACE_ALL</li>
     *   <li>i -  Regexp.MATCH_CASE_INSENSITIVE</li>
     *   <li>m -  Regexp.MATCH_MULTILINE</li>
     *   <li>s -  Regexp.MATCH_SINGLELINE</li>
     * </dl>
     */
    public static int convertRegexOptions(String flags) {
        if (flags == null)
            return 0;
        int options = 0;
        if (flags.indexOf('g') != -1)
            options |= Regexp.REPLACE_ALL;
        if (flags.indexOf('i') != -1)
            options |= Regexp.MATCH_CASE_INSENSITIVE;
        if (flags.indexOf('m') != -1)
            options |= Regexp.MATCH_MULTILINE;
        if (flags.indexOf('s') != -1)
            options |= Regexp.MATCH_SINGLELINE;
        return options;
    }
}
