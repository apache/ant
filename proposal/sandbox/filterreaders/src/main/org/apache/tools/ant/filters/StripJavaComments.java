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

/**
 * This is a java comment and string stripper reader that filters
 * these lexical tokens out for purposes of simple Java parsing.
 * (if you have more complex Java parsing needs, use a real lexer).
 * Since this class heavily relies on the single char read function,
 * you are reccomended to make it work on top of a buffered reader.
 */
public final class StripJavaComments
    extends BaseFilterReader
    implements ChainableReader
{
    /**
     * This constructor is a dummy constructor and is
     * not meant to be used by any class other than Ant's
     * introspection mechanism. This will close the filter
     * that is created making it useless for further operations.
     */
    public StripJavaComments() {
        super();
    }

    /**
     * Create a new filtered reader.
     *
     * @param in  a Reader object providing the underlying stream.
     */
    public StripJavaComments(final Reader in) {
        super(in);
    }

    /**
     * Filter out Java Style comments
     */
    public final int read() throws IOException {
        int ch = in.read();
        if (ch == '/') {
            ch = in.read();
            if (ch == '/') {
                while (ch != '\n' && ch != -1) {
                    ch = in.read();
                }
            } else if (ch == '*') {
                while (ch != -1) {
                    ch = in.read();
                    if (ch == '*') {
                        ch = in.read();
                        while (ch == '*' && ch != -1) {
                            ch = in.read();
                        }

                        if (ch == '/') {
                            ch = read();
                            break;
                        }
                    }
                }
            }
        }

        if (ch == '"') {
            while (ch != -1) {
                ch = in.read();
                if (ch == '\\') {
                    ch = in.read();
                } else if (ch == '"') {
                    ch = read();
                    break;
                }
            }
        }

        if (ch == '\'') {
            ch = in.read();
            if (ch == '\\') {
                ch = in.read();
            }
            ch = in.read();
            ch = read();
        }

        return ch;
    }

    /**
     * Create a new StripJavaComments object using the passed in
     * Reader for instantiation.
     */
    public final Reader chain(final Reader rdr) {
        StripJavaComments newFilter = new StripJavaComments(rdr);
        return newFilter;
    }
}
