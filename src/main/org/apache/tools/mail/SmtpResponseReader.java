/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
 */

package org.apache.tools.mail;

import java.io.*;

/**
 * A wrapper around the raw input from the SMTP server that assembles
 * multi line responses into a single String.
 *
 * <p>The same rules used here would apply to FTP and other Telnet
 * based protocols as well.</p>
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 */
public class SmtpResponseReader {

    protected BufferedReader reader = null;
    private StringBuffer result = new StringBuffer();

    /**
     * Wrap this input stream.
     */
    public SmtpResponseReader(InputStream in) {
        reader = new BufferedReader(new InputStreamReader(in)); 
    }

    /**
     * Read until the server indicates that the response is complete.
     *
     * @return Responsecode (3 digits) + Blank + Text from all
     *         response line concatenated (with blanks replacing the \r\n
     *         sequences).
     */
    public String getResponse() throws IOException {
        result.setLength(0);
        String line = reader.readLine();
        if (line != null) {
            result.append(line.substring(0, 3));
            result.append(" ");
        }
        
        while (line != null) {
            append(line);
            if (!hasMoreLines(line)) {
                break;
            }
            line = reader.readLine();
        }
        return result.toString().trim();
    }

    /**
     * Closes the underlying stream.
     */
    public void close() throws IOException {
        reader.close();
    }

    /**
     * Should we expect more input?
     */
    protected boolean hasMoreLines(String line) {
        return line.charAt(3) == '-';
    }

    /**
     * Append the text from this line of the resonse.
     */
    private void append(String line) {
        result.append(line.substring(4));
        result.append(" ");
    }
}
