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
package org.apache.tools.ant.filters;

import java.io.IOException;
import java.io.Reader;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.types.RegularExpression;
import org.apache.tools.ant.types.Substitution;
import org.apache.tools.ant.util.LineTokenizer;
import org.apache.tools.ant.util.StringUtils;
import org.apache.tools.ant.util.Tokenizer;
import org.apache.tools.ant.util.regexp.Regexp;
import org.apache.tools.ant.util.regexp.RegexpUtil;

/**
 * This splits up input into tokens and passes
 * the tokens to a sequence of filters.
 *
 * @since Ant 1.6
 * @see BaseFilterReader
 * @see ChainableReader
 * @see org.apache.tools.ant.DynamicConfigurator
 */
public class TokenFilter extends BaseFilterReader
    implements ChainableReader {
    /**
     * string filters implement this interface
     */
    public interface Filter {
        /**
         * filter and/of modify a string
         *
         * @param string the string to filter
         * @return the modified string or null if the
         *         string did not pass the filter
         */
        String filter(String string);
    }


    /** string filters */
    private Vector<Filter> filters = new Vector<>();
    /** the tokenizer to use on the input stream */
    private Tokenizer tokenizer = null;
    /** the output token termination */
    private String delimOutput = null;
    /** the current string token from the input stream */
    private String line    = null;
    /** the position in the current string token */
    private int    linePos = 0;

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
        if (tokenizer == null) {
            tokenizer = new LineTokenizer();
        }
        while (line == null || line.isEmpty()) {
            line = tokenizer.getToken(in);
            if (line == null) {
                return -1;
            }
            for (Filter filter : filters) {
                line = filter.filter(line);
                if (line == null) {
                    break;
                }
            }
            linePos = 0;
            if (line != null && !tokenizer.getPostToken().isEmpty()) {
                if (delimOutput != null) {
                    line += delimOutput;
                } else {
                    line += tokenizer.getPostToken();
                }
            }
        }
        int ch = line.charAt(linePos);
        linePos++;
        if (linePos == line.length()) {
            line = null;
        }
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
     * set the output delimiter.
     * @param delimOutput replaces the delim string returned by the
     *                    tokenizer, if present.
     */

    public void setDelimOutput(String delimOutput) {
        this.delimOutput = resolveBackSlash(delimOutput);
    }

    // -----------------------------------------
    //  Predefined tokenizers
    // -----------------------------------------

    /**
     * add a line tokenizer - this is the default.
     * @param tokenizer the line tokenizer
     */

    public void addLineTokenizer(LineTokenizer tokenizer) {
        add(tokenizer);
    }

    /**
     * add a string tokenizer
     * @param tokenizer the string tokenizer
     */

    public void addStringTokenizer(StringTokenizer tokenizer) {
        add(tokenizer);
    }

    /**
     * add a file tokenizer
     * @param tokenizer the file tokenizer
     */
    public void addFileTokenizer(FileTokenizer tokenizer) {
        add(tokenizer);
    }

    /**
     * add an arbitrary tokenizer
     * @param tokenizer the tokenizer to all, only one allowed
     */

    public void add(Tokenizer tokenizer) {
        if (this.tokenizer != null) {
            throw new BuildException("Only one tokenizer allowed");
        }
        this.tokenizer = tokenizer;
    }

    // -----------------------------------------
    //  Predefined filters
    // -----------------------------------------

    /**
     * replace string filter
     * @param filter the replace string filter
     */
    public void addReplaceString(ReplaceString filter) {
        filters.addElement(filter);
    }

    /**
     * contains string filter
     * @param filter the contains string filter
     */
    public void addContainsString(ContainsString filter) {
        filters.addElement(filter);
    }

    /**
     * replace regex filter
     * @param filter the replace regex filter
     */
    public void addReplaceRegex(ReplaceRegex filter) {
        filters.addElement(filter);
    }

    /**
     * contains regex filter
     * @param filter the contains regex filter
     */
    public void addContainsRegex(ContainsRegex filter) {
        filters.addElement(filter);
    }

    /**
     * trim filter
     * @param filter the trim filter
     */
    public void addTrim(Trim filter) {
        filters.addElement(filter);
    }

    /**
     * ignore blank filter
     * @param filter the ignore blank filter
     */
    public void addIgnoreBlank(IgnoreBlank filter) {
        filters.addElement(filter);
    }

    /**
     * delete chars
     * @param filter the delete characters filter
     */
    public void addDeleteCharacters(DeleteCharacters filter) {
        filters.addElement(filter);
    }

    /**
     * Add an arbitrary filter
     * @param filter the filter to add
     */
    public void add(Filter filter) {
        filters.addElement(filter);
    }


    // --------------------------------------------
    //
    //      Tokenizer Classes (impls moved to oata.util)
    //
    // --------------------------------------------

    /**
     * class to read the complete input into a string
     */
    public static class FileTokenizer
        extends org.apache.tools.ant.util.FileTokenizer {
    }

    /**
     * class to tokenize the input as areas separated
     * by white space, or by a specified list of
     * delim characters. Behaves like java.util.StringTokenizer.
     * if the stream starts with delim characters, the first
     * token will be an empty string (unless the treat delims
     * as tokens flag is set).
     */
    public static class StringTokenizer
        extends org.apache.tools.ant.util.StringTokenizer {
    }

    // --------------------------------------------
    //
    //      Filter classes
    //
    // --------------------------------------------

    /**
     * Abstract class that converts derived filter classes into
     * ChainableReaderFilter's
     */
    public abstract static class ChainableReaderFilter extends ProjectComponent
        implements ChainableReader, Filter {
        private boolean byLine = true;

        /**
         * set whether to use filetokenizer or line tokenizer
         * @param byLine if true use a linetokenizer (default) otherwise
         *               use a filetokenizer
         */
        public void setByLine(boolean byLine) {
            this.byLine = byLine;
        }

        /**
         * Chain a tokenfilter reader to a reader,
         *
         * @param reader the input reader object
         * @return the chained reader object
         */
        public Reader chain(Reader reader) {
            TokenFilter tokenFilter = new TokenFilter(reader);
            if (!byLine) {
                tokenFilter.add(new FileTokenizer());
            }
            tokenFilter.add(this);
            return tokenFilter;
        }
    }

    /**
     * Simple replace string filter.
     */
    public static class ReplaceString extends ChainableReaderFilter {
        private String from;
        private String to;

        /**
         * the from attribute
         *
         * @param from the string to replace
         */
        public void setFrom(String from) {
            this.from = from;
        }

        /**
         * the to attribute
         *
         * @param to the string to replace 'from' with
         */
        public void setTo(String to) {
            this.to = to;
        }

        /**
         * Filter a string 'line' replacing from with to
         * (Copy&amp;Paste from the Replace task)
         * @param line the string to be filtered
         * @return the filtered line
         */
        public String filter(String line) {
            if (from == null) {
                throw new BuildException("Missing from in stringreplace");
            }
            final StringBuilder ret = new StringBuilder();
            int start = 0;
            int found = line.indexOf(from);
            while (found >= 0) {
                // write everything up to the from
                if (found > start) {
                    ret.append(line, start, found);
                }

                // write the replacement to
                if (to != null) {
                    ret.append(to);
                }

                // search again
                start = found + from.length();
                found = line.indexOf(from, start);
            }

            // write the remaining characters
            if (line.length() > start) {
                ret.append(line, start, line.length());
            }

            return ret.toString();
        }
    }

    /**
     * Simple filter to filter lines contains strings
     */
    public static class ContainsString extends ProjectComponent
        implements Filter {
        private String contains;

        /**
         * the contains attribute
         * @param contains the string that the token should contain
         */
        public void setContains(String contains) {
            this.contains = contains;
        }

        /**
         * Filter strings that contain the contains attribute
         *
         * @param string the string to be filtered
         * @return null if the string does not contain "contains",
         *              string otherwise
         */
        public String filter(String string) {
            if (contains == null) {
                throw new BuildException("Missing contains in containsstring");
            }
            if (string.contains(contains)) {
                return string;
            }
            return null;
        }
    }

    /**
     * filter to replace regex.
     */
    public static class ReplaceRegex extends ChainableReaderFilter {
        private String             from;
        private String             to;
        private RegularExpression  regularExpression;
        private Substitution       substitution;
        private boolean            initialized = false;
        private String             flags = "";
        private int                options;
        private Regexp             regexp;

        /**
         * the from attribute
         * @param from the regex string
         */
        public void setPattern(String from) {
            this.from = from;
        }
        /**
         * the to attribute
         * @param to the replacement string
         */
        public void setReplace(String to) {
            this.to = to;
        }

        /**
         * @param flags the regex flags
         */
        public void setFlags(String flags) {
            this.flags = flags;
        }

        private void initialize() {
            if (initialized) {
                return;
            }
            options = convertRegexOptions(flags);
            if (from == null) {
                throw new BuildException("Missing pattern in replaceregex");
            }
            regularExpression = new RegularExpression();
            regularExpression.setPattern(from);
            regexp = regularExpression.getRegexp(getProject());
            if (to == null) {
                to = "";
            }
            substitution = new Substitution();
            substitution.setExpression(to);
        }

        /**
         * @param line the string to modify
         * @return the modified string
         */
        public String filter(String line) {
            initialize();

            if (!regexp.matches(line, options)) {
                return line;
            }
            return regexp.substitute(
                line, substitution.getExpression(getProject()), options);
        }
    }

    /**
     * filter to filter tokens matching regular expressions.
     */
    public static class ContainsRegex extends ChainableReaderFilter {
        private String             from;
        private String             to;
        private RegularExpression  regularExpression;
        private Substitution       substitution;
        private boolean            initialized = false;
        private String             flags = "";
        private int                options;
        private Regexp             regexp;


        /**
         * @param from the regex pattern
         */
        public void setPattern(String from) {
            this.from = from;
        }

        /**
         * @param to the replacement string
         */
        public void setReplace(String to) {
            this.to = to;
        }

        /**
         * @param flags the regex flags
         */
        public void setFlags(String flags) {
            this.flags = flags;
        }

        private void initialize() {
            if (initialized) {
                return;
            }
            options = convertRegexOptions(flags);
            if (from == null) {
                throw new BuildException("Missing from in containsregex");
            }
            regularExpression = new RegularExpression();
            regularExpression.setPattern(from);
            regexp = regularExpression.getRegexp(getProject());
            if (to == null) {
                return;
            }
            substitution = new Substitution();
            substitution.setExpression(to);
        }

        /**
         * apply regex and substitution on a string
         * @param string the string to apply filter on
         * @return the filtered string
         */
        public String filter(String string) {
            initialize();
            if (!regexp.matches(string, options)) {
                return null;
            }
            if (substitution == null) {
                return string;
            }
            return regexp.substitute(
                string, substitution.getExpression(getProject()), options);
        }
    }

    /** Filter to trim white space */
    public static class Trim extends ChainableReaderFilter {
        /**
         * @param line the string to be trimmed
         * @return the trimmed string
         */
        public String filter(String line) {
            return line.trim();
        }
    }



    /** Filter remove empty tokens */
    public static class IgnoreBlank extends ChainableReaderFilter {
        /**
         * @param line the line to modify
         * @return the trimmed line
         */
        public String filter(String line) {
            if (line.trim().isEmpty()) {
                return null;
            }
            return line;
        }
    }

    /**
     * Filter to delete characters
     */
    public static class DeleteCharacters extends ProjectComponent
        implements Filter, ChainableReader {
        // Attributes
        /** the list of characters to remove from the input */
        private String deleteChars = "";

        /**
         * Set the list of characters to delete
         * @param deleteChars the list of characters
         */
        public void setChars(String deleteChars) {
            this.deleteChars = resolveBackSlash(deleteChars);
        }

        /**
         * remove characters from a string
         * @param string the string to remove the characters from
         * @return the converted string
         */
        public String filter(String string) {
            StringBuilder output = new StringBuilder(string.length());
            for (int i = 0; i < string.length(); ++i) {
                char ch = string.charAt(i);
                if (!(isDeleteCharacter(ch))) {
                    output.append(ch);
                }
            }
            return output.toString();
        }

        /**
         * factory method to provide a reader that removes
         * the characters from a reader as part of a filter
         * chain
         * @param reader the reader object
         * @return the chained reader object
         */
        public Reader chain(Reader reader) {
            return new BaseFilterReader(reader) {
                /**
                 * @return the next non delete character
                 */
                public int read()
                    throws IOException {
                    while (true) {
                        int c = in.read();
                        if (c == -1) {
                            return c;
                        }
                        if (!(isDeleteCharacter((char) c))) {
                            return c;
                        }
                    }
                }
            };
        }

        /**
         *  check if the character c is to be deleted
         *
         * @param c char to test
         * @return true if the supplied char is in the list to be stripped.
         */
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
     * xml does not do "c" like interpretation of strings.
     * i.e. \n\r\t etc.
     * this method processes \n, \r, \t, \f, \\
     * also subs \s with " \n\r\t\f"
     * a trailing '\' will be ignored
     *
     * @param input raw string with possible embedded '\'s
     * @return converted string
     */
    public static String resolveBackSlash(String input) {
        return StringUtils.resolveBackSlash(input);
    }

    /**
     * convert regex option flag characters to regex options
     * <ul>
     *   <li>g -  Regexp.REPLACE_ALL</li>
     *   <li>i -  Regexp.MATCH_CASE_INSENSITIVE</li>
     *   <li>m -  Regexp.MATCH_MULTILINE</li>
     *   <li>s -  Regexp.MATCH_SINGLELINE</li>
     * </ul>
     * @param flags the string containing the flags
     * @return the Regexp option bits
     */
    public static int convertRegexOptions(String flags) {
        return RegexpUtil.asOptions(flags);
    }
}
