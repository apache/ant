/* 
 * Copyright  2000-2002,2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * 
 */

package org.apache.tools.mail;

import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * A wrapper around the raw input from the SMTP server that assembles
 * multi line responses into a single String.
 *
 * <p>The same rules used here would apply to FTP and other Telnet
 * based protocols as well.</p>
 *
 * @author Stefan Bodewig
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
        if (line != null && line.length() >= 3) {
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
        return line.length() > 3 && line.charAt(3) == '-';
    }

    /**
     * Append the text from this line of the resonse.
     */
    private void append(String line) {
        if (line.length() > 4) {
            result.append(line.substring(4));
            result.append(" ");
        }
    }
}
