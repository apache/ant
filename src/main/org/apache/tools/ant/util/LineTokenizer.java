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
package org.apache.tools.ant.util;

import java.io.Reader;
import java.io.IOException;

import org.apache.tools.ant.ProjectComponent;

/**
 * class to tokenize the input as lines seperated
 * by \r (mac style), \r\n (dos/windows style) or \n (unix style)
 * @author Peter Reilly
 * @since Ant 1.6
 */
public class LineTokenizer extends ProjectComponent
    implements Tokenizer {
    private String  lineEnd = "";
    private int     pushed = -2;
    private boolean includeDelims = false;

    /**
     * attribute includedelims - whether to include
     * the line ending with the line, or to return
     * it in the posttoken
     * default false
     * @param includeDelims if true include /r and /n in the line
     */

    public void setIncludeDelims(boolean includeDelims) {
        this.includeDelims = includeDelims;
    }

    /**
     * get the next line from the input
     *
     * @param in the input reader
     * @return the line excluding /r or /n, unless includedelims is set
     * @exception IOException if an error occurs reading
     */
    public String getToken(Reader in) throws IOException {
        int ch = -1;
        if (pushed != -2) {
            ch = pushed;
            pushed = -2;
        } else {
            ch = in.read();
        }
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
                } else if (ch == '\n') {
                    lineEnd = "\n";
                    break;
                } else {
                    line.append((char) ch);
                }
            } else {
                state = 0;
                if (ch == '\n') {
                    lineEnd = "\r\n";
                } else {
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

    /**
     * @return the line ending character(s) for the current line
     */
    public String getPostToken() {
        if (includeDelims) {
            return "";
        }
        return lineEnd;
    }

}

