/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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

/*
 * The original version of this class was donated by Jason Hunter,
 * who wrote the class as part of the com.oreilly.servlet
 * package for his book "Java Servlet Programming" (O'Reilly).
 * See http://www.servlets.com.
 *
 */

package org.apache.tools.mail;

import java.io.IOException;
import java.io.PrintStream;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.InetAddress;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

/**
 * A class to help send SMTP email.
 * This class is an improvement on the sun.net.smtp.SmtpClient class
 * found in the JDK.  This version has extra functionality, and can be used
 * with JVMs that did not extend from the JDK.  It's not as robust as
 * the JavaMail Standard Extension classes, but it's easier to use and
 * easier to install, and has an Open Source license.
 * <p>
 * It can be used like this:
 * <blockquote><pre>
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
 * Enumeration enum = req.getParameterNames();
 * while (enum.hasMoreElements()) {
 *   String name = (String)enum.nextElement();
 *   String value = req.getParameter(name);
 *   out.println(name + " = " + value);
 * }
 * &nbsp;
 * msg.sendAndClose();
 * </pre></blockquote>
 * <p>
 * Be sure to set the from address, then set the recepient
 * addresses, then set the subject and other headers, then get the
 * PrintStream, then write the message, and finally send and close.
 * The class does minimal error checking internally; it counts on the mail
 * host to complain if there's any malformatted input or out of order
 * execution.
 * <p>
 * An attachment mechanism based on RFC 1521 could be implemented on top of
 * this class.  In the meanwhile, JavaMail is the best solution for sending
 * email with attachments.
 * <p>
 * Still to do:
 * <ul>
 * <li>Figure out how to close the connection in case of error
 * </ul>
 *
 * @author Jason Hunter
 * @version 1.1, 2000/03/19, added angle brackets to address, helps some servers
 * version 1.0, 1999/12/29
 */
public class MailMessage {

    /** default port for SMTP: 25 */
    public static final int DEFAULT_PORT = 25;

    /** host name for the mail server */
    private String host;

    /** host port for the mail server */
    private int port = DEFAULT_PORT;

    /** sender email address */
    private String from;

    /** list of email addresses to send to */
    private Vector to;

    /** list of email addresses to cc to */
    private Vector cc;

    /** headers to send in the mail */
    private Hashtable headers;

    private MailPrintStream out;

    private SmtpResponseReader in;

    private Socket socket;

  /**
   * Constructs a new MailMessage to send an email.
   * Use localhost as the mail server with port 25.
   *
   * @exception IOException if there's any problem contacting the mail server
   */
  public MailMessage() throws IOException {
    this("localhost",DEFAULT_PORT);
  }

  /**
   * Constructs a new MailMessage to send an email.
   * Use the given host as the mail server with port 25.
   *
   * @param host the mail server to use
   * @exception IOException if there's any problem contacting the mail server
   */
  public MailMessage(String host) throws IOException {
      this(host,DEFAULT_PORT);
  }

  /**
   * Constructs a new MailMessage to send an email.
   * Use the given host and port as the mail server.
   *
   * @param host the mail server to use
   * @param port the port to connect to
   * @exception IOException if there's any problem contacting the mail server
   */
  public MailMessage(String host, int port) throws IOException{
    this.port = port;
    this.host = host;
    to = new Vector();
    cc = new Vector();
    headers = new Hashtable();
    setHeader("X-Mailer", "org.apache.tools.mail.MailMessage (jakarta.apache.org)");
    connect();
    sendHelo();
  }

    /**
     * Set the port to connect to the SMTP host.
     * @param port the port to use for connection.
     * @see #DEFAULT_PORT
     */
    public void setPort(int port){
        this.port = port;
    }

  /**
   * Sets the from address.  Also sets the "From" header.  This method should
   * be called only once.
   *
   * @exception IOException if there's any problem reported by the mail server
   */
  public void from(String from) throws IOException {
    sendFrom(from);
    this.from = from;
  }

  /**
   * Sets the to address.  Also sets the "To" header.  This method may be
   * called multiple times.
   *
   * @exception IOException if there's any problem reported by the mail server
   */
  public void to(String to) throws IOException {
    sendRcpt(to);
    this.to.addElement(to);
  }

  /**
   * Sets the cc address.  Also sets the "Cc" header.  This method may be
   * called multiple times.
   *
   * @exception IOException if there's any problem reported by the mail server
   */
  public void cc(String cc) throws IOException {
    sendRcpt(cc);
    this.cc.addElement(cc);
  }

  /**
   * Sets the bcc address.  Does NOT set any header since it's a *blind* copy.
   * This method may be called multiple times.
   *
   * @exception IOException if there's any problem reported by the mail server
   */
  public void bcc(String bcc) throws IOException {
    sendRcpt(bcc);
    // No need to keep track of Bcc'd addresses
  }

  /**
   * Sets the subject of the mail message.  Actually sets the "Subject"
   * header.
   */
  public void setSubject(String subj) {
    headers.put("Subject", subj);
  }

  /**
   * Sets the named header to the given value.  RFC 822 provides the rules for
   * what text may constitute a header name and value.
   */
  public void setHeader(String name, String value) {
    // Blindly trust the user doesn't set any invalid headers
    headers.put(name, value);
  }

  /**
   * Returns a PrintStream that can be used to write the body of the message.
   * A stream is used since email bodies are byte-oriented.  A writer could
   * be wrapped on top if necessary for internationalization.
   *
   * @exception IOException if there's any problem reported by the mail server
   */
  public PrintStream getPrintStream() throws IOException {
    setFromHeader();
    setToHeader();
    setCcHeader();
    sendData();
    flushHeaders();
    return out;
  }

  void setFromHeader() {
    setHeader("From", from);
  }

  void setToHeader() {
    setHeader("To", vectorToList(to));
  }

  void setCcHeader() {
    setHeader("Cc", vectorToList(cc));
  }

  String vectorToList(Vector v) {
    StringBuffer buf = new StringBuffer();
    Enumeration e = v.elements();
    while (e.hasMoreElements()) {
      buf.append(e.nextElement());
      if (e.hasMoreElements()) {
        buf.append(", ");
      }
    }
    return buf.toString();
  }

  void flushHeaders() throws IOException {
    // XXX Should I care about order here?
    Enumeration e = headers.keys();
    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();
      String value = (String) headers.get(name);
      out.println(name + ": " + value);
    }
    out.println();
    out.flush();
  }

  /**
   * Sends the message and closes the connection to the server.
   * The MailMessage object cannot be reused.
   *
   * @exception IOException if there's any problem reported by the mail server
   */
  public void sendAndClose() throws IOException {
      try {
          sendDot();
          sendQuit();
      } finally {
          disconnect();
      }
  }

  // Make a limited attempt to extract a sanitized email address
  // Prefer text in <brackets>, ignore anything in (parentheses)
  static String sanitizeAddress(String s) {
    int paramDepth = 0;
    int start = 0;
    int end = 0;
    int len = s.length();

    for (int i = 0; i < len; i++) {
      char c = s.charAt(i);
      if (c == '(') {
        paramDepth++;
        if (start == 0) {
          end = i;  // support "address (name)"
        }
      } else if (c == ')') {
        paramDepth--;
        if (end == 0) {
          start = i + 1;  // support "(name) address"
        }
      } else if (paramDepth == 0 && c == '<') {
        start = i + 1;
      } else if (paramDepth == 0 && c == '>') {
        end = i;
      }
    }

    if (end == 0) {
      end = len;
    }

    return s.substring(start, end);
  }

  // * * * * * Raw protocol methods below here * * * * *

  void connect() throws IOException {
    socket = new Socket(host, port);
    out = new MailPrintStream(
          new BufferedOutputStream(
          socket.getOutputStream()));
    in = new SmtpResponseReader(socket.getInputStream());
    getReady();
  }

  void getReady() throws IOException {
    String response = in.getResponse();
    int[] ok = { 220 };
    if (!isResponseOK(response, ok)) {
      throw new IOException(
        "Didn't get introduction from server: " + response);
    }
  }

  void sendHelo() throws IOException {
    String local = InetAddress.getLocalHost().getHostName();
    int[] ok = { 250 };
    send("HELO " + local, ok);
  }

  void sendFrom(String from) throws IOException {
    int[] ok = { 250 };
    send("MAIL FROM: " + "<" + sanitizeAddress(from) + ">", ok);
  }

  void sendRcpt(String rcpt) throws IOException {
    int[] ok = { 250, 251 };
    send("RCPT TO: " + "<" + sanitizeAddress(rcpt) + ">", ok);
  }

  void sendData() throws IOException {
    int[] ok = { 354 };
    send("DATA", ok);
  }

  void sendDot() throws IOException {
    int[] ok = { 250 };
    send("\r\n.", ok);  // make sure dot is on new line
  }

    void sendQuit() throws IOException {
        int[] ok = { 221 };
        try {
            send("QUIT", ok);
        } catch (IOException e) {
            throw new ErrorInQuitException(e);
        }
    }

    void send(String msg, int[] ok) throws IOException {
        out.rawPrint(msg + "\r\n");  // raw supports <CRLF>.<CRLF>
        String response = in.getResponse();
        if (!isResponseOK(response, ok)) {
            throw new IOException("Unexpected reply to command: "
                                  + msg + ": " + response);
        }
    }

  boolean isResponseOK(String response, int[] ok) {
    // Check that the response is one of the valid codes
    for (int i = 0; i < ok.length; i++) {
      if (response.startsWith("" + ok[i])) {
        return true;
      }
    }
    return false;
  }

    void disconnect() throws IOException {
        if (out != null) {
            out.close();
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }
}

// This PrintStream subclass makes sure that <CRLF>. becomes <CRLF>..
// per RFC 821.  It also ensures that new lines are always \r\n.
//
class MailPrintStream extends PrintStream {

  int lastChar;

  public MailPrintStream(OutputStream out) {
    super(out, true);  // deprecated, but email is byte-oriented
  }

  // Mac does \n\r, but that's tough to distinguish from Windows \r\n\r\n.
  // Don't tackle that problem right now.
  public void write(int b) {
    if (b == '\n' && lastChar != '\r') {
      rawWrite('\r');  // ensure always \r\n
      rawWrite(b);
    } else if (b == '.' && lastChar == '\n') {
      rawWrite('.');  // add extra dot
      rawWrite(b);
    } else {
      rawWrite(b);
    }
    lastChar = b;
  }

  public void write(byte[] buf, int off, int len) {
    for (int i = 0; i < len; i++) {
      write(buf[off + i]);
    }
  }

  void rawWrite(int b) {
    super.write(b);
  }

  void rawPrint(String s) {
    int len = s.length();
    for (int i = 0; i < len; i++) {
      rawWrite(s.charAt(i));
    }
  }
}

