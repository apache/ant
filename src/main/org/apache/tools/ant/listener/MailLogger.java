/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2002 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.listener;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.DateUtils;
import org.apache.tools.ant.util.StringUtils;
import org.apache.tools.mail.MailMessage;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 *  Buffers log messages from DefaultLogger, and sends an e-mail with the
 *  results. The following Project properties are used to send the mail.
 *  <ul>
 *    <li> MailLogger.mailhost [default: localhost] - Mail server to use</li>
 *
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
 * @author Erik Hatcher
 *         <a href="mailto:ehatcher@apache.org">ehatcher@apache.org</a>
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

            String mailhost = getValue(properties, "mailhost", "localhost");
            String from = getValue(properties, "from", null);

            String toList = getValue(properties, prefix + ".to", null);
            String subject = getValue(properties, prefix + ".subject",
                    (success) ? "Build Success" : "Build Failure");

            sendMail(mailhost, from, toList, subject, buffer.toString());
        } catch (Exception e) {
            System.out.println("MailLogger failed to send e-mail!");
            e.printStackTrace(System.err);
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
     *
     * @param  mailhost         mail server
     * @param  from             from address
     * @param  toList           comma-separated recipient list
     * @param  subject          mail subject
     * @param  message          mail body
     * @exception  IOException  thrown if sending message fails
     */
    private void sendMail(String mailhost, String from, String toList,
                          String subject, String message) throws IOException {
        MailMessage mailMessage = new MailMessage(mailhost);
        mailMessage.setHeader("Date", DateUtils.getDateForHeader());

        mailMessage.from(from);

        StringTokenizer t = new StringTokenizer(toList, ", ", false);
        while (t.hasMoreTokens()) {
            mailMessage.to(t.nextToken());
        }

        mailMessage.setSubject(subject);

        PrintStream ps = mailMessage.getPrintStream();
        ps.println(message);

        mailMessage.sendAndClose();
    }
}


