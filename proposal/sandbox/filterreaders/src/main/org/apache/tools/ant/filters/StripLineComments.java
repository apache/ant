/*
 *  The Apache Software License, Version 1.1
 *
 *  Copyright (c) 2002 The Apache Software Foundation.  All rights
 *  reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution, if
 *  any, must include the following acknowlegement:
 *  "This product includes software developed by the
 *  Apache Software Foundation (http://www.apache.org/)."
 *  Alternately, this acknowlegement may appear in the software itself,
 *  if and wherever such third-party acknowlegements normally appear.
 *
 *  4. The names "The Jakarta Project", "Ant", and "Apache Software
 *  Foundation" must not be used to endorse or promote products derived
 *  from this software without prior written permission. For written
 *  permission, please contact apache@apache.org.
 *
 *  5. Products derived from this software may not be called "Apache"
 *  nor may "Apache" appear in their names without prior written
 *  permission of the Apache Group.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of the Apache Software Foundation.  For more
 *  information on the Apache Software Foundation, please see
 *  <http://www.apache.org/>.
 */
package org.apache.tools.ant.filters;

import java.io.IOException;
import java.io.Reader;
import java.util.Vector;

import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.Parameterizable;

/**
 * This is a line comment stripper reader
 *
 * Example:
 * =======
 *
 * &lt;striplinecomments&gt;
 *   &lt;comment value=&quot;#&quot;/&gt;
 *   &lt;comment value=&quot;--&quot;/&gt;
 *   &lt;comment value=&quot;REM &quot;/&gt;
 *   &lt;comment value=&quot;rem &quot;/&gt;
 *   &lt;comment value=&quot;//&quot;/&gt;
 * &lt;/striplinecomments&gt;
 *
 * Or:
 *
 * &lt;filterreader classname=&quot;org.apache.tools.ant.filters.StripLineComments&quot;&gt;
 *    &lt;param type=&quot;comment&quot; value="#&quot;/&gt;
 *    &lt;param type=&quot;comment&quot; value=&quot;--&quot;/&gt;
 *    &lt;param type=&quot;comment&quot; value=&quot;REM &quot;/&gt;
 *    &lt;param type=&quot;comment&quot; value=&quot;rem &quot;/&gt;
 *    &lt;param type=&quot;comment&quot; value=&quot;//&quot;/&gt;
 * &lt;/filterreader&gt;
 *
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 */
public final class StripLineComments
    extends BaseFilterReader
    implements Parameterizable, ChainableReader
{
    /** The type that param recognizes to set the comments. */
    private static final String COMMENTS_KEY = "comment";

    /** The passed in parameter array. */
    private Parameter[] parameters;

    /** Have the parameters passed been interpreted? */
    private boolean initialized = false;

    /** Vector that holds comments. */
    private Vector comments = new Vector();

    /** The line that has been read ahead. */
    private String line = null;

    /**
     * This constructor is a dummy constructor and is
     * not meant to be used by any class other than Ant's
     * introspection mechanism. This will close the filter
     * that is created making it useless for further operations.
     */
    public StripLineComments() {
        super();
    }

    /**
     * Create a new filtered reader.
     *
     * @param in  a Reader object providing the underlying stream.
     */
    public StripLineComments(final Reader in) {
        super(in);
    }

    /**
     * Read in line by line; Ignore line if it
     * begins with a comment string.
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
            ch = in.read();
            while (ch != -1) {
                if (line == null) {
                    line = "";
                }
                line = line + (char) ch;
                if (ch == '\n') {
                    break;
                }
                ch = in.read();
            }

            if (line != null) {
                int commentsSize = comments.size();
                for (int i = 0; i < commentsSize; i++) {
                    String comment = (String) comments.elementAt(i);
                    if (line.startsWith(comment)) {
                        line = null;
                        break;
                    }
                }
                return read();
            }
        }

        return ch;
    }

    /**
     * Add the Comment element.
     */
    public final void addConfiguredComment(final Comment comment) {
        comments.addElement(comment.getValue());
    }

    /**
     * Set the comments vector.
     */
    private void setComments(final Vector comments) {
        this.comments = comments;
    }

    /**
     * Get the comments vector.
     */
    private final Vector getComments() {
        return comments;
    }

    /**
     * Set the initialized status.
     */
    private final void setInitialized(final boolean initialized) {
        this.initialized = initialized;
    }

    /**
     * Get the initialized status.
     */
    private final boolean getInitialized() {
        return initialized;
    }

    /**
     * Create a new StripLineComments object using the passed in
     * Reader for instantiation.
     */
    public final Reader chain(final Reader rdr) {
        StripLineComments newFilter = new StripLineComments(rdr);
        newFilter.setComments(getComments());
        newFilter.setInitialized(true);
        return newFilter;
    }

    /**
     * Set Parameters
     */
    public final void setParameters(final Parameter[] parameters) {
        this.parameters = parameters;
        setInitialized(false);
    }

    /**
     * Comments set using the param element.
     */
    private final void initialize() {
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                if (COMMENTS_KEY.equals(parameters[i].getType())) {
                    comments.addElement(parameters[i].getValue());
                }
            }
        }
    }

    /**
     * The class that holds a comment.
     */
    public static class Comment {

        /** The comment*/
        private String value;

        /**
         * Set the comment.
         */
        public final void setValue(String comment) {
            value = comment;
        }

        /**
         * Get the comment.
         */
        public final String getValue() {
            return value;
        }
    }
}
