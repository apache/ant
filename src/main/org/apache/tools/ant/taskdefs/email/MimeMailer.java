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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

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

import org.apache.tools.ant.BuildException;

/**
 * Uses the JavaMail classes to send Mime format email.
 *
 * @author roxspring@yahoo.com Rob Oxspring
 * @author <a href="mailto:ishu@akm.ru">Aleksandr Ishutin</a>
 * @since Ant 1.5
 */
class MimeMailer extends Mailer {
    // Default character set
    private static final String defaultCharset = System.getProperty("file.encoding");

    // To work poperly with national charsets we have to use
    // implementation of interface javax.activation.DataSource
    /**
     * @since Ant 1.6
     */
    class StringDataSource implements javax.activation.DataSource {
      private String data=null;
      private String type=null;
      private String charset = null;
      private ByteArrayOutputStream out;

      public InputStream getInputStream() throws IOException {
        if(data == null && out == null)
          throw new IOException("No data");
        else {
          if(out!=null) {
            data=(data!=null)?data.concat(out.toString(charset)):out.toString(charset);
            out=null;
          }
          return new ByteArrayInputStream(data.getBytes(charset));
        }
      }

      public OutputStream getOutputStream() throws IOException {
        if(out==null) {
          out=new ByteArrayOutputStream();
        }
        return out;
      }

      public void setContentType(String type) {
        this.type=type.toLowerCase();
      }

      public String getContentType() {
        if(type !=null && type.indexOf("charset")>0 && type.startsWith("text/"))
          return type;
        // Must be like "text/plain; charset=windows-1251"
        return type!=null?type.concat("; charset=".concat(charset)):
                     "text/plain".concat("; charset=".concat(charset));
      }

      public String getName() {
        return "StringDataSource";
      }
      public void setCharset(String charset) {
        this.charset = charset;
      }
      public String getCharset() {
        return charset;
      }
  }

  /** Sends the email  */
  public void send() {
        try {
            Properties props = new Properties();

            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", String.valueOf(port));

            // Aside, the JDK is clearly unaware of the scottish
            // 'session', which //involves excessive quantities of
            // alcohol :-)
            Session sesh = Session.getDefaultInstance(props, null);

            //create the message
            MimeMessage msg = new MimeMessage(sesh);
            MimeMultipart attachments = new MimeMultipart();

            //set the sender
            if (from.getName() == null) {
                msg.setFrom(new InternetAddress(from.getAddress()));
            } else {
                msg.setFrom(new InternetAddress(from.getAddress(),
                    from.getName()));
            }
            // set the reply to addresses
            msg.setReplyTo(internetAddresses(replyToList));
            msg.setRecipients(Message.RecipientType.TO,
                internetAddresses(toList));
            msg.setRecipients(Message.RecipientType.CC,
                internetAddresses(ccList));
            msg.setRecipients(Message.RecipientType.BCC,
                internetAddresses(bccList));

            // Choosing character set of the mail message
            // First: looking it from MimeType
            String charset = parseCharSetFromMimeType(message.getMimeType());
            if(charset!=null) {
              // Assign/reassign message charset from MimeType
                message.setCharset(charset);
            }
            // Next: looking if charset having explict definition
            else {
              charset = message.getCharset();
              if(charset==null) {
                // Using default
                charset=defaultCharset;
                message.setCharset(charset);
              }
            }

            // Using javax.activation.DataSource paradigm
            StringDataSource sds = new StringDataSource();
            sds.setContentType(message.getMimeType());
            sds.setCharset(charset);

            if (subject != null)
                msg.setSubject(subject,charset);
            msg.addHeader("Date", getDate());

            PrintStream out = new PrintStream(sds.getOutputStream());
            message.print(out);
            out.close();

            MimeBodyPart textbody = new MimeBodyPart();
            textbody.setDataHandler(new DataHandler(sds));
            attachments.addBodyPart(textbody);

            Enumeration e = files.elements();

            while (e.hasMoreElements()) {
                File file = (File) e.nextElement();

                MimeBodyPart body;

                body = new MimeBodyPart();
                if (!file.exists() || !file.canRead()) {
                    throw new BuildException("File \"" + file.getAbsolutePath()
                         + "\" does not exist or is not "
                         + "readable.");
                }
                FileDataSource fileData = new FileDataSource(file);
                DataHandler fileDataHandler = new DataHandler(fileData);

                body.setDataHandler(fileDataHandler);
                body.setFileName(file.getName());
                attachments.addBodyPart(body);
            }

            msg.setContent(attachments);
            Transport.send(msg);
        } catch (MessagingException e) {
            throw new BuildException("Problem while sending mime mail:", e);
        } catch (IOException e) {
            throw new BuildException("Problem while sending mime mail:", e);
        }
    }


    private static InternetAddress[] internetAddresses(Vector list)
         throws AddressException, UnsupportedEncodingException {
        InternetAddress[] addrs = new InternetAddress[list.size()];

        for (int i = 0; i < list.size(); ++i) {
            EmailAddress addr = (EmailAddress) list.elementAt(i);

            if (addr.getName() == null) {
                addrs[i] = new InternetAddress(addr.getAddress());
            } else {
                addrs[i] = new InternetAddress(addr.getAddress(),
                    addr.getName());
            }
        }

        return addrs;
    }

    private String parseCharSetFromMimeType(String type){
      int pos;
      if(type==null || (pos=type.indexOf("charset"))<0)
        return null;
      // Assuming mime type in form "text/XXXX; charset=XXXXXX"
      StringTokenizer token = new StringTokenizer(type.substring(pos),"=; ");
      token.nextToken();// Skip 'charset='
      return token.nextToken();
    }
}

