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
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import org.apache.tools.ant.types.Parameter;

/**
 * Concats a file before and/or after the file.
 *
 * <p>Example:<pre>
 * <copy todir="build">
 *     <fileset dir="src" includes="*.java"/>
 *     <filterchain>
 *         <concatfilter before="apache-license-java.txt"/>
 *     </filterchain>
 * </copy>
 * </pre>
 * Copies all java sources from <i>src</i> to <i>build</i> and adds the
 * content of <i>apache-license-java.txt</i> add the beginning of each
 * file.</p>
 *
 * @since 1.6
 * @version 2003-09-17
 * @author Jan Matèrne
 */
public final class ConcatFilter extends BaseParamFilterReader
    implements ChainableReader {

    /** File to add before the content. */
    private File before;

    /** File to add after the content. */
    private File after;

    /** Reader for before-file. */
    private Reader beforeReader = new EmptyReader();

    /** Reader for after-file. */
    private Reader afterReader = new EmptyReader();

    /**
     * Constructor for "dummy" instances.
     *
     * @see BaseFilterReader#BaseFilterReader()
     */
    public ConcatFilter() {
        super();
    }

    /**
     * Creates a new filtered reader.
     *
     * @param in A Reader object providing the underlying stream.
     *           Must not be <code>null</code>.
     */
    public ConcatFilter(final Reader in) {
        super(in);
    }

    /**
     * Returns the next character in the filtered stream. If the desired
     * number of lines have already been read, the resulting stream is
     * effectively at an end. Otherwise, the next character from the
     * underlying stream is read and returned.
     *
     * @return the next character in the resulting stream, or -1
     * if the end of the resulting stream has been reached
     *
     * @exception IOException if the underlying stream throws an IOException
     * during reading
     */
    public int read() throws IOException {
        // do the "singleton" initialization
        if (!getInitialized()) {
            initialize();
            setInitialized(true);
        }

        int ch = -1;

        // The readers return -1 if they end. So simply read the "before"
        // after that the "content" and at the end the "after" file.
        ch = beforeReader.read();
        if (ch == -1) {
            ch = super.read();
        }
        if (ch == -1) {
            ch = afterReader.read();
        }

        return ch;
    }

    /**
     * Sets <i>before</i> attribute.
     * @param before new value
     */
    public void setBefore(final File before) {
        this.before = before;
    }

    /**
     * Returns <i>before</i> attribute.
     * @return before attribute
     */
    public File getBefore() {
        return before;
    }

    /**
     * Sets <i>after</i> attribute.
     * @param after new value
     */
    public void setAfter(final File after) {
        this.after = after;
    }

    /**
     * Returns <i>after</i> attribute.
     * @return after attribute
     */
    public File getAfter() {
        return after;
    }

    /**
     * Creates a new ConcatReader using the passed in
     * Reader for instantiation.
     *
     * @param rdr A Reader object providing the underlying stream.
     *            Must not be <code>null</code>.
     *
     * @return a new filter based on this configuration, but filtering
     *         the specified reader
     */
    public Reader chain(final Reader rdr) {
        ConcatFilter newFilter = new ConcatFilter(rdr);
        newFilter.setBefore(getBefore());
        newFilter.setAfter(getAfter());
        // Usually the initialized is set to true. But here it must not.
        // Because the before and after readers have to be instantiated
        // on runtime
        //newFilter.setInitialized(true);
        return newFilter;
    }

    /**
     * Scans the parameters list for the "lines" parameter and uses
     * it to set the number of lines to be returned in the filtered stream.
     * also scan for skip parameter.
     */
    private void initialize() throws IOException {
        // get parameters
        Parameter[] params = getParameters();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if ("before".equals(params[i].getName())) {
                    setBefore(new File(params[i].getValue()));
                    continue;
                }
                if ("after".equals(params[i].getName())) {
                    setAfter(new File(params[i].getValue()));
                    continue;
                }
            }
        }
        if (before != null) {
            if (!before.isAbsolute()) {
                before = new File(getProject().getBaseDir(), before.getPath());
            }
            beforeReader = new BufferedReader(new FileReader(before));
        }
        if (after != null) {
            if (!after.isAbsolute()) {
                after = new File(getProject().getBaseDir(), after.getPath());
            }
            afterReader = new BufferedReader(new FileReader(after));
        }
   }

   /**
    * Reader which is always at the end of file.
    * Used for easier algorithm (polymorphism instead if-cascades).
    */
   private class EmptyReader extends Reader {
       public int read(char[] ch, int i1, int i2) { return -1; }
       public void close() { }
   }

}