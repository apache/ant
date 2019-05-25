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

import org.apache.tools.ant.types.Parameter;

/**
 * This filter strips line comments.
 *
 * Example:
 *
 * <pre>&lt;striplinecomments&gt;
 *   &lt;comment value=&quot;#&quot;/&gt;
 *   &lt;comment value=&quot;--&quot;/&gt;
 *   &lt;comment value=&quot;REM &quot;/&gt;
 *   &lt;comment value=&quot;rem &quot;/&gt;
 *   &lt;comment value=&quot;//&quot;/&gt;
 * &lt;/striplinecomments&gt;</pre>
 *
 * Or:
 *
 * <pre>&lt;filterreader
 *      classname=&quot;org.apache.tools.ant.filters.StripLineComments&quot;&gt;
 *   &lt;param type=&quot;comment&quot; value="#&quot;/&gt;
 *   &lt;param type=&quot;comment&quot; value=&quot;--&quot;/&gt;
 *   &lt;param type=&quot;comment&quot; value=&quot;REM &quot;/&gt;
 *   &lt;param type=&quot;comment&quot; value=&quot;rem &quot;/&gt;
 *   &lt;param type=&quot;comment&quot; value=&quot;//&quot;/&gt;
 * &lt;/filterreader&gt;</pre>
 *
 */
public final class StripLineComments
    extends BaseParamFilterReader
    implements ChainableReader {
    /** Parameter name for the comment prefix. */
    private static final String COMMENTS_KEY = "comment";

    /** Vector that holds the comment prefixes. */
    private Vector<String> comments = new Vector<>();

    /** The line that has been read ahead. */
    private String line = null;

    /**
     * Constructor for "dummy" instances.
     *
     * @see BaseFilterReader#BaseFilterReader()
     */
    public StripLineComments() {
        super();
    }

    /**
     * Creates a new filtered reader.
     *
     * @param in A Reader object providing the underlying stream.
     *           Must not be <code>null</code>.
     */
    public StripLineComments(final Reader in) {
        super(in);
    }

    /**
     * Returns the next character in the filtered stream, only including
     * lines from the original stream which don't start with any of the
     * specified comment prefixes.
     *
     * @return the next character in the resulting stream, or -1
     * if the end of the resulting stream has been reached
     *
     * @exception IOException if the underlying stream throws an IOException
     * during reading
     */
    public int read() throws IOException {
        if (!getInitialized()) {
            initialize();
            setInitialized(true);
        }

        int ch = -1;

        if (line != null) {
            ch = line.charAt(0);
            if (line.length() == 1) {
                line = null;
            } else {
                line = line.substring(1);
            }
        } else {
            line = readLine();
            final int commentsSize = comments.size();

            while (line != null) {
                for (int i = 0; i < commentsSize; i++) {
                    String comment = comments.elementAt(i);
                    if (line.startsWith(comment)) {
                        line = null;
                        break;
                    }
                }

                if (line == null) {
                    // line started with comment
                    line = readLine();
                } else {
                    break;
                }
            }

            if (line != null) {
                return read();
            }
        }

        return ch;
    }

    /**
     * Adds a <code>comment</code> element to the list of prefixes.
     *
     * @param comment The <code>comment</code> element to add to the
     * list of comment prefixes to strip. Must not be <code>null</code>.
     */
    public void addConfiguredComment(final Comment comment) {
        comments.addElement(comment.getValue());
    }

    /**
     * Sets the list of comment prefixes to strip.
     *
     * @param comments A list of strings, each of which is a prefix
     * for a comment line. Must not be <code>null</code>.
     */
    private void setComments(final Vector<String> comments) {
        this.comments = comments;
    }

    /**
     * Returns the list of comment prefixes to strip.
     *
     * @return the list of comment prefixes to strip.
     */
    private Vector<String> getComments() {
        return comments;
    }

    /**
     * Creates a new StripLineComments using the passed in
     * Reader for instantiation.
     *
     * @param rdr A Reader object providing the underlying stream.
     *            Must not be <code>null</code>.
     *
     * @return a new filter based on this configuration, but filtering
     *         the specified reader
     */
    public Reader chain(final Reader rdr) {
        StripLineComments newFilter = new StripLineComments(rdr);
        newFilter.setComments(getComments());
        newFilter.setInitialized(true);
        return newFilter;
    }

    /**
     * Parses the parameters to set the comment prefixes.
     */
    private void initialize() {
        Parameter[] params = getParameters();
        if (params != null) {
            for (Parameter param : params) {
                if (COMMENTS_KEY.equals(param.getType())) {
                    comments.addElement(param.getValue());
                }
            }
        }
    }

    /**
     * The class that holds a comment representation.
     */
    public static class Comment {

        /** The prefix for a line comment. */
        private String value;

        /**
         * Sets the prefix for this type of line comment.
         *
         * @param comment The prefix for a line comment of this type.
         * Must not be <code>null</code>.
         */
        public final void setValue(String comment) {
            if (value != null) {
                throw new IllegalStateException("Comment value already set.");
            }
            value = comment;
        }

        /**
         * Returns the prefix for this type of line comment.
         *
         * @return the prefix for this type of line comment.
         */
        public final String getValue() {
            return value;
        }

        /**
         * Alt. syntax to set the prefix for this type of line comment.
         *
         * @param comment The prefix for a line comment of this type.
         * Must not be <code>null</code>.
         */
        public void addText(String comment) {
            setValue(comment);
        }
    }
}
