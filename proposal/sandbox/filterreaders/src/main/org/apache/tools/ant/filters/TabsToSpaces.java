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

/**
 * Converts tabs to spaces.
 *
 * Example Usage:
 * =============
 *
 * &lt;tabtospaces tablength=&quot;8&quot;/&gt;
 *
 * Or:
 *
 * <filterreader classname=&quot;org.apache.tools.ant.filters.TabsToSpaces&quot;>
 *    <param name=&quot;tablength&quot; value=&quot;8&quot;/>
 * </filterreader>
 *
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 */
public final class TabsToSpaces
    extends BaseParamFilterReader
    implements ChainableReader
{
    /** The default tab length is 8 */
    private static final int DEFAULT_TAB_LENGTH = 8;

    /** The name that param recognizes to set the tablength. */
    private static final String TAB_LENGTH_KEY = "tablength";

    /** Default tab length. */
    private int tabLength = DEFAULT_TAB_LENGTH;

    /** How many more spaces must be returned to replace a tab? */
    private int spacesRemaining = 0;

    /**
     * This constructor is a dummy constructor and is
     * not meant to be used by any class other than Ant's
     * introspection mechanism. This will close the filter
     * that is created making it useless for further operations.
     */
    public TabsToSpaces() {
        super();
    }

    /**
     * Create a new filtered reader.
     *
     * @param in  a Reader object providing the underlying stream.
     */
    public TabsToSpaces(final Reader in) {
        super(in);
    }

    /**
     * Convert tabs with spaces
     */
    public final int read() throws IOException {
        if (!getInitialized()) {
            initialize();
            setInitialized(true);
        }

        int ch = -1;

        if (spacesRemaining > 0) {
            spacesRemaining--;
            ch = ' ';
        } else {
            ch = in.read();
            if (ch == '\t') {
                spacesRemaining = tabLength - 1;
                ch = ' ';
            }
        }
        return ch;
    }

    /**
     * Set the tab length.
     */
    public final void setTablength(final int tabLength) {
        this.tabLength = tabLength;
    }

    /**
     * Get the tab length
     */
    private final int getTablength() {
        return tabLength;
    }

    /**
     * Create a new TabsToSpaces object using the passed in
     * Reader for instantiation.
     */
    public final Reader chain(final Reader rdr) {
        TabsToSpaces newFilter = new TabsToSpaces(rdr);
        newFilter.setTablength(getTablength());
        newFilter.setInitialized(true);
        return newFilter;
    }

    /**
     * Initialize tokens
     */
    private final void initialize() {
        Parameter[] params = getParameters();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] != null) {
                    if (TAB_LENGTH_KEY.equals(params[i].getName())) {
                        tabLength =
                            new Integer(params[i].getValue()).intValue();
                        break;
                    }
                }
            }
        }
    }
}
