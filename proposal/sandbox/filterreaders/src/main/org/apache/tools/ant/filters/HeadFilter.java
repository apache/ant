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

import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.Parameterizable;

/**
 * Read the first n lines (Default is first 10 lines)
 *
 * Example:
 * =======
 *
 * &lt;headfilter lines=&quot;3&quot;/&gt;
 *
 * Or:
 *
 * &lt;filterreader classname=&quot;org.apache.tools.ant.filters.HeadFilter&quot;&gt;
 *    &lt;param name=&quot;lines&quot; value=&quot;3&quot;/&gt;
 * &lt;/filterreader&gt;
 *
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 */
public final class HeadFilter
    extends BaseFilterReader
    implements Parameterizable, ChainableReader
{
    /** Lines key to represent the number of lines to be returned. */
    private static final String LINES_KEY = "lines";

    /** The passed in parameter array. */
    private Parameter[] parameters;

    /** Have the parameters passed been interpreted? */
    private boolean initialized = false;

    /** Number of lines currently read in. */
    private long linesRead = 0;

    /** Default number of lines returned. */
    private long lines = 10;

    /** If the next character being read is a linefeed, must it be ignored? */
    private boolean ignoreLineFeed = false;

    /**
     * This constructor is a dummy constructor and is
     * not meant to be used by any class other than Ant's
     * introspection mechanism. This will close the filter
     * that is created making it useless for further operations.
     */
    public HeadFilter() {
        super();
    }

    /**
     * Create a new filtered reader.
     *
     * @param in  a Reader object providing the underlying stream.
     */
    public HeadFilter(final Reader in) {
        super(in);
    }

    /**
     * Read the first n lines.
     */
    public final int read() throws IOException {
        if (!getInitialized()) {
            initialize();
            setInitialized(true);
        }

        int ch = -1;

        if (linesRead < lines) {

            ch = in.read();

            if (ignoreLineFeed) {
                if (ch == '\n') {
                    ch = in.read();
                }
                ignoreLineFeed = false;
            }

            switch (ch) {
                case '\r':
                    ch = '\n';
                    ignoreLineFeed = true;
                    //fall through
                case '\n':
                    linesRead++;
                    break;
            }
        }

        return ch;
    }

    /**
     * Set number of lines to be returned.
     */
    public final void setLines(final long lines) {
        this.lines = lines;
    }

    /**
     * Get number of lines to be returned.
     */
    private final long getLines() {
        return lines;
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
     * Create a new HeadFilter using the passed in
     * Reader for instantiation.
     */
    public final Reader chain(final Reader rdr) {
        HeadFilter newFilter = new HeadFilter(rdr);
        newFilter.setLines(getLines());
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
     * Scan for the lines parameter.
     */
    private final void initialize() {
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                if (LINES_KEY.equals(parameters[i].getName())) {
                    lines = new Long(parameters[i].getValue()).longValue();
                    break;
                }
            }
        }
    }
}
