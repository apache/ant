/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in
 * the documentation and/or other materials provided with the
 * distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 * any, must include the following acknowlegement:
 * "This product includes software developed by the
 * Apache Software Foundation (http://www.apache.org/)."
 * Alternately, this acknowlegement may appear in the software itself,
 * if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 * Foundation" must not be used to endorse or promote products derived
 * from this software without prior written permission. For written
 * permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 * nor may "Apache" appear in their names without prior written
 * permission of the Apache Group.
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
 *
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant.taskdefs.optional.net;


// Standard SDK imports
import java.util.Properties;
import java.util.Vector;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

//imported for the mail api
import javax.mail.Address;
import javax.mail.Session;
import javax.mail.Message;
import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.SendFailedException;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.AddressException;

//imported for data source and handler
import javax.activation.DataHandler;
import javax.activation.FileDataSource;

// Ant imports
import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.FileScanner;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;



/**
 * A task to send SMTP email. This version has near identical syntax to
 * the SendEmail task, but is MIME aware. It also requires Sun's mail.jar and
 * activation.jar to compile and execute, which puts it clearly into the
 * very optional category.
 *
 * @author glenn_twiggs@bmc.com
 * @author steve_l@iseran.com steve loughran
 * @author erik@hatcher.net Erik Hatcher
 * @author paulo.gaspar@krankikom.de Paulo Gaspar
 * @created 01 May 2001
 */
public class MimeMail extends Task {
    /**
     * failure flag
     */
    private boolean failOnError = true;

    /**
     * sender
     */
    private String from = null;

    /**
     * host running SMTP
     */
    private String mailhost = "localhost";

    /**
     * any text
     */
    private String message = null;


    /**
     *  message file (mutually exclusive from message)
     */
    private File messageFile = null;

    /**
     * TO recipients
     */
    private String toList = null;

    /**
     * CC (Carbon Copy) recipients
     */
    protected String   ccList  = null;

    /**
     * BCC (Blind Carbon Copy) recipients
     */
    protected String   bccList  = null;

    /**
     * subject field
     */
    private String subject = null;

    /**
     * file list
     */
    private Vector filesets = new Vector();

    /**
     * type of the text message, plaintext by default but text/html or
     * text/xml is quite feasible
     */
    private String messageMimeType = "text/plain";

    /**
     * Creates new instance
     */
    public MimeMail() {
    }


    /**
     * Sets the FailOnError attribute of the MimeMail object
     *
     * @param failOnError The new FailOnError value
     */
    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    /**
     * Adds a set of files (nested fileset attribute).
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    /**
     * Sets the toList parameter of this build task.
     *
     * @param toList Comma-separated list of email recipient addreses.
     */
    public void setToList(String toList) {
        this.toList = toList;
    }

    /**
     * Sets the toList parameter of this build task.
     *
     * @param toList Comma-separated list of email recipient addreses.
     */
    public void setCcList(String ccList) {
        this.ccList = ccList;
    }

    /**
     * Sets the toList parameter of this build task.
     *
     * @param toList Comma-separated list of email recipient addreses.
     */
    public void setBccList(String bccList) {
        this.bccList = bccList;
    }


    /**
     * Sets the "from" parameter of this build task.
     *
     * @param from Email address of sender.
     */
    public void setFrom(String from) {
        this.from = from;
    }


    /**
     * Sets the mailhost parameter of this build task.
     *
     * @param mailhost Mail host name.
     */
    public void setMailhost(String mailhost) {
        this.mailhost = mailhost;
    }


    /**
     * Sets the message parameter of this build task.
     *
     * @param message Message body of this email.
     */
    public void setMessage(String message) {
        this.message = message;
    }

    public void setMessageFile(File messageFile) {
        this.messageFile = messageFile;
    }


    /**
     * set type of the text message, plaintext by default but text/html
     * or text/xml is quite feasible
     *
     * @param type The new MessageMimeType value
     */
    public void setMessageMimeType(String type) {
        this.messageMimeType = type;
    }

    /**
     * Sets the subject parameter of this build task.
     *
     * @param subject Subject of this email.
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }


    /**
     * verify parameters
     *
     * @throws BuildException if something is invalid
     */
    public void validate() {
        if (from == null) {
            throw new BuildException("Attribute \"from\" is required.");
        }

        if ((toList == null) && (ccList == null) && (bccList == null)) {
            throw new BuildException("Attribute \"toList\", \"ccList\" or \"bccList\" is required.");
        }

        if (message == null && filesets.isEmpty() && messageFile == null) {
            throw new BuildException("FileSet, \"message\", or \"messageFile\" is required.");
        }

        if (message != null && messageFile != null) {
            throw new BuildException("Only one of \"message\" or \"messageFile\" may be specified.");
        }
    }


    /**
     * Executes this build task. throws org.apache.tools.ant.BuildException
     * if there is an error during task execution.
     *
     * @exception BuildException Description of Exception
     */
    public void execute()
        throws BuildException {
        try {
            validate();
            doMail();
        }
        catch (Exception e) {
            if (failOnError) {
                throw new BuildException(e);
            }
            else {
                String text = e.toString();
                log(text, Project.MSG_ERR);
            }
        }
    }


    // helper method to add recipients
    private static void addRecipients( MimeMessage msg,
                                       Message.RecipientType recipType,
                                       String addrUserName,
                                       String addrList
                                     ) throws MessagingException, BuildException {
        if ((null == addrList) || (addrList.trim().length() <= 0))
            return;

        try {
            InternetAddress[] addrArray = InternetAddress.parse(addrList);

            if ((null == addrArray) || (0 == addrArray.length))
                throw new BuildException("Empty " + addrUserName + " recipients list was specified");

            msg.setRecipients(recipType, addrArray);
        }
        catch(AddressException ae) {
            throw new BuildException("Invalid " + addrUserName + " recipient list");
        }
    }

    /**
     * here is where the mail is sent
     *
     * @exception MessagingException Description of Exception
     * @exception AddressException Description of Exception
     * @exception BuildException Description of Exception
     */
    public void doMail()
        throws MessagingException, AddressException, BuildException {
        Properties props = new Properties();
        props.put("mail.smtp.host", mailhost);

        //Aside, the JDK is clearly unaware of the scottish 'session', which
        //involves excessive quantities of alcohol :-)
        Session sesh = Session.getDefaultInstance(props, null);

        //create the message
        MimeMessage msg = new MimeMessage(sesh);

        //set the sender
        log("message sender: " + from, Project.MSG_VERBOSE);
        msg.setFrom(new InternetAddress(from));

        // add recipient lists
        addRecipients(msg, Message.RecipientType.TO,  "To",  toList);
        addRecipients(msg, Message.RecipientType.CC,  "Cc",  ccList);
        addRecipients(msg, Message.RecipientType.BCC, "Bcc", bccList);

        if (subject != null) {
            log("subject: " + subject, Project.MSG_VERBOSE);
            msg.setSubject(subject);
        }

        //now the complex bit; adding multiple mime objects. And guessing
        //the file type
        MimeMultipart attachments = new MimeMultipart();

        //first a message
        if (messageFile != null) {
            int size = (int)messageFile.length();
            byte data[] = new byte[size];

            try {
                FileInputStream inStream = new FileInputStream(messageFile);
                inStream.read(data);
                inStream.close();
                message = new String(data);
            } catch (IOException e) {
                throw new BuildException(e);
            }
        }

        if (message != null) {
            MimeBodyPart textbody = new MimeBodyPart();
            textbody.setContent(message, messageMimeType);
            attachments.addBodyPart(textbody);
        }

        for (int i = 0; i < filesets.size(); i++)
        {
            FileSet fs = (FileSet) filesets.elementAt(i);
            if (fs != null)
            {
                DirectoryScanner ds = fs.getDirectoryScanner(project);
                String[] dsfiles = ds.getIncludedFiles();
                File baseDir = ds.getBasedir();

                for (int j = 0; j < dsfiles.length; j++)
                {
                    File file = new File(baseDir, dsfiles[j]);
                    MimeBodyPart body;
                    body = new MimeBodyPart();
                    if (!file.exists() || !file.canRead()) {
                        throw new BuildException("File \"" + file.getAbsolutePath()
                                 + "\" does not exist or is not readable.");
                    }
                    log("Attaching " + file.toString()+" - " +file.length()+" bytes",
                        Project.MSG_VERBOSE);
                    FileDataSource fileData = new FileDataSource(file);
                    DataHandler fileDataHandler = new DataHandler(fileData);
                    body.setDataHandler(fileDataHandler);
                    body.setFileName(file.getName());
                    attachments.addBodyPart(body);
                } // for j
            } // if (fs != null)
        } // for i

        msg.setContent(attachments);
        log("sending email ");
        Transport.send(msg);
    }
}

