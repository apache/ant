/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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
package org.apache.tools.ant.taskdefs.email;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Enumeration;
import org.apache.tools.ant.BuildException;
import org.apache.tools.mail.MailMessage;

/**
 * Class responsible for sending email through raw protocol methods.
 *
 * @since Ant 1.5
 */
class PlainMailer extends Mailer {
    /**
     * Sends the email using the apache MailMessage class.
     *
     * @see org.apache.tools.mail.MailMessage
     */
    public void send() {
        try {
            MailMessage mailMessage = new MailMessage(host, port);

            mailMessage.from(from.toString());

            Enumeration e;

            e = replyToList.elements();
            while (e.hasMoreElements()) {
                mailMessage.replyto(e.nextElement().toString());
            }
            e = toList.elements();
            while (e.hasMoreElements()) {
                mailMessage.to(e.nextElement().toString());
            }
            e = ccList.elements();
            while (e.hasMoreElements()) {
                mailMessage.cc(e.nextElement().toString());
            }
            e = bccList.elements();
            while (e.hasMoreElements()) {
                mailMessage.bcc(e.nextElement().toString());
            }
            if (subject != null) {
                mailMessage.setSubject(subject);
            }
            mailMessage.setHeader("Date", getDate());
            if (message.getCharset() != null) {
                mailMessage.setHeader("Content-Type", message.getMimeType()
                    + "; charset=\"" + message.getCharset() + "\"");
            } else {
                mailMessage.setHeader("Content-Type", message.getMimeType());
            }
            e = headers.elements();
            while (e.hasMoreElements()) {
                Header h = (Header) e.nextElement();
                mailMessage.setHeader(h.getName(), h.getValue());
            }
            PrintStream out = mailMessage.getPrintStream();
            message.print(out);

            e = files.elements();
            while (e.hasMoreElements()) {
                attach((File) e.nextElement(), out);
            }
            mailMessage.sendAndClose();
        } catch (IOException ioe) {
            throw new BuildException("IO error sending mail", ioe);
        }

    }

    /**
     * Attaches a file to this email
     *
     * @param file The file to attache
     * @param out The message stream to add to
     * @throws IOException if errors occur
     */
    protected void attach(File file, PrintStream out)
         throws IOException {
        if (!file.exists() || !file.canRead()) {
            throw new BuildException("File \"" + file.getName()
                 + "\" does not exist or is not "
                 + "readable.");
        }

        if (includeFileNames) {
            out.println();

            String filename = file.getName();
            int filenamelength = filename.length();

            out.println(filename);
            for (int star = 0; star < filenamelength; star++) {
                out.print('=');
            }
            out.println();
        }

        int length;
        final int maxBuf = 1024;
        byte[] buf = new byte[maxBuf];
        FileInputStream finstr = new FileInputStream(file);

        try {
            BufferedInputStream in = new BufferedInputStream(finstr, buf.length);

            while ((length = in.read(buf)) != -1) {
                out.write(buf, 0, length);
            }
        } finally {
            finstr.close();
        }
    }
}

