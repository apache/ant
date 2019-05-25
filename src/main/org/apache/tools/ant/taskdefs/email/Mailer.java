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

import java.io.File;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.DateUtils;

/**
 * Base class for the various emailing implementations.
 *
 * @since Ant 1.5
 */
public abstract class Mailer {
    // CheckStyle:VisibilityModifier OFF - bc
    protected String host = null;
    protected int port = -1;
    protected String user = null;
    protected String password = null;
    // CheckStyle:MemberNameCheck OFF - bc
    protected boolean SSL = false;
    // CheckStyle:MemberNameCheck ON
    protected Message message;
    protected EmailAddress from;
    protected Vector<EmailAddress> replyToList = null;
    protected Vector<EmailAddress> toList = null;
    protected Vector<EmailAddress> ccList = null;
    protected Vector<EmailAddress> bccList = null;
    protected Vector<File> files = null;
    protected String subject = null;
    protected Task task;
    protected boolean includeFileNames = false;
    protected Vector<Header> headers = null;
    // CheckStyle:VisibilityModifier ON
    private boolean ignoreInvalidRecipients = false;
    private boolean starttls = false;
    private boolean portExplicitlySpecified = false;

    /**
     * Set the mail server.
     *
     * @param host the mail server name.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Set the smtp port.
     *
     * @param port the SMTP port.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Whether the port has been explicitly specified by the user.
     *
     * @param explicit boolean
     * @since Ant 1.8.2
     */
    public void setPortExplicitlySpecified(boolean explicit) {
        portExplicitlySpecified = explicit;
    }

    /**
     * Whether the port has been explicitly specified by the user.
     *
     * @return boolean
     * @since Ant 1.8.2
     */
    protected boolean isPortExplicitlySpecified() {
        return portExplicitlySpecified;
    }

    /**
     * Set the user for smtp auth.
     *
     * @param user the username.
     * @since Ant 1.6
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Set the password for smtp auth.
     *
     * @param password the authentication password.
     * @since Ant 1.6
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Set whether to send the mail through SSL.
     *
     * @param ssl if true use SSL transport.
     * @since Ant 1.6
     */
    public void setSSL(boolean ssl) {
        this.SSL = ssl;
    }

    /**
     * Set whether to allow authentication to switch to a TLS
     * connection via STARTTLS.
     * @param b boolean; if true STARTTLS will be supported.
     * @since Ant 1.8.0
     */
    public void setEnableStartTLS(boolean b) {
        this.starttls = b;
    }

    protected boolean isStartTLSEnabled() {
        return starttls;
    }

    /**
     * Set the message.
     *
     * @param m the message content.
     */
    public void setMessage(Message m) {
        this.message = m;
    }

    /**
     * Set the address to send from.
     *
     * @param from the sender.
     */
    public void setFrom(EmailAddress from) {
        this.from = from;
    }

    /**
     * Set the replyTo addresses.
     *
     * @param list a vector of replyTo addresses.
     * @since Ant 1.6
     */
    public void setReplyToList(Vector<EmailAddress> list) {
        this.replyToList = list;
    }

    /**
     * Set the to addresses.
     *
     * @param list a vector of recipient addresses.
     */
    public void setToList(Vector<EmailAddress> list) {
        this.toList = list;
    }

    /**
     * Set the cc addresses.
     *
     * @param list a vector of cc addresses.
     */
    public void setCcList(Vector<EmailAddress> list) {
        this.ccList = list;
    }

    /**
     * Set the bcc addresses.
     *
     * @param list a vector of the bcc addresses.
     */
    public void setBccList(Vector<EmailAddress> list) {
        this.bccList = list;
    }

    /**
     * Set the files to attach.
     *
     * @param files list of files to attach to the email.
     */
    public void setFiles(Vector<File> files) {
        this.files = files;
    }

    /**
     * Set the subject.
     *
     * @param subject the subject line.
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * Set the owning task.
     *
     * @param task the owning task instance.
     */
    public void setTask(Task task) {
        this.task = task;
    }

    /**
     * Indicate whether filenames should be listed in the body.
     *
     * @param b if true list attached file names in the body content.
     */
    public void setIncludeFileNames(boolean b) {
        this.includeFileNames = b;
    }

    /**
     * Set the generic headers to add to the email.
     * @param v a Vector presumed to contain Header objects.
     * @since Ant 1.7
     */
    public void setHeaders(Vector<Header> v) {
        this.headers = v;
    }

    /**
     * Send the email.
     *
     * @throws BuildException if the email can't be sent.
     */
    public abstract void send()
         throws BuildException;

    /**
     * Whether invalid recipients should be ignored (but a warning
     * will be logged) instead of making the task fail.
     *
     * <p>Even with this property set to true the task will still fail
     * if the mail couldn't be sent to any recipient at all.</p>
     *
     * @param b boolean
     * @since Ant 1.8.0
     */
    public void setIgnoreInvalidRecipients(boolean b) {
        ignoreInvalidRecipients = b;
    }

    /**
     * Whether invalid recipients should be ignored.
     *
     * @return boolean
     * @since Ant 1.8.0
     */
    protected boolean shouldIgnoreInvalidRecipients() {
        return ignoreInvalidRecipients;
    }

    /**
     * Return the current Date in a format suitable for a SMTP date
     * header.
     *
     * @return the current date in SMTP suitable format.
     * @since Ant 1.5
     */
    protected final String getDate() {
        return DateUtils.getDateForHeader();
    }
}
