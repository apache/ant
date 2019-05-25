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

package org.apache.tools.mail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A wrapper around the raw input from the SMTP server that assembles
 * multi line responses into a single String.
 *
 * <p>The same rules used here would apply to FTP and other Telnet
 * based protocols as well.</p>
 *
 */
public class SmtpResponseReader {
    // CheckStyle:VisibilityModifier OFF - bc
    protected BufferedReader reader = null;
    // CheckStyle:VisibilityModifier ON

    /**
     * Wrap this input stream.
     * @param in the stream to wrap.
     */
    public SmtpResponseReader(InputStream in) {
        reader = new BufferedReader(new InputStreamReader(in));
    }

    /**
     * Read until the server indicates that the response is complete.
     *
     * @return Response code (3 digits) + Blank + Text from all
     *         response line concatenated (with blanks replacing the \r\n
     *         sequences).
     * @throws IOException on error.
     */
    public String getResponse() throws IOException {
        StringBuilder result = new StringBuilder();
        String line = reader.readLine();
        // CheckStyle:MagicNumber OFF
        if (line != null && line.length() >= 3) {
            result.append(line, 0, 3);
            result.append(" ");
        }
        // CheckStyle:MagicNumber ON

        while (line != null) {
            appendTo(result, line);
            if (!hasMoreLines(line)) {
                break;
            }
            line = reader.readLine();
        }
        return result.toString().trim();
    }

    /**
     * Closes the underlying stream.
     * @throws IOException on error.
     */
    public void close() throws IOException {
        reader.close();
    }

    /**
     * Should we expect more input?
     * @param line the line to check.
     * @return true if there are more lines to check.
     */
    protected boolean hasMoreLines(String line) {
        // CheckStyle:MagicNumber OFF
        return line.length() > 3 && line.charAt(3) == '-';
        // CheckStyle:MagicNumber ON
    }

    /**
     * Append the text from this line of the response.
     */
    private static void appendTo(StringBuilder target, String line) {
        // CheckStyle:MagicNumber OFF
        if (line.length() > 4) {
            target.append(line.substring(4)).append(' ');
        }
        // CheckStyle:MagicNumber ON
    }
}
