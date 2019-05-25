/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.taskdefs.email;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.file.Files;

import org.apache.tools.ant.ProjectComponent;

/**
 * Class representing an email message.
 *
 * @since Ant 1.5
 */
public class Message extends ProjectComponent {
    private File messageSource = null;
    private StringBuffer buffer = new StringBuffer();
    private String mimeType = "text/plain";
    private boolean specified = false;
    private String charset = null;
    private String inputEncoding;

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
     * @param ps The print stream to write to
     * @throws IOException if an error occurs
     */
    @SuppressWarnings("resource")
    public void print(PrintStream ps) throws IOException {
        // We need character encoding aware printing here.
        // So, using BufferedWriter over OutputStreamWriter instead of PrintStream
        BufferedWriter out = null;
        try {
            out = charset == null ? new BufferedWriter(new OutputStreamWriter(ps))
                    : new BufferedWriter(new OutputStreamWriter(ps, charset));
            if (messageSource != null) {
                // Read message from a file
                try (BufferedReader in = new BufferedReader(getReader(messageSource))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        out.write(getProject().replaceProperties(line));
                        out.newLine();
                    }
                }
            } else {
                out.write(getProject().replaceProperties(buffer.substring(0)));
                out.newLine();
            }
            out.flush();
        } finally {
            //do not close the out writer as it is reused afterwards by the mail task
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

    /**
     * Sets the character set of mail message.
     * Will be ignored if mimeType contains ....; Charset=... substring.
     * @param charset the character set name.
     * @since Ant 1.6
     */
    public void setCharset(String charset) {
        this.charset = charset;
    }

    /**
     * Returns the charset of mail message.
     *
     * @return Charset of mail message.
     * @since Ant 1.6
     */
    public String getCharset() {
        return charset;
    }

    /**
     * Sets the encoding to expect when reading the message from a file.
     * <p>Will be ignored if the message has been specified inline.</p>
     * @param encoding the name of the charset used
     * @since Ant 1.9.4
     */
    public void setInputEncoding(String encoding) {
        this.inputEncoding = encoding;
    }

    private Reader getReader(File f) throws IOException {
        if (inputEncoding != null) {
            InputStream fis = Files.newInputStream(f.toPath());
            try {
                return new InputStreamReader(fis, inputEncoding);
            } catch (IOException ex) {
                fis.close();
                throw ex;
            }
        }
        return new FileReader(f);
    }
}
