/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
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

