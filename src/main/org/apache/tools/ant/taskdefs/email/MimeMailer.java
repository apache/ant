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

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import java.util.Vector;
import java.util.Iterator;
import java.util.Properties;
import java.util.Enumeration;
import java.util.StringTokenizer;

import java.security.Provider;
import java.security.Security;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.AddressException;

import org.apache.tools.ant.BuildException;

/**
 * Uses the JavaMail classes to send Mime format email.
 *
 * @since Ant 1.5
 */
public class MimeMailer extends Mailer {
    private static final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";

    /** Default character set */
    private static final String DEFAULT_CHARSET
        = System.getProperty("file.encoding");

    // To work properly with national charsets we have to use
    // implementation of interface javax.activation.DataSource
    /**
     * String data source implementation.
     * @since Ant 1.6
     */
    class StringDataSource implements javax.activation.DataSource {
        private String data = null;
        private String type = null;
        private String charset = null;
        private ByteArrayOutputStream out;

        public InputStream getInputStream() throws IOException {
            if (data == null && out == null) {
                throw new IOException("No data");
            }
            if (out != null) {
                String encodedOut = out.toString(charset);
                data = (data != null) ? data.concat(encodedOut) : encodedOut;
                out = null;
            }
            return new ByteArrayInputStream(data.getBytes(charset));
        }

        public OutputStream getOutputStream() throws IOException {
            out = (out == null) ? new ByteArrayOutputStream() : out;
            return out;
        }

        public void setContentType(String type) {
            this.type = type.toLowerCase();
        }

        public String getContentType() {
            if (type != null && type.indexOf("charset") > 0
                && type.startsWith("text/")) {
                return type;
            }
            // Must be like "text/plain; charset=windows-1251"
            return new StringBuffer(type != null ? type : "text/plain").append(
                "; charset=").append(charset).toString();
        }

        public String getName() {
            return "StringDataSource";
        }

        public void setCharset(String charset) {
            this.charset = charset;
        }

        public String getCharset() {
            return charset;
        }
    }

    /**
     * Send the email.
     *
     * @throws BuildException if the email can't be sent.
     */
    public void send() {
        try {
            Properties props = new Properties();

            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", String.valueOf(port));

            // Aside, the JDK is clearly unaware of the Scottish
            // 'session', which involves excessive quantities of
            // alcohol :-)
            Session sesh;
            Authenticator auth;
            if (SSL) {
                try {
                    Provider p = (Provider) Class.forName(
                        "com.sun.net.ssl.internal.ssl.Provider").newInstance();
                    Security.addProvider(p);
                } catch (Exception e) {
                    throw new BuildException("could not instantiate ssl "
                        + "security provider, check that you have JSSE in "
                        + "your classpath");
                }
                // SMTP provider
                props.put("mail.smtp.socketFactory.class", SSL_FACTORY);
                props.put("mail.smtp.socketFactory.fallback", "false");
            }
            if (user == null && password == null) {
                sesh = Session.getDefaultInstance(props, null);
            } else {
                props.put("mail.smtp.auth", "true");
                auth = new SimpleAuthenticator(user, password);
                sesh = Session.getInstance(props, auth);
            }
            //create the message
            MimeMessage msg = new MimeMessage(sesh);
            MimeMultipart attachments = new MimeMultipart();

            //set the sender
            if (from.getName() == null) {
                msg.setFrom(new InternetAddress(from.getAddress()));
            } else {
                msg.setFrom(new InternetAddress(from.getAddress(),
                    from.getName()));
            }
            // set the reply to addresses
            msg.setReplyTo(internetAddresses(replyToList));
            msg.setRecipients(Message.RecipientType.TO,
                internetAddresses(toList));
            msg.setRecipients(Message.RecipientType.CC,
                internetAddresses(ccList));
            msg.setRecipients(Message.RecipientType.BCC,
                internetAddresses(bccList));

            // Choosing character set of the mail message
            // First: looking it from MimeType
            String charset = parseCharSetFromMimeType(message.getMimeType());
            if (charset != null) {
                // Assign/reassign message charset from MimeType
                message.setCharset(charset);
            } else {
                // Next: looking if charset having explicit definition
                charset = message.getCharset();
                if (charset == null) {
                    // Using default
                    charset = DEFAULT_CHARSET;
                    message.setCharset(charset);
                }
            }
            // Using javax.activation.DataSource paradigm
            StringDataSource sds = new StringDataSource();
            sds.setContentType(message.getMimeType());
            sds.setCharset(charset);

            if (subject != null) {
                msg.setSubject(subject, charset);
            }
            msg.addHeader("Date", getDate());

            for (Iterator iter = headers.iterator(); iter.hasNext();) {
                Header h = (Header) iter.next();
                msg.addHeader(h.getName(), h.getValue());
            }
            PrintStream out = new PrintStream(sds.getOutputStream());
            message.print(out);
            out.close();

            MimeBodyPart textbody = new MimeBodyPart();
            textbody.setDataHandler(new DataHandler(sds));
            attachments.addBodyPart(textbody);

            Enumeration e = files.elements();

            while (e.hasMoreElements()) {
                File file = (File) e.nextElement();

                MimeBodyPart body;

                body = new MimeBodyPart();
                if (!file.exists() || !file.canRead()) {
                    throw new BuildException("File \"" + file.getAbsolutePath()
                         + "\" does not exist or is not "
                         + "readable.");
                }
                FileDataSource fileData = new FileDataSource(file);
                DataHandler fileDataHandler = new DataHandler(fileData);

                body.setDataHandler(fileDataHandler);
                body.setFileName(file.getName());
                attachments.addBodyPart(body);
            }
            msg.setContent(attachments);
            Transport.send(msg);
        } catch (MessagingException e) {
            throw new BuildException("Problem while sending mime mail:", e);
        } catch (IOException e) {
            throw new BuildException("Problem while sending mime mail:", e);
        }
    }

    private static InternetAddress[] internetAddresses(Vector list)
        throws AddressException, UnsupportedEncodingException {
        InternetAddress[] addrs = new InternetAddress[list.size()];

        for (int i = 0; i < list.size(); ++i) {
            EmailAddress addr = (EmailAddress) list.elementAt(i);

            String name = addr.getName();
            addrs[i] = (name == null)
                ? new InternetAddress(addr.getAddress())
                : new InternetAddress(addr.getAddress(), name);
        }
        return addrs;
    }

    private String parseCharSetFromMimeType(String type) {
        int pos;
        if (type == null || (pos = type.indexOf("charset")) < 0) {
          return null;
        }
        // Assuming mime type in form "text/XXXX; charset=XXXXXX"
        StringTokenizer token = new StringTokenizer(type.substring(pos), "=; ");
        token.nextToken(); // Skip 'charset='
        return token.nextToken();
    }

    static class SimpleAuthenticator extends Authenticator {
        private String user = null;
        private String password = null;
        public SimpleAuthenticator(String user, String password) {
            this.user = user;
            this.password = password;
        }
        public PasswordAuthentication getPasswordAuthentication() {

            return new PasswordAuthentication(user, password);
        }
    }
}

