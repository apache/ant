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
    protected Vector replyToList = null;
    protected Vector toList = null;
    protected Vector ccList = null;
    protected Vector bccList = null;
    protected Vector files = null;
    protected String subject = null;
    protected Task task;
    protected boolean includeFileNames = false;
    protected Vector headers = null;
    // CheckStyle:VisibilityModifier ON

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
     * Set the replyto addresses.
     *
     * @param list a vector of reployTo addresses.
     * @since Ant 1.6
     */
    public void setReplyToList(Vector list) {
        this.replyToList = list;
    }

    /**
     * Set the to addresses.
     *
     * @param list a vector of recipient addresses.
     */
    public void setToList(Vector list) {
        this.toList = list;
    }

    /**
     * Set the cc addresses.
     *
     * @param list a vector of cc addresses.
     */
    public void setCcList(Vector list) {
        this.ccList = list;
    }

    /**
     * Set the bcc addresses.
     *
     * @param list a vector of the bcc addresses.
     */
    public void setBccList(Vector list) {
        this.bccList = list;
    }

    /**
     * Set the files to attach.
     *
     * @param files list of files to attach to the email.
     */
    public void setFiles(Vector files) {
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
    public void setHeaders(Vector v) {
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
     * Return the current Date in a format suitable for a SMTP date
     * header.
     *
     * @return the current date in SMTP suitable format.
     *
     * @since Ant 1.5
     */
    protected final String getDate() {
        return DateUtils.getDateForHeader();
    }
}

