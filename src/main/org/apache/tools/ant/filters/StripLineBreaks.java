/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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

import org.apache.tools.ant.types.Parameter;

/**
 * Filter to flatten the stream to a single line.
 * 
 * Example:
 *
 * <pre>&lt;striplinebreaks/&gt;</pre>
 *
 * Or:
 *
 * <pre>&lt;filterreader classname=&quot;org.apache.tools.ant.filters.StripLineBreaks&quot;/&gt;</pre>
 *
 * @author Steve Loughran
 * @author Magesh Umasankar
 */
public final class StripLineBreaks
    extends BaseParamFilterReader
    implements ChainableReader {
    /**
     * Line-breaking characters.
     * What should we do on funny IBM mainframes with odd line endings?
     */
    private static final String DEFAULT_LINE_BREAKS = "\r\n";

    /** Parameter name for the line-breaking characters parameter. */
    private static final String LINE_BREAKS_KEY = "linebreaks";

    /** The characters that are recognized as line breaks. */
    private String lineBreaks = DEFAULT_LINE_BREAKS;

    /**
     * Constructor for "dummy" instances.
     * 
     * @see BaseFilterReader#BaseFilterReader()
     */
    public StripLineBreaks() {
        super();
    }

    /**
     * Creates a new filtered reader.
     *
     * @param in A Reader object providing the underlying stream.
     *           Must not be <code>null</code>.
     */
    public StripLineBreaks(final Reader in) {
        super(in);
    }

    /**
     * Returns the next character in the filtered stream, only including
     * characters not in the set of line-breaking characters.
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

        int ch = in.read();
        while (ch != -1) {
            if (lineBreaks.indexOf(ch) == -1) {
                break;
            } else {
                ch = in.read();
            }
        }
        return ch;
    }

    /**
     * Sets the line-breaking characters.
     * 
     * @param lineBreaks A String containing all the characters to be
     *                   considered as line-breaking.
     */
    public final void setLineBreaks(final String lineBreaks) {
        this.lineBreaks = lineBreaks;
    }

    /**
     * Returns the line-breaking characters as a String.
     * 
     * @return a String containing all the characters considered as 
     *         line-breaking
     */
    private final String getLineBreaks() {
        return lineBreaks;
    }

    /**
     * Creates a new StripLineBreaks using the passed in
     * Reader for instantiation.
     * 
     * @param rdr A Reader object providing the underlying stream.
     *            Must not be <code>null</code>.
     * 
     * @return a new filter based on this configuration, but filtering
     *         the specified reader
     */
    public final Reader chain(final Reader rdr) {
        StripLineBreaks newFilter = new StripLineBreaks(rdr);
        newFilter.setLineBreaks(getLineBreaks());
        newFilter.setInitialized(true);
        return newFilter;
    }

    /**
     * Parses the parameters to set the line-breaking characters.
     */
    private final void initialize() {
        String userDefinedLineBreaks = null;
        Parameter[] params = getParameters();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (LINE_BREAKS_KEY.equals(params[i].getName())) {
                    userDefinedLineBreaks = params[i].getValue();
                    break;
                }
            }
        }
        if (userDefinedLineBreaks != null) {
            lineBreaks = userDefinedLineBreaks;
        }
    }
}
