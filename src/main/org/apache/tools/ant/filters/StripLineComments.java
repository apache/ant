/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
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
 * <pre>&lt;filterreader classname=&quot;org.apache.tools.ant.filters.StripLineComments&quot;&gt;
 *   &lt;param type=&quot;comment&quot; value="#&quot;/&gt;
 *   &lt;param type=&quot;comment&quot; value=&quot;--&quot;/&gt;
 *   &lt;param type=&quot;comment&quot; value=&quot;REM &quot;/&gt;
 *   &lt;param type=&quot;comment&quot; value=&quot;rem &quot;/&gt;
 *   &lt;param type=&quot;comment&quot; value=&quot;//&quot;/&gt;
 * &lt;/filterreader&gt;</pre>
 *
 * @author Magesh Umasankar
 */
public final class StripLineComments
    extends BaseParamFilterReader
    implements ChainableReader {
    /** Parameter name for the comment prefix. */
    private static final String COMMENTS_KEY = "comment";

    /** Vector that holds the comment prefixes. */
    private Vector comments = new Vector();

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
    public final int read() throws IOException {
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
                    String comment = (String) comments.elementAt(i);
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
    public final void addConfiguredComment(final Comment comment) {
        comments.addElement(comment.getValue());
    }

    /**
     * Sets the list of comment prefixes to strip.
     * 
     * @param comments A list of strings, each of which is a prefix
     * for a comment line. Must not be <code>null</code>.
     */
    private void setComments(final Vector comments) {
        this.comments = comments;
    }

    /**
     * Returns the list of comment prefixes to strip.
     * 
     * @return the list of comment prefixes to strip.
     */
    private final Vector getComments() {
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
    public final Reader chain(final Reader rdr) {
        StripLineComments newFilter = new StripLineComments(rdr);
        newFilter.setComments(getComments());
        newFilter.setInitialized(true);
        return newFilter;
    }

    /**
     * Parses the parameters to set the comment prefixes.
     */
    private final void initialize() {
        Parameter[] params = getParameters();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (COMMENTS_KEY.equals(params[i].getType())) {
                    comments.addElement(params[i].getValue());
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
    }
}
