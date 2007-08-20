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
package org.apache.tools.ant.listener;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Properties;
import java.util.Enumeration;
import java.util.StringTokenizer;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.email.EmailAddress;
import org.apache.tools.ant.taskdefs.email.Message;
import org.apache.tools.ant.taskdefs.email.Mailer;
import org.apache.tools.ant.util.ClasspathUtils;
import org.apache.tools.ant.util.DateUtils;
import org.apache.tools.ant.util.StringUtils;
import org.apache.tools.mail.MailMessage;

/**
 *  Buffers log messages from DefaultLogger, and sends an e-mail with the
 *  results. The following Project properties are used to send the mail.
 *  <ul>
 *    <li> MailLogger.mailhost [default: localhost] - Mail server to use</li>
 *    <li> MailLogger.port [default: 25] - Default port for SMTP </li>
 *    <li> MailLogger.from [required] - Mail "from" address</li>
 *    <li> MailLogger.failure.notify [default: true] - Send build failure
 *    e-mails?</li>
 *    <li> MailLogger.success.notify [default: true] - Send build success
 *    e-mails?</li>
 *    <li> MailLogger.failure.to [required if failure mail to be sent] - Address
 *    to send failure messages to</li>
 *    <li> MailLogger.success.to [required if success mail to be sent] - Address
 *    to send success messages to</li>
 *    <li> MailLogger.failure.subject [default: "Build Failure"] - Subject of
 *    failed build</li>
 *    <li> MailLogger.success.subject [default: "Build Success"] - Subject of
 *    successful build</li>
 *  </ul>
 *  These properties are set using standard Ant property setting mechanisms
 *  (&lt;property&gt;, command-line -D, etc). Ant properties can be overridden
 *  by specifying the filename of a properties file in the <i>
 *  MailLogger.properties.file property</i> . Any properties defined in that
 *  file will override Ant properties.
 *
 */
public class MailLogger extends DefaultLogger {
    /** Buffer in which the message is constructed prior to sending */
    private StringBuffer buffer = new StringBuffer();

    /**
     *  Sends an e-mail with the log results.
     *
     * @param event the build finished event
     */
    public void buildFinished(BuildEvent event) {
        super.buildFinished(event);

        Project project = event.getProject();
        Hashtable properties = project.getProperties();

        // overlay specified properties file (if any), which overrides project
        // settings
        Properties fileProperties = new Properties();
        String filename = (String) properties.get("MailLogger.properties.file");
        if (filename != null) {
            InputStream is = null;
            try {
                is = new FileInputStream(filename);
                fileProperties.load(is);
            } catch (IOException ioe) {
                // ignore because properties file is not required
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }

        for (Enumeration e = fileProperties.keys(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            String value = fileProperties.getProperty(key);
            properties.put(key, project.replaceProperties(value));
        }

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
                .from(getValue(properties, "from", null))
                .replytoList(getValue(properties, "replyto", ""))
                .toList(getValue(properties, prefix + ".to", null))
                .subject(getValue(
                             properties, prefix + ".subject",
                             (success) ? "Build Success" : "Build Failure"));
            if (values.user().equals("")
                && values.password().equals("")
                && !values.ssl()) {
                sendMail(values, buffer.substring(0));
            } else {
                sendMimeMail(
                    event.getProject(), values, buffer.substring(0));
            }
        } catch (Exception e) {
            System.out.println("MailLogger failed to send e-mail!");
            e.printStackTrace(System.err);
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
        private String subject;
        public String subject() {
            return subject;
        }
        public Values subject(String subject) {
            this.subject = subject;
            return this;
        }
    }

    /**
     *  Receives and buffers log messages.
     *
     * @param message the message being logger
     */
    protected void log(String message) {
        buffer.append(message).append(StringUtils.LINE_SEP);
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
     * @exception  Exception  thrown if no default value is specified and the
     *      property is not present in properties.
     */
    private String getValue(Hashtable properties, String name,
                            String defaultValue) throws Exception {
        String propertyName = "MailLogger." + name;
        String value = (String) properties.get(propertyName);

        if (value == null) {
            value = defaultValue;
        }

        if (value == null) {
            throw new Exception("Missing required parameter: " + propertyName);
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
        if (!values.replytoList().equals("")) {
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

        PrintStream ps = mailMessage.getPrintStream();
        ps.println(message);

        mailMessage.sendAndClose();
    }
    /**
     *  Send the mail  (MimeMail)
     * @param  project          current ant project
     * @param  values           various values
     * @param  message          mail body
     */
    private void sendMimeMail(Project project, Values values, String message) {
        // convert the replyTo string into a vector of emailaddresses
        Mailer mailer = null;
        try {
            mailer = (Mailer) ClasspathUtils.newInstance(
                    "org.apache.tools.ant.taskdefs.email.MimeMailer",
                    MailLogger.class.getClassLoader(), Mailer.class);
        } catch (BuildException e) {
            Throwable t = e.getCause() == null ? e : e.getCause();
            log("Failed to initialise MIME mail: " + t.getMessage());
            return;
        }
        Vector replyToList = vectorizeEmailAddresses(values.replytoList());
        mailer.setHost(values.mailhost());
        mailer.setPort(values.port());
        mailer.setUser(values.user());
        mailer.setPassword(values.password());
        mailer.setSSL(values.ssl());
        Message mymessage = new Message(message);
        mymessage.setProject(project);
        mailer.setMessage(mymessage);
        mailer.setFrom(new EmailAddress(values.from()));
        mailer.setReplyToList(replyToList);
        Vector toList = vectorizeEmailAddresses(values.toList());
        mailer.setToList(toList);
        mailer.setCcList(new Vector());
        mailer.setBccList(new Vector());
        mailer.setFiles(new Vector());
        mailer.setSubject(values.subject());
        mailer.send();
    }
    private Vector vectorizeEmailAddresses(String listString) {
        Vector emailList = new Vector();
        StringTokenizer tokens = new StringTokenizer(listString, ",");
        while (tokens.hasMoreTokens()) {
            emailList.addElement(new EmailAddress(tokens.nextToken()));
        }
        return emailList;
    }
}


