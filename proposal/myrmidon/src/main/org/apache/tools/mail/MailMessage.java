/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.mail;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * A class to help send SMTP email. This class is an improvement on the
 * sun.net.smtp.SmtpClient class found in the JDK. This version has extra
 * functionality, and can be used with JVMs that did not extend from the JDK.
 * It's not as robust as the JavaMail Standard Extension classes, but it's
 * easier to use and easier to install, and has an Open Source license. <p>
 *
 * It can be used like this: <blockquote><pre>
 * String mailhost = "localhost";  // or another mail host
 * String from = "Mail Message Servlet &lt;MailMessage@server.com&gt;";
 * String to = "to@you.com";
 * String cc1 = "cc1@you.com";
 * String cc2 = "cc2@you.com";
 * String bcc = "bcc@you.com";
 * &nbsp;
 * MailMessage msg = new MailMessage(mailhost);
 * msg.setPort(25);
 * msg.from(from);
 * msg.to(to);
 * msg.cc(cc1);
 * msg.cc(cc2);
 * msg.bcc(bcc);
 * msg.setSubject("Test subject");
 * PrintStream out = msg.getPrintStream();
 * &nbsp;
 * Iterator enum = req.getParameterNames();
 * while (enum.hasNext()) {
 *   String name = (String)enum.next();
 *   String value = req.getParameter(name);
 *   out.println(name + " = " + value);
 * }
 * &nbsp;
 * msg.sendAndClose();
 * </pre></blockquote> <p>
 *
 * Be sure to set the from address, then set the recepient addresses, then set
 * the subject and other headers, then get the PrintStream, then write the
 * message, and finally send and close. The class does minimal error checking
 * internally; it counts on the mail host to complain if there's any
 * malformatted input or out of order execution. <p>
 *
 * An attachment mechanism based on RFC 1521 could be implemented on top of this
 * class. In the meanwhile, JavaMail is the best solution for sending email with
 * attachments. <p>
 *
 * Still to do:
 * <ul>
 *   <li> Figure out how to close the connection in case of error
 * </ul>
 *
 *
 * @author Jason Hunter
 * @version 1.1, 2000/03/19, added angle brackets to address, helps some servers
 *      version 1.0, 1999/12/29
 */
public class MailMessage
{

    /**
     * default port for SMTP: 25
     */
    public final static int DEFAULT_PORT = 25;

    /**
     * host port for the mail server
     */
    private int port = DEFAULT_PORT;

    /**
     * list of email addresses to cc to
     */
    private ArrayList cc;

    /**
     * sender email address
     */
    private String from;

    /**
     * headers to send in the mail
     */
    private Hashtable headers;

    /**
     * host name for the mail server
     */
    private String host;

    private SmtpResponseReader in;

    private MailPrintStream out;

    private Socket socket;

    /**
     * list of email addresses to send to
     */
    private ArrayList to;

    /**
     * Constructs a new MailMessage to send an email. Use localhost as the mail
     * server.
     *
     * @exception IOException if there's any problem contacting the mail server
     */
    public MailMessage()
        throws IOException
    {
        this( "localhost" );
    }

    /**
     * Constructs a new MailMessage to send an email. Use the given host as the
     * mail server.
     *
     * @param host the mail server to use
     * @exception IOException if there's any problem contacting the mail server
     */
    public MailMessage( String host )
        throws IOException
    {
        this.host = host;
        to = new ArrayList();
        cc = new ArrayList();
        headers = new Hashtable();
        setHeader( "X-Mailer", "org.apache.tools.mail.MailMessage (jakarta.apache.org)" );
        connect();
        sendHelo();
    }

    // Make a limited attempt to extract a sanitized email address
    // Prefer text in <brackets>, ignore anything in (parentheses)
    static String sanitizeAddress( String s )
    {
        int paramDepth = 0;
        int start = 0;
        int end = 0;
        int len = s.length();

        for( int i = 0; i < len; i++ )
        {
            char c = s.charAt( i );
            if( c == '(' )
            {
                paramDepth++;
                if( start == 0 )
                {
                    end = i;// support "address (name)"
                }
            }
            else if( c == ')' )
            {
                paramDepth--;
                if( end == 0 )
                {
                    start = i + 1;// support "(name) address"
                }
            }
            else if( paramDepth == 0 && c == '<' )
            {
                start = i + 1;
            }
            else if( paramDepth == 0 && c == '>' )
            {
                end = i;
            }
        }

        if( end == 0 )
        {
            end = len;
        }

        return s.substring( start, end );
    }

    /**
     * Sets the named header to the given value. RFC 822 provides the rules for
     * what text may constitute a header name and value.
     *
     * @param name The new Header value
     * @param value The new Header value
     */
    public void setHeader( String name, String value )
    {
        // Blindly trust the user doesn't set any invalid headers
        headers.put( name, value );
    }

    /**
     * Set the port to connect to the SMTP host.
     *
     * @param port the port to use for connection.
     * @see #DEFAULT_PORT
     */
    public void setPort( int port )
    {
        this.port = port;
    }

    /**
     * Sets the subject of the mail message. Actually sets the "Subject" header.
     *
     * @param subj The new Subject value
     */
    public void setSubject( String subj )
    {
        headers.put( "Subject", subj );
    }

    /**
     * Returns a PrintStream that can be used to write the body of the message.
     * A stream is used since email bodies are byte-oriented. A writer could be
     * wrapped on top if necessary for internationalization.
     *
     * @return The PrintStream value
     * @exception IOException if there's any problem reported by the mail server
     */
    public PrintStream getPrintStream()
        throws IOException
    {
        setFromHeader();
        setToHeader();
        setCcHeader();
        sendData();
        flushHeaders();
        return out;
    }

    /**
     * Sets the bcc address. Does NOT set any header since it's a *blind* copy.
     * This method may be called multiple times.
     *
     * @param bcc Description of Parameter
     * @exception IOException if there's any problem reported by the mail server
     */
    public void bcc( String bcc )
        throws IOException
    {
        sendRcpt( bcc );
        // No need to keep track of Bcc'd addresses
    }

    /**
     * Sets the cc address. Also sets the "Cc" header. This method may be called
     * multiple times.
     *
     * @param cc Description of Parameter
     * @exception IOException if there's any problem reported by the mail server
     */
    public void cc( String cc )
        throws IOException
    {
        sendRcpt( cc );
        this.cc.add( cc );
    }

    /**
     * Sets the from address. Also sets the "From" header. This method should be
     * called only once.
     *
     * @param from Description of Parameter
     * @exception IOException if there's any problem reported by the mail server
     */
    public void from( String from )
        throws IOException
    {
        sendFrom( from );
        this.from = from;
    }

    /**
     * Sends the message and closes the connection to the server. The
     * MailMessage object cannot be reused.
     *
     * @exception IOException if there's any problem reported by the mail server
     */
    public void sendAndClose()
        throws IOException
    {
        sendDot();
        sendQuit();
        disconnect();
    }

    /**
     * Sets the to address. Also sets the "To" header. This method may be called
     * multiple times.
     *
     * @param to Description of Parameter
     * @exception IOException if there's any problem reported by the mail server
     */
    public void to( String to )
        throws IOException
    {
        sendRcpt( to );
        this.to.add( to );
    }

    void setCcHeader()
    {
        setHeader( "Cc", vectorToList( cc ) );
    }

    void setFromHeader()
    {
        setHeader( "From", from );
    }

    void setToHeader()
    {
        setHeader( "To", vectorToList( to ) );
    }

    void getReady()
        throws IOException
    {
        String response = in.getResponse();
        int[] ok = {220};
        if( !isResponseOK( response, ok ) )
        {
            throw new IOException(
                "Didn't get introduction from server: " + response );
        }
    }

    boolean isResponseOK( String response, int[] ok )
    {
        // Check that the response is one of the valid codes
        for( int i = 0; i < ok.length; i++ )
        {
            if( response.startsWith( "" + ok[ i ] ) )
            {
                return true;
            }
        }
        return false;
    }

    // * * * * * Raw protocol methods below here * * * * *

    void connect()
        throws IOException
    {
        socket = new Socket( host, port );
        out = new MailPrintStream(
            new BufferedOutputStream(
                socket.getOutputStream() ) );
        in = new SmtpResponseReader( socket.getInputStream() );
        getReady();
    }

    void disconnect()
        throws IOException
    {
        if( out != null )
        {
            out.close();
        }
        if( in != null )
        {
            in.close();
        }
        if( socket != null )
        {
            socket.close();
        }
    }

    void flushHeaders()
        throws IOException
    {
        // XXX Should I care about order here?
        Enumeration e = headers.keys();
        while( e.hasMoreElements() )
        {
            String name = (String)e.nextElement();
            String value = (String)headers.get( name );
            out.println( name + ": " + value );
        }
        out.println();
        out.flush();
    }

    void send( String msg, int[] ok )
        throws IOException
    {
        out.rawPrint( msg + "\r\n" );// raw supports <CRLF>.<CRLF>
        //System.out.println("S: " + msg);
        String response = in.getResponse();
        //System.out.println("R: " + response);
        if( !isResponseOK( response, ok ) )
        {
            throw new IOException(
                "Unexpected reply to command: " + msg + ": " + response );
        }
    }

    void sendData()
        throws IOException
    {
        int[] ok = {354};
        send( "DATA", ok );
    }

    void sendDot()
        throws IOException
    {
        int[] ok = {250};
        send( "\r\n.", ok );// make sure dot is on new line
    }

    void sendFrom( String from )
        throws IOException
    {
        int[] ok = {250};
        send( "MAIL FROM: " + "<" + sanitizeAddress( from ) + ">", ok );
    }

    void sendHelo()
        throws IOException
    {
        String local = InetAddress.getLocalHost().getHostName();
        int[] ok = {250};
        send( "HELO " + local, ok );
    }

    void sendQuit()
        throws IOException
    {
        int[] ok = {221};
        send( "QUIT", ok );
    }

    void sendRcpt( String rcpt )
        throws IOException
    {
        int[] ok = {250, 251};
        send( "RCPT TO: " + "<" + sanitizeAddress( rcpt ) + ">", ok );
    }

    String vectorToList( ArrayList v )
    {
        StringBuffer buf = new StringBuffer();
        Iterator e = v.iterator();
        while( e.hasNext() )
        {
            buf.append( e.next() );
            if( e.hasNext() )
            {
                buf.append( ", " );
            }
        }
        return buf.toString();
    }
}

// This PrintStream subclass makes sure that <CRLF>. becomes <CRLF>..
// per RFC 821.  It also ensures that new lines are always \r\n.
//

class MailPrintStream extends PrintStream
{

    int lastChar;

    public MailPrintStream( OutputStream out )
    {
        super( out, true );// deprecated, but email is byte-oriented
    }

    // Mac does \n\r, but that's tough to distinguish from Windows \r\n\r\n.
    // Don't tackle that problem right now.
    public void write( int b )
    {
        if( b == '\n' && lastChar != '\r' )
        {
            rawWrite( '\r' );// ensure always \r\n
            rawWrite( b );
        }
        else if( b == '.' && lastChar == '\n' )
        {
            rawWrite( '.' );// add extra dot
            rawWrite( b );
        }
        else
        {
            rawWrite( b );
        }
        lastChar = b;
    }

    public void write( byte buf[], int off, int len )
    {
        for( int i = 0; i < len; i++ )
        {
            write( buf[ off + i ] );
        }
    }

    void rawPrint( String s )
    {
        int len = s.length();
        for( int i = 0; i < len; i++ )
        {
            rawWrite( s.charAt( i ) );
        }
    }

    void rawWrite( int b )
    {
        super.write( b );
    }
}

