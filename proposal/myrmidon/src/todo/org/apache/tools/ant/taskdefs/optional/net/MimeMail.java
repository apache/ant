/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.TaskContext;
import org.apache.tools.todo.types.DirectoryScanner;
import org.apache.tools.todo.types.FileSet;
import org.apache.tools.todo.types.ScannerUtil;

/**
 * A task to send SMTP email. This version has near identical syntax to the
 * SendEmail task, but is MIME aware. It also requires Sun's mail.jar and
 * activation.jar to compile and execute, which puts it clearly into the very
 * optional category.
 *
 * @author glenn_twiggs@bmc.com
 * @author steve_l@iseran.com steve loughran
 * @author ehatcher@apache.org Erik Hatcher
 * @author paulo.gaspar@krankikom.de Paulo Gaspar
 * @created 01 May 2001
 */
public class MimeMail extends AbstractTask
{
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
     * message file (mutually exclusive from message)
     */
    private File messageFile = null;

    /**
     * TO recipients
     */
    private String toList = null;

    /**
     * CC (Carbon Copy) recipients
     */
    protected String ccList = null;

    /**
     * BCC (Blind Carbon Copy) recipients
     */
    protected String bccList = null;

    /**
     * subject field
     */
    private String subject = null;

    /**
     * file list
     */
    private ArrayList filesets = new ArrayList();

    /**
     * type of the text message, plaintext by default but text/html or text/xml
     * is quite feasible
     */
    private String messageMimeType = "text/plain";

    /**
     * Creates new instance
     */
    public MimeMail()
    {
    }

    // helper method to add recipients
    private static void addRecipients( MimeMessage msg,
                                       Message.RecipientType recipType,
                                       String addrUserName,
                                       String addrList
                                       )
        throws MessagingException, TaskException
    {
        if( ( null == addrList ) || ( addrList.trim().length() <= 0 ) )
        {
            return;
        }

        try
        {
            InternetAddress[] addrArray = InternetAddress.parse( addrList );

            if( ( null == addrArray ) || ( 0 == addrArray.length ) )
            {
                throw new TaskException( "Empty " + addrUserName + " recipients list was specified" );
            }

            msg.setRecipients( recipType, addrArray );
        }
        catch( AddressException ae )
        {
            throw new TaskException( "Invalid " + addrUserName + " recipient list" );
        }
    }

    /**
     * Sets the toList parameter of this build task.
     *
     * @param bccList The new BccList value
     */
    public void setBccList( String bccList )
    {
        this.bccList = bccList;
    }

    /**
     * Sets the toList parameter of this build task.
     *
     * @param ccList The new CcList value
     */
    public void setCcList( String ccList )
    {
        this.ccList = ccList;
    }

    /**
     * Sets the FailOnError attribute of the MimeMail object
     *
     * @param failOnError The new FailOnError value
     */
    public void setFailOnError( boolean failOnError )
    {
        this.failOnError = failOnError;
    }

    /**
     * Sets the "from" parameter of this build task.
     *
     * @param from Email address of sender.
     */
    public void setFrom( String from )
    {
        this.from = from;
    }

    /**
     * Sets the mailhost parameter of this build task.
     *
     * @param mailhost Mail host name.
     */
    public void setMailhost( String mailhost )
    {
        this.mailhost = mailhost;
    }

    /**
     * Sets the message parameter of this build task.
     *
     * @param message Message body of this email.
     */
    public void setMessage( String message )
    {
        this.message = message;
    }

    public void setMessageFile( File messageFile )
    {
        this.messageFile = messageFile;
    }

    /**
     * set type of the text message, plaintext by default but text/html or
     * text/xml is quite feasible
     *
     * @param type The new MessageMimeType value
     */
    public void setMessageMimeType( String type )
    {
        this.messageMimeType = type;
    }

    /**
     * Sets the subject parameter of this build task.
     *
     * @param subject Subject of this email.
     */
    public void setSubject( String subject )
    {
        this.subject = subject;
    }

    /**
     * Sets the toList parameter of this build task.
     *
     * @param toList Comma-separated list of email recipient addreses.
     */
    public void setToList( String toList )
    {
        this.toList = toList;
    }

    /**
     * Adds a set of files (nested fileset attribute).
     *
     * @param set The feature to be added to the Fileset attribute
     */
    public void addFileset( FileSet set )
    {
        filesets.add( set );
    }

    /**
     * here is where the mail is sent
     *
     * @exception MessagingException Description of Exception
     * @exception AddressException Description of Exception
     * @exception TaskException Description of Exception
     */
    public void doMail()
        throws MessagingException, AddressException, TaskException
    {
        Properties props = new Properties();
        props.put( "mail.smtp.host", mailhost );

        //Aside, the JDK is clearly unaware of the scottish 'session', which
        //involves excessive quantities of alcohol :-)
        Session sesh = Session.getDefaultInstance( props, null );

        //create the message
        MimeMessage msg = new MimeMessage( sesh );

        //set the sender
        getContext().debug( "message sender: " + from );
        msg.setFrom( new InternetAddress( from ) );

        // add recipient lists
        addRecipients( msg, Message.RecipientType.TO, "To", toList );
        addRecipients( msg, Message.RecipientType.CC, "Cc", ccList );
        addRecipients( msg, Message.RecipientType.BCC, "Bcc", bccList );

        if( subject != null )
        {
            getContext().debug( "subject: " + subject );
            msg.setSubject( subject );
        }

        //now the complex bit; adding multiple mime objects. And guessing
        //the file type
        MimeMultipart attachments = new MimeMultipart();

        //first a message
        if( messageFile != null )
        {
            int size = (int)messageFile.length();
            byte data[] = new byte[ size ];

            try
            {
                FileInputStream inStream = new FileInputStream( messageFile );
                inStream.read( data );
                inStream.close();
                message = new String( data );
            }
            catch( IOException e )
            {
                throw new TaskException( "Error", e );
            }
        }

        if( message != null )
        {
            MimeBodyPart textbody = new MimeBodyPart();
            textbody.setContent( message, messageMimeType );
            attachments.addBodyPart( textbody );
        }

        for( int i = 0; i < filesets.size(); i++ )
        {
            FileSet fs = (FileSet)filesets.get( i );
            if( fs != null )
            {
                DirectoryScanner ds = ScannerUtil.getDirectoryScanner( fs );
                String[] dsfiles = ds.getIncludedFiles();
                File baseDir = ds.getBasedir();

                for( int j = 0; j < dsfiles.length; j++ )
                {
                    File file = new File( baseDir, dsfiles[ j ] );
                    MimeBodyPart body;
                    body = new MimeBodyPart();
                    if( !file.exists() || !file.canRead() )
                    {
                        throw new TaskException( "File \"" + file.getAbsolutePath()
                                                 + "\" does not exist or is not readable." );
                    }
                    getContext().debug( "Attaching " + file.toString() + " - " + file.length() + " bytes" );
                    FileDataSource fileData = new FileDataSource( file );
                    DataHandler fileDataHandler = new DataHandler( fileData );
                    body.setDataHandler( fileDataHandler );
                    body.setFileName( file.getName() );
                    attachments.addBodyPart( body );
                }// for j
            }// if (fs != null)
        }// for i

        msg.setContent( attachments );
        getContext().info( "sending email " );
        Transport.send( msg );
    }

    /**
     * Executes this build task. throws org.apache.tools.ant.TaskException if
     * there is an error during task execution.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        try
        {
            validate();
            doMail();
        }
        catch( Exception e )
        {
            if( failOnError )
            {
                throw new TaskException( "Error", e );
            }
            else
            {
                String text = e.toString();
                getContext().error( text );
            }
        }
    }

    /**
     * verify parameters
     *
     * @throws TaskException if something is invalid
     */
    public void validate()
    {
        if( from == null )
        {
            throw new TaskException( "Attribute \"from\" is required." );
        }

        if( ( toList == null ) && ( ccList == null ) && ( bccList == null ) )
        {
            throw new TaskException( "Attribute \"toList\", \"ccList\" or \"bccList\" is required." );
        }

        if( message == null && filesets.isEmpty() && messageFile == null )
        {
            throw new TaskException( "FileSet, \"message\", or \"messageFile\" is required." );
        }

        if( message != null && messageFile != null )
        {
            throw new TaskException( "Only one of \"message\" or \"messageFile\" may be specified." );
        }
    }
}

