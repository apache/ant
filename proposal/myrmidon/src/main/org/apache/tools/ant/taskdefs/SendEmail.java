/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.mail.MailMessage;

/**
 * A task to send SMTP email. <p>
 *
 *
 * <tableborder="1" cellpadding="3" cellspacing="0">
 *
 *   <trbgcolor="#CCCCFF">
 *
 *     <th>
 *       Attribute
 *     </th>
 *
 *     <th>
 *       Description
 *     </th>
 *
 *     <th>
 *       Required
 *     </th>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       from
 *     </td>
 *
 *     <td>
 *       Email address of sender.
 *     </td>
 *
 *     <td>
 *       Yes
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       mailhost
 *     </td>
 *
 *     <td>
 *       Host name of the mail server.
 *     </td>
 *
 *     <td>
 *       No, default to &quot;localhost&quot;
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       toList
 *     </td>
 *
 *     <td>
 *       Comma-separated list of recipients.
 *     </td>
 *
 *     <td>
 *       Yes
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       subject
 *     </td>
 *
 *     <td>
 *       Email subject line.
 *     </td>
 *
 *     <td>
 *       No
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       files
 *     </td>
 *
 *     <td>
 *       Filename(s) of text to send in the body of the email. Multiple files
 *       are comma-separated.
 *     </td>
 *
 *     <tdrowspan="2">
 *       One of these two attributes
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       message
 *     </td>
 *
 *     <td>
 *       Message to send inthe body of the email.
 *     </td>
 *
 *   </tr>
 *
 * </table>
 *
 * <tr>
 *
 *   <td>
 *     includefilenames
 *   </td>
 *
 *   <td>
 *     Includes filenames before file contents when set to true.
 *   </td>
 *
 *   <td>
 *     No, default is <I>false</I>
 *   </td>
 *
 * </tr>
 * <p>
 *
 *
 *
 * @author glenn_twiggs@bmc.com
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 */
public class SendEmail extends Task
{
    private String mailhost = "localhost";
    private int mailport = MailMessage.DEFAULT_PORT;
    private Vector files = new Vector();
    /**
     * failure flag
     */
    private boolean failOnError = true;
    private String from;
    private boolean includefilenames;
    private String message;
    private String subject;
    private String toList;

    /**
     * Creates new SendEmail
     */
    public SendEmail()
    {
    }

    /**
     * Sets the FailOnError attribute of the MimeMail object
     *
     * @param failOnError The new FailOnError value
     * @since 1.5
     */
    public void setFailOnError( boolean failOnError )
    {
        this.failOnError = failOnError;
    }

    /**
     * Sets the file parameter of this build task.
     *
     * @param filenames Filenames to include as the message body of this email.
     */
    public void setFiles( String filenames )
        throws TaskException
    {
        StringTokenizer t = new StringTokenizer( filenames, ", " );

        while( t.hasMoreTokens() )
        {
            files.addElement( resolveFile( t.nextToken() ) );
        }
    }

    /**
     * Sets the from parameter of this build task.
     *
     * @param from Email address of sender.
     */
    public void setFrom( String from )
    {
        this.from = from;
    }

    /**
     * Sets Includefilenames attribute
     *
     * @param includefilenames Set to true if file names are to be included.
     * @since 1.5
     */
    public void setIncludefilenames( boolean includefilenames )
    {
        this.includefilenames = includefilenames;
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
     * Sets the mailport parameter of this build task.
     *
     * @param value mail port name.
     */
    public void setMailport( Integer value )
    {
        this.mailport = value.intValue();
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
     * Executes this build task.
     *
     * @throws TaskException if there is an error during task execution.
     */
    public void execute()
        throws TaskException
    {
        try
        {
            MailMessage mailMessage = new MailMessage( mailhost );
            mailMessage.setPort( mailport );

            if( from != null )
            {
                mailMessage.from( from );
            }
            else
            {
                throw new TaskException( "Attribute \"from\" is required." );
            }

            if( toList != null )
            {
                StringTokenizer t = new StringTokenizer( toList, ", ", false );

                while( t.hasMoreTokens() )
                {
                    mailMessage.to( t.nextToken() );
                }
            }
            else
            {
                throw new TaskException( "Attribute \"toList\" is required." );
            }

            if( subject != null )
            {
                mailMessage.setSubject( subject );
            }

            if( !files.isEmpty() )
            {
                PrintStream out = mailMessage.getPrintStream();

                for( Enumeration e = files.elements(); e.hasMoreElements(); )
                {
                    File file = (File)e.nextElement();

                    if( file.exists() && file.canRead() )
                    {
                        int bufsize = 1024;
                        int length;
                        byte[] buf = new byte[ bufsize ];
                        if( includefilenames )
                        {
                            String filename = file.getName();
                            int filenamelength = filename.length();
                            out.println( filename );
                            for( int star = 0; star < filenamelength; star++ )
                            {
                                out.print( '=' );
                            }
                            out.println();
                        }
                        BufferedInputStream in = null;
                        try
                        {
                            in = new BufferedInputStream(
                                new FileInputStream( file ), bufsize );
                            while( ( length = in.read( buf, 0, bufsize ) ) != -1 )
                            {
                                out.write( buf, 0, length );
                            }
                            if( includefilenames )
                            {
                                out.println();
                            }
                        }
                        finally
                        {
                            if( in != null )
                            {
                                try
                                {
                                    in.close();
                                }
                                catch( IOException ioe )
                                {
                                }
                            }
                        }

                    }
                    else
                    {
                        throw new TaskException( "File \"" + file.getName()
                                                 + "\" does not exist or is not readable." );
                    }
                }
            }
            else if( message != null )
            {
                PrintStream out = mailMessage.getPrintStream();
                out.print( message );
            }
            else
            {
                throw new TaskException( "Attribute \"file\" or \"message\" is required." );
            }

            log( "Sending email" );
            mailMessage.sendAndClose();
        }
        catch( IOException ioe )
        {
            String err = "IO error sending mail " + ioe.toString();
            if( failOnError )
            {
                throw new TaskException( err, ioe );
            }
            else
            {
                log( err, Project.MSG_ERR );
            }
        }
    }

}
