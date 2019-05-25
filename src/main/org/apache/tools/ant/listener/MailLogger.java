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
package org.apache.tools.ant.listener;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.email.EmailAddress;
import org.apache.tools.ant.taskdefs.email.Mailer;
import org.apache.tools.ant.taskdefs.email.Message;
import org.apache.tools.ant.util.ClasspathUtils;
import org.apache.tools.ant.util.DateUtils;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.mail.MailMessage;

/**
 *  Buffers log messages from DefaultLogger, and sends an e-mail with the
 *  results. The following Project properties are used to send the mail.
 *  <ul>
 *    <li> MailLogger.mailhost [default: localhost] - Mail server to use</li>
 *    <li> MailLogger.port [default: 25] - Default port for SMTP </li>
 *    <li> Maillogger.user [no default] - user name for SMTP auth
 *    (requires JavaMail)</li>
 *    <li> Maillogger.password [no default] - password for SMTP auth
 *    (requires JavaMail)</li>
 *    <li> Maillogger.ssl [default: false] - on or true if ssl is
 *    needed (requires JavaMail)</li>
 *    <li> MailLogger.from [required] - Mail "from" address</li>
 *    <li> MailLogger.from [no default] - Mail "replyto" address(es),
 *    comma-separated</li>
 *    <li> MailLogger.failure.notify [default: true] - Send build failure
 *    e-mails?</li>
 *    <li> MailLogger.success.notify [default: true] - Send build success
 *    e-mails?</li>
 *    <li> MailLogger.failure.to [required if failure mail to be sent] - Address
 *    to send failure messages to</li>
 *    <li> MailLogger.success.to [required if success mail to be sent] - Address
 *    to send success messages to</li>
 *    <li> MailLogger.failure.cc [no default] - Address
 *    to send failure messages to carbon copy (cc)</li>
 *    <li> MailLogger.success.to [no default] - Address
 *    to send success messages to carbon copy (cc)</li>
 *    <li> MailLogger.failure.bcc [no default] - Address
 *    to send failure messages to blind carbon copy (bcc)</li>
 *    <li> MailLogger.success.bcc [no default] - Address
 *    to send success messages to blind carbon copy (bcc)</li>
 *    <li> MailLogger.failure.subject [default: "Build Failure"] - Subject of
 *    failed build</li>
 *    <li> MailLogger.success.subject [default: "Build Success"] - Subject of
 *    successful build</li>
 *    <li> MailLogger.failure.body [default: none] - fixed text of
 *    mail body for a failed build, default is to send the logfile</li>
 *    <li> MailLogger.success.body [default: none] - fixed text of
 *    mail body for a successful build, default is to send the logfile</li>
 *    <li> MailLogger.mimeType [default: text/plain] - MIME-Type of email</li>
 *    <li> MailLogger.charset [no default] - character set of email</li>
 *    <li> Maillogger.starttls.enable [default: false] - on or true if
 *    STARTTLS should be supported (requires JavaMail)</li>
 *    <li> MailLogger.properties.file [no default] - Filename of
 *    properties file that will override other values.</li>
 *  </ul>
 *  These properties are set using standard Ant property setting mechanisms
 *  (&lt;property&gt;, command-line -D, etc). Ant properties can be overridden
 *  by specifying the filename of a properties file in the <i>
 *  MailLogger.properties.file property</i> . Any properties defined in that
 *  file will override Ant properties.
 *
 */
public class MailLogger extends DefaultLogger {
    private static final String DEFAULT_MIME_TYPE = "text/plain";

    /** Buffer in which the message is constructed prior to sending */
    private StringBuffer buffer = new StringBuffer();

    /**
     *  Sends an e-mail with the log results.
     *
     * @param event the build finished event
     */
    @Override
    public void buildFinished(BuildEvent event) {
        super.buildFinished(event);

        Project project = event.getProject();
        Map<String, Object> properties = project.getProperties();

        // overlay specified properties file (if any), which overrides project
        // settings
        Properties fileProperties = new Properties();
        String filename = (String) properties.get("MailLogger.properties.file");
        if (filename != null) {
            InputStream is = null;
            try {
                is = Files.newInputStream(Paths.get(filename));
                fileProperties.load(is);
            } catch (IOException ioe) {
                // ignore because properties file is not required
            } finally {
                FileUtils.close(is);
            }
        }

        fileProperties.stringPropertyNames()
                .forEach(key -> properties.put(key, project.replaceProperties(fileProperties.getProperty(key))));

        boolean success = (event.getException() == null);
        String prefix = success ? "success" : "failure";

        try {
            boolean notify = Project.toBoolean(getValue(properties,
                    prefix + ".notify", "on"));

            if (!notify) {
                return;
            }
            Values values = new Values()
                .mailhost(getValue(properties, "mailhost", "localhost"))
                .port(Integer.parseInt(
                          getValue(
                              properties, "port",
                              String.valueOf(MailMessage.DEFAULT_PORT))))
                .user(getValue(properties, "user", ""))
                .password(getValue(properties, "password", ""))
                .ssl(Project.toBoolean(getValue(properties,
                                                "ssl", "off")))
                .starttls(Project.toBoolean(getValue(properties,
                                                     "starttls.enable", "off")))
                .from(getValue(properties, "from", null))
                .replytoList(getValue(properties, "replyto", ""))
                .toList(getValue(properties, prefix + ".to", null))
                .toCcList(getValue(properties, prefix + ".cc", ""))
                .toBccList(getValue(properties, prefix + ".bcc", ""))
                .mimeType(getValue(properties, "mimeType", DEFAULT_MIME_TYPE))
                .charset(getValue(properties, "charset", ""))
                .body(getValue(properties, prefix + ".body", ""))
                .subject(getValue(
                             properties, prefix + ".subject",
                             (success) ? "Build Success" : "Build Failure"));
            if (values.user().isEmpty()
                && values.password().isEmpty()
                && !values.ssl() && !values.starttls()) {
                sendMail(values, buffer.substring(0));
            } else {
                sendMimeMail(
                    event.getProject(), values, buffer.substring(0));
            }
        } catch (Exception e) {
            System.out.println("MailLogger failed to send e-mail!");
            e.printStackTrace(System.err); //NOSONAR
        }
    }

    private static class Values {
        private String mailhost;
        public String mailhost() {
            return mailhost;
        }
        public Values mailhost(String mailhost) {
            this.mailhost = mailhost;
            return this;
        }
        private int port;
        public int port() {
            return port;
        }
        public Values port(int port) {
            this.port = port;
            return this;
        }
        private String user;
        public String user() {
            return user;
        }
        public Values user(String user) {
            this.user = user;
            return this;
        }
        private String password;
        public String password() {
            return password;
        }
        public Values password(String password) {
            this.password = password;
            return this;
        }
        private boolean ssl;
        public boolean ssl() {
            return ssl;
        }
        public Values ssl(boolean ssl) {
            this.ssl = ssl;
            return this;
        }
        private String from;
        public String from() {
            return from;
        }
        public Values from(String from) {
            this.from = from;
            return this;
        }
        private String replytoList;
        public String replytoList() {
            return replytoList;
        }
        public Values replytoList(String replytoList) {
            this.replytoList = replytoList;
            return this;
        }
        private String toList;
        public String toList() {
            return toList;
        }
        public Values toList(String toList) {
            this.toList = toList;
            return this;
        }
        private String toCcList;
        public String toCcList() {
            return toCcList;
        }
        public Values toCcList(String toCcList) {
            this.toCcList = toCcList;
            return this;
        }
        private String toBccList;
        public String toBccList() {
            return toBccList;
        }
        public Values toBccList(String toBccList) {
            this.toBccList = toBccList;
            return this;
        }
        private String subject;
        public String subject() {
            return subject;
        }
        public Values subject(String subject) {
            this.subject = subject;
            return this;
        }
        private String charset;
        public String charset() {
            return charset;
        }
        public Values charset(String charset) {
            this.charset = charset;
            return this;
        }
        private String mimeType;
        public String mimeType() {
            return mimeType;
        }
        public Values mimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }
        private String body;
        public String body() {
            return body;
        }
        public Values body(String body) {
            this.body = body;
            return this;
        }
        private boolean starttls;
        public boolean starttls() {
            return starttls;
        }
        public Values starttls(boolean starttls) {
            this.starttls = starttls;
            return this;
        }
    }

    /**
     *  Receives and buffers log messages.
     *
     * @param message the message being logger
     */
    @Override
    protected void log(String message) {
        buffer.append(message).append(System.lineSeparator());
    }


    /**
     *  Gets the value of a property.
     *
     * @param  properties     Properties to obtain value from
     * @param  name           suffix of property name. "MailLogger." will be
     *      prepended internally.
     * @param  defaultValue   value returned if not present in the properties.
     *      Set to null to make required.
     * @return                The value of the property, or default value.
     */
    private String getValue(Map<String, Object> properties, String name,
                            String defaultValue) {
        String propertyName = "MailLogger." + name;
        String value = (String) properties.get(propertyName);

        if (value == null) {
            value = defaultValue;
        }

        if (value == null) {
            throw new RuntimeException("Missing required parameter: " + propertyName); //NOSONAR
        }

        return value;
    }


    /**
     *  Send the mail
     * @param  values           the various values.
     * @param  message          mail body
     * @exception  IOException  thrown if sending message fails
     */
    private void sendMail(Values values, String message) throws IOException {
        MailMessage mailMessage = new MailMessage(
            values.mailhost(), values.port());
        mailMessage.setHeader("Date", DateUtils.getDateForHeader());

        mailMessage.from(values.from());
        if (!values.replytoList().isEmpty()) {
            StringTokenizer t = new StringTokenizer(
                values.replytoList(), ", ", false);
            while (t.hasMoreTokens()) {
                mailMessage.replyto(t.nextToken());
            }
        }
        StringTokenizer t = new StringTokenizer(values.toList(), ", ", false);
        while (t.hasMoreTokens()) {
            mailMessage.to(t.nextToken());
        }

        mailMessage.setSubject(values.subject());

        if (values.charset().isEmpty()) {
            mailMessage.setHeader("Content-Type", values.mimeType());
        } else {
            mailMessage.setHeader("Content-Type", values.mimeType()
                                  + "; charset=\"" + values.charset() + "\"");
        }

        PrintStream ps = mailMessage.getPrintStream();
        ps.println(values.body().isEmpty() ? message : values.body());

        mailMessage.sendAndClose();
    }
    /**
     *  Send the mail  (MimeMail)
     * @param  project          current ant project
     * @param  values           various values
     * @param  message          mail body
     */
    private void sendMimeMail(Project project, Values values, String message) {
        Mailer mailer = null;
        try {
            mailer = ClasspathUtils.newInstance(
                    "org.apache.tools.ant.taskdefs.email.MimeMailer",
                    MailLogger.class.getClassLoader(), Mailer.class);
        } catch (BuildException e) {
            Throwable t = e.getCause() == null ? e : e.getCause();
            log("Failed to initialise MIME mail: " + t.getMessage());
            return;
        }
        // convert the replyTo string into a vector of emailaddresses
        Vector<EmailAddress> replyToList = splitEmailAddresses(values.replytoList());
        mailer.setHost(values.mailhost());
        mailer.setPort(values.port());
        mailer.setUser(values.user());
        mailer.setPassword(values.password());
        mailer.setSSL(values.ssl());
        mailer.setEnableStartTLS(values.starttls());
        Message mymessage =
            new Message(!values.body().isEmpty() ? values.body() : message);
        mymessage.setProject(project);
        mymessage.setMimeType(values.mimeType());
        if (!values.charset().isEmpty()) {
            mymessage.setCharset(values.charset());
        }
        mailer.setMessage(mymessage);
        mailer.setFrom(new EmailAddress(values.from()));
        mailer.setReplyToList(replyToList);
        Vector<EmailAddress> toList = splitEmailAddresses(values.toList());
        mailer.setToList(toList);
        Vector<EmailAddress> toCcList = splitEmailAddresses(values.toCcList());
        mailer.setCcList(toCcList);
        Vector<EmailAddress> toBccList = splitEmailAddresses(values.toBccList());
        mailer.setBccList(toBccList);
        mailer.setFiles(new Vector<>());
        mailer.setSubject(values.subject());
        mailer.setHeaders(new Vector<>());
        mailer.send();
    }

    private Vector<EmailAddress> splitEmailAddresses(String listString) {
        return Stream.of(listString.split(",")).map(EmailAddress::new)
            .collect(Collectors.toCollection(Vector::new));
    }
}
