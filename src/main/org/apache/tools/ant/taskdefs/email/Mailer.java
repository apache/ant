/*
 * Copyright  2002-2004 The Apache Software Foundation
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
package org.apache.tools.ant.taskdefs.email;

import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.DateUtils;

/**
 * Base class for the various emailing implementations.
 *
 * @author roxspring@yahoo.com Rob Oxspring
 * @since Ant 1.5
 */
public abstract class Mailer {
    protected String host = null;
    protected int port = -1;
    protected String user = null;
    protected String password = null;
    protected boolean SSL = false;
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

    /**
     * Sets the mail server
     *
     * @param host the mail server name
     */
    public void setHost(String host) {
        this.host = host;
    }


    /**
     * Sets the smtp port
     *
     * @param port the SMTP port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Sets the user for smtp auth
     *
     * @param user the username
     * @since ant 1.6
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Sets the password for smtp auth
     *
     * @param password the authentication password
     * @since ant 1.6
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Sets whether the user wants to send the mail through SSL
     *
     * @param SSL if true use SSL transport
     * @since ant 1.6
     */
    public void setSSL(boolean SSL) {
        this.SSL = SSL;
    }

    /**
     * Sets the message
     *
     * @param m the message content
     */
    public void setMessage(Message m) {
        this.message = m;
    }


    /**
     * Sets the address to send from
     *
     * @param from the sender
     */
    public void setFrom(EmailAddress from) {
        this.from = from;
    }


    /**
     * Sets the replyto addresses
     *
     * @param list a vector of reployTo addresses
     * @since ant 1.6
     */
    public void setReplyToList(Vector list) {
        this.replyToList = list;
    }


    /**
     * Set the to addresses
     *
     * @param list a vector of recipient addresses
     */
    public void setToList(Vector list) {
        this.toList = list;
    }


    /**
     * Sets the cc addresses
     *
     * @param list a vector of cc addresses
     */
    public void setCcList(Vector list) {
        this.ccList = list;
    }


    /**
     * Sets the bcc addresses
     *
     * @param list a vector of the bcc addresses
     */
    public void setBccList(Vector list) {
        this.bccList = list;
    }


    /**
     * Sets the files to attach
     *
     * @param files list of files to attach to the email.
     */
    public void setFiles(Vector files) {
        this.files = files;
    }


    /**
     * Sets the subject
     *
     * @param subject the subject line
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }


    /**
     * Sets the owning task
     *
     * @param task the owning task instance
     */
    public void setTask(Task task) {
        this.task = task;
    }


    /**
     * Indicates whether filenames should be listed in the body
     *
     * @param b if true list attached file names in the body content.
     */
    public void setIncludeFileNames(boolean b) {
        this.includeFileNames = b;
    }


    /**
     * This method should send the email
     *
     * @throws BuildException if the email can't be sent.
     */
    public abstract void send()
         throws BuildException;

    /**
     * Returns the current Date in a format suitable for a SMTP date
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

