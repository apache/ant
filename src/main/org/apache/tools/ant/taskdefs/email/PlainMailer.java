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
 * @author roxspring@imapmail.org Rob Oxspring
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
            MailMessage mailMessage = new MailMessage(host,port);

            mailMessage.from(from.toString());

            Enumeration e;

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
            mailMessage.setHeader("Content-Type", message.getMimeType());

            PrintStream out = mailMessage.getPrintStream();

            message.print(out);

            e = files.elements();
            while (e.hasMoreElements()) {
                File file = (File) e.nextElement();

                attach(file, out);
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
        byte[] buf = new byte[1024];
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

