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
package org.apache.tools.ant.taskdefs.email;

import org.apache.tools.ant.ProjectComponent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Class representing an email message.
 *
 * @author roxspring@yahoo.com Rob Oxspring
 * @since Ant 1.5
 */
public class Message extends ProjectComponent {
    private File messageSource = null;
    private StringBuffer buffer = new StringBuffer();
    private String mimeType = "text/plain";
    private boolean specified = false;


    /** Creates a new empty message  */
    public Message() {
    }


    /**
     * Creates a new message based on the given string
     *
     * @param text the message
     */
    public Message(String text) {
        addText(text);
    }


    /**
     * Creates a new message using the contents of the given file.
     *
     * @param file the source of the message
     */
    public Message(File file) {
        messageSource = file;
    }


    /**
     * Adds a textual part of the message
     *
     * @param text some text to add
     */
    public void addText(String text) {
        buffer.append(text);
    }


    /**
     * Sets the source file of the message
     *
     * @param src the source of the message
     */
    public void setSrc(File src) {
        this.messageSource = src;
    }


    /**
     * Sets the content type for the message
     *
     * @param mimeType a mime type e.g. "text/plain"
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
        specified = true;
    }


    /**
     * Returns the content type
     *
     * @return the mime type
     */
    public String getMimeType() {
        return mimeType;
    }


    /**
     * Prints the message onto an output stream
     *
     * @param out The print stream to write to
     * @throws IOException if an error occurs
     */
    public void print(PrintStream out)
         throws IOException {
        if (messageSource != null) {
            // Read message from a file
            FileReader freader = new FileReader(messageSource);

            try {
                BufferedReader in = new BufferedReader(freader);
                String line = null;

                while ((line = in.readLine()) != null) {
                    out.println(getProject().replaceProperties(line));
                }
            } finally {
                freader.close();
            }
        } else {
            out.println(getProject().replaceProperties(buffer.toString()));
        }
    }


    /**
     * Returns true if the mimeType has been set.
     *
     * @return false if the default value is in use
     */
    public boolean isMimeTypeSpecified() {
        return specified;
    }
}

