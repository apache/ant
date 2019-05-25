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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
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
    @Override
    public void send() {
        try {
            MailMessage mailMessage = new MailMessage(host, port);

            mailMessage.from(from.toString());

            boolean atLeastOneRcptReached = false;

            replyToList.stream().map(Object::toString).forEach(mailMessage::replyto);

            for (EmailAddress to : toList) {
                try {
                    mailMessage.to(to.toString());
                    atLeastOneRcptReached = true;
                } catch (IOException ex) {
                    badRecipient(to, ex);
                }
            }

            for (EmailAddress cc : ccList) {
                try {
                    mailMessage.cc(cc.toString());
                    atLeastOneRcptReached = true;
                } catch (IOException ex) {
                    badRecipient(cc, ex);
                }
            }

            for (EmailAddress bcc : bccList) {
                try {
                    mailMessage.bcc(bcc.toString());
                    atLeastOneRcptReached = true;
                } catch (IOException ex) {
                    badRecipient(bcc, ex);
                }
            }

            if (!atLeastOneRcptReached) {
                throw new BuildException("Couldn't reach any recipient");
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
            if (headers != null) {
                for (Header h : headers) {
                    mailMessage.setHeader(h.getName(), h.getValue());
                }
            }
            PrintStream out = mailMessage.getPrintStream();
            message.print(out);

            if (files != null) {
                for (File f : files) {
                    attach(f, out);
                }
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
            throw new BuildException(
                "File \"%s\" does not exist or is not readable.",
                file.getAbsolutePath());
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

        final int maxBuf = 1024;
        byte[] buf = new byte[maxBuf];

        try (InputStream finstr = Files.newInputStream(file.toPath());
             BufferedInputStream in = new BufferedInputStream(finstr, buf.length)) {

            int length;
            while ((length = in.read(buf)) != -1) {
                out.write(buf, 0, length);
            }
        }
    }

    private void badRecipient(EmailAddress rcpt, IOException reason) {
        String msg = "Failed to send mail to " + rcpt;
        if (shouldIgnoreInvalidRecipients()) {
            msg += " because of :" + reason.getMessage();
            if (task != null) {
                task.log(msg, Project.MSG_WARN);
            } else {
                System.err.println(msg);
            }
        } else {
            throw new BuildException(msg, reason);
        }
    }
}

