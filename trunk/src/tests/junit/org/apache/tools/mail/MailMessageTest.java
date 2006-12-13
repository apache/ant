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

package org.apache.tools.mail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.tools.mail.MailMessage;

import junit.framework.TestCase;

/**
 * JUnit 3 testcases for org.apache.tools.mail.MailMessage.
 *
 * @since Ant 1.6
 */
public class MailMessageTest extends TestCase {

    // 27224 = magic (a random port which is unlikely to be in use)
    private static int TEST_PORT = 27224;

    private String local = null;

    public MailMessageTest(String name) {
        super(name);
    }

    public void setUp() {
        try {
            local = InetAddress.getLocalHost().getHostName();
        } catch (java.net.UnknownHostException uhe) {
            // ignore
        }
    }

    /**
     *  Test an example that is similar to the one given in the API
     *  If this testcase takes >90s to complete, it is very likely that
     *  the two threads are blocked waiting for each other and Thread.join()
     *  timed out.
     */
    public void testAPIExample() {

        ServerThread testMailServer = new ServerThread();
        Thread server = new Thread(testMailServer);
        server.start();

        ClientThread testMailClient = new ClientThread();

        testMailClient.from("Mail Message <EmailTaskTest@ant.apache.org>");
        testMailClient.to("to@you.com");
        testMailClient.cc("cc1@you.com");
        testMailClient.cc("cc2@you.com");
        testMailClient.bcc("bcc@you.com");
        testMailClient.setSubject("Test subject");
        testMailClient.setMessage( "test line 1\n" +
            "test line 2" );

        Thread client = new Thread(testMailClient);
        client.start();

        try {
            server.join(60 * 1000); // 60s
            client.join(30 * 1000); // a further 30s
        } catch (InterruptedException ie ) {
            fail( "InterruptedException: " + ie );
        }

        String result = testMailServer.getResult();
        String expectedResult = "220 test SMTP EmailTaskTest\r\n" +
        "HELO " + local + "\r\n" +
        "250 " + local + " Hello " + local + " [127.0.0.1], pleased to meet you\r\n" +
        "MAIL FROM: <EmailTaskTest@ant.apache.org>\r\n" +
        "250\r\n" +
        "RCPT TO: <to@you.com>\r\n" +
        "250\r\n" +
        "RCPT TO: <cc1@you.com>\r\n" +
        "250\r\n" +
        "RCPT TO: <cc2@you.com>\r\n" +
        "250\r\n" +
        "RCPT TO: <bcc@you.com>\r\n" +
        "250\r\n" +
        "DATA\r\n" +
        "354\r\n" +
        "Subject: Test subject\r\n" +
        "From: Mail Message <EmailTaskTest@ant.apache.org>\r\n" +
        "To: to@you.com\r\n" +
        "Cc: cc1@you.com, cc2@you.com\r\n" +
        "X-Mailer: org.apache.tools.mail.MailMessage (ant.apache.org)\r\n" +
        "\r\n" +
        "test line 1\r\n" +
        "test line 2\r\n" +
        "\r\n" +
        ".\r\n" +
        "250\r\n" +
        "QUIT\r\n" +
        "221\r\n";
        for (int icounter = 0; icounter<expectedResult.length(); icounter++) {
            if (icounter < result.length()) {
                if (expectedResult.charAt(icounter) != result.charAt(icounter)) {
                    System.out.println("posit " + icounter + " expected "
                        + expectedResult.charAt(icounter)
                    + " result " + result.charAt(icounter));
                }
            }
        }
        if (expectedResult.length()>result.length()) {
            System.out.println("excedent of expected result "
                + expectedResult.substring(result.length()));
        }
        if (expectedResult.length()<result.length()) {
            System.out.println("excedent of result "
                + result.substring(expectedResult.length()));
        }
        assertEquals(expectedResult.length(), result.length());
        assertEquals(expectedResult, result); // order of headers cannot be guaranteed
        if (testMailClient.isFailed()) {
            fail(testMailClient.getFailMessage());
        }
    }

    /**
     *  Test a MailMessage with no cc or bcc lines
     */
    public void testToOnly() {
        ServerThread testMailServer = new ServerThread();
        Thread server = new Thread(testMailServer);
        server.start();

        ClientThread testMailClient = new ClientThread();

        testMailClient.from("Mail Message <EmailTaskTest@ant.apache.org>");
        testMailClient.to("to@you.com");
        testMailClient.setSubject("Test subject");
        testMailClient.setMessage( "test line 1\n" +
            "test line 2" );

        Thread client = new Thread(testMailClient);
        client.start();

        try {
            server.join(60 * 1000); // 60s
            client.join(30 * 1000); // a further 30s
        } catch (InterruptedException ie ) {
            fail("InterruptedException: " + ie);
        }

        String result = testMailServer.getResult();
        String expectedResult = "220 test SMTP EmailTaskTest\r\n" +
        "HELO " + local + "\r\n" +
        "250 " + local + " Hello " + local + " [127.0.0.1], pleased to meet you\r\n" +
        "MAIL FROM: <EmailTaskTest@ant.apache.org>\r\n" +
        "250\r\n" +
        "RCPT TO: <to@you.com>\r\n" +
        "250\r\n" +
        "DATA\r\n" +
        "354\r\n" +
        "Subject: Test subject\r\n" +
            "From: Mail Message <EmailTaskTest@ant.apache.org>\r\n" +
            "To: to@you.com\r\n" +
        "X-Mailer: org.apache.tools.mail.MailMessage (ant.apache.org)\r\n" +
        "\r\n" +
        "test line 1\r\n" +
        "test line 2\r\n" +
        "\r\n" +
        ".\r\n" +
        "250\r\n" +
        "QUIT\r\n" +
        "221\r\n";
        assertEquals(expectedResult.length(), result.length());
        assertEquals(expectedResult, result); // order of headers cannot be guaranteed
        if (testMailClient.isFailed()) {
            fail(testMailClient.getFailMessage());
        }
    }


    /**
     *  Test a MailMessage with no to or bcc lines
     */
    public void testCcOnly() {
        ServerThread testMailServer = new ServerThread();
        Thread server = new Thread(testMailServer);
        server.start();

        ClientThread testMailClient = new ClientThread();

        testMailClient.from("Mail Message <EmailTaskTest@ant.apache.org>");
        testMailClient.cc("cc@you.com");
        testMailClient.setSubject("Test subject");
        testMailClient.setMessage( "test line 1\n" +
            "test line 2" );

        Thread client = new Thread(testMailClient);
        client.start();

        try {
            server.join(60 * 1000); // 60s
            client.join(30 * 1000); // a further 30s
        } catch (InterruptedException ie ) {
            fail( "InterruptedException: " + ie );
        }

        String result = testMailServer.getResult();
        String expectedResult = "220 test SMTP EmailTaskTest\r\n" +
        "HELO " + local + "\r\n" +
        "250 " + local + " Hello " + local + " [127.0.0.1], pleased to meet you\r\n" +
        "MAIL FROM: <EmailTaskTest@ant.apache.org>\r\n" +
        "250\r\n" +
        "RCPT TO: <cc@you.com>\r\n" +
        "250\r\n" +
        "DATA\r\n" +
        "354\r\n" +
        "Subject: Test subject\r\n" +
            "From: Mail Message <EmailTaskTest@ant.apache.org>\r\n" +
            "Cc: cc@you.com\r\n" +
        "X-Mailer: org.apache.tools.mail.MailMessage (ant.apache.org)\r\n" +
        "\r\n" +
        "test line 1\r\n" +
        "test line 2\r\n" +
        "\r\n" +
        ".\r\n" +
        "250\r\n" +
        "QUIT\r\n" +
        "221\r\n";
        assertEquals(expectedResult.length(), result.length());
        assertEquals(expectedResult, result);
        if (testMailClient.isFailed()) {
            fail(testMailClient.getFailMessage());
        }
    }


    /**
     *  Test a MailMessage with no to or cc lines
     */
    public void testBccOnly() {
        ServerThread testMailServer = new ServerThread();
        Thread server = new Thread(testMailServer);
        server.start();

        ClientThread testMailClient = new ClientThread();

        testMailClient.from("Mail Message <EmailTaskTest@ant.apache.org>");
        testMailClient.bcc("bcc@you.com");
        testMailClient.setSubject("Test subject");
        testMailClient.setMessage( "test line 1\n" +
            "test line 2" );

        Thread client = new Thread(testMailClient);
        client.start();

        try {
            server.join(60 * 1000); // 60s
            client.join(30 * 1000); // a further 30s
        } catch (InterruptedException ie ) {
            fail( "InterruptedException: " + ie );
        }

        String result = testMailServer.getResult();
        String expectedResult = "220 test SMTP EmailTaskTest\r\n" +
        "HELO " + local + "\r\n" +
        "250 " + local + " Hello " + local + " [127.0.0.1], pleased to meet you\r\n" +
        "MAIL FROM: <EmailTaskTest@ant.apache.org>\r\n" +
        "250\r\n" +
        "RCPT TO: <bcc@you.com>\r\n" +
        "250\r\n" +
        "DATA\r\n" +
        "354\r\n" +
        "Subject: Test subject\r\n" +
        "From: Mail Message <EmailTaskTest@ant.apache.org>\r\n" +
        "X-Mailer: org.apache.tools.mail.MailMessage (ant.apache.org)\r\n" +
        "\r\n" +
        "test line 1\r\n" +
        "test line 2\r\n" +
        "\r\n" +
        ".\r\n" +
        "250\r\n" +
        "QUIT\r\n" +
        "221\r\n";
        assertEquals( expectedResult.length(), result.length() );
        assertEquals( expectedResult, result );
        if ( testMailClient.isFailed() ) {
            fail( testMailClient.getFailMessage() );
        }
    }


    /**
     *  Test a MailMessage with no subject line
     *  Subject is an optional field (RFC 822 s4.1)
     */
    public void testNoSubject() {
        ServerThread testMailServer = new ServerThread();
        Thread server = new Thread(testMailServer);
        server.start();

        ClientThread testMailClient = new ClientThread();

        testMailClient.from("Mail Message <EmailTaskTest@ant.apache.org>");
        testMailClient.to("to@you.com");
        testMailClient.setMessage( "test line 1\n" +
            "test line 2" );

        Thread client = new Thread(testMailClient);
        client.start();

        try {
            server.join(60 * 1000); // 60s
            client.join(30 * 1000); // a further 30s
        } catch (InterruptedException ie ) {
            fail( "InterruptedException: " + ie );
        }

        String result = testMailServer.getResult();
        String expectedResult = "220 test SMTP EmailTaskTest\r\n" +
        "HELO " + local + "\r\n" +
        "250 " + local + " Hello " + local + " [127.0.0.1], pleased to meet you\r\n" +
        "MAIL FROM: <EmailTaskTest@ant.apache.org>\r\n" +
        "250\r\n" +
        "RCPT TO: <to@you.com>\r\n" +
        "250\r\n" +
        "DATA\r\n" +
        "354\r\n" +
        "From: Mail Message <EmailTaskTest@ant.apache.org>\r\n" +
            "To: to@you.com\r\n" +
        "X-Mailer: org.apache.tools.mail.MailMessage (ant.apache.org)\r\n" +
        "\r\n" +
        "test line 1\r\n" +
        "test line 2\r\n" +
        "\r\n" +
        ".\r\n" +
        "250\r\n" +
        "QUIT\r\n" +
        "221\r\n";
        assertEquals( expectedResult.length(), result.length() );
        assertEquals( expectedResult, result );
        if ( testMailClient.isFailed() ) {
            fail( testMailClient.getFailMessage() );
        }
    }


    /**
     *  Test a MailMessage with empty body message
     */
    public void testEmptyBody() {
        ServerThread testMailServer = new ServerThread();
        Thread server = new Thread(testMailServer);
        server.start();

        ClientThread testMailClient = new ClientThread();

        testMailClient.from("Mail Message <EmailTaskTest@ant.apache.org>");
        testMailClient.to("to@you.com");
        testMailClient.setSubject("Test subject");
        testMailClient.setMessage("");

        Thread client = new Thread(testMailClient);
        client.start();

        try {
            server.join(60 * 1000); // 60s
            client.join(30 * 1000); // a further 30s
        } catch (InterruptedException ie ) {
            fail( "InterruptedException: " + ie );
        }

        String result = testMailServer.getResult();
        String expectedResult = "220 test SMTP EmailTaskTest\r\n" +
        "HELO " + local + "\r\n" +
        "250 " + local + " Hello " + local + " [127.0.0.1], pleased to meet you\r\n" +
        "MAIL FROM: <EmailTaskTest@ant.apache.org>\r\n" +
        "250\r\n" +
        "RCPT TO: <to@you.com>\r\n" +
        "250\r\n" +
        "DATA\r\n" +
        "354\r\n" +
        "Subject: Test subject\r\n" +
            "From: Mail Message <EmailTaskTest@ant.apache.org>\r\n" +
            "To: to@you.com\r\n" +
        "X-Mailer: org.apache.tools.mail.MailMessage (ant.apache.org)\r\n" +
        "\r\n" +
        "\r\n" +
        "\r\n" +
        ".\r\n" +
        "250\r\n" +
        "QUIT\r\n" +
        "221\r\n";
        assertEquals(expectedResult.length(), result.length());
        assertEquals(expectedResult, result);
        if (testMailClient.isFailed()) {
            fail(testMailClient.getFailMessage());
        }
    }


    /**
     *  Test a MailMessage with US-ASCII character set
     *  The next four testcase can be kinda hard to debug as Ant will often
     *  print the junit failure in US-ASCII.
     */
    public void testAsciiCharset() {

        ServerThread testMailServer = new ServerThread();
        Thread server = new Thread(testMailServer);
        server.start();

        ClientThread testMailClient = new ClientThread();

        testMailClient.from("Mail Message <EmailTaskTest@ant.apache.org>");
        testMailClient.to("Ceki G\u00fclc\u00fc <abuse@mail-abuse.org>");
        testMailClient.setSubject("Test subject");
        testMailClient.setMessage("");

        Thread client = new Thread(testMailClient);
        client.start();

        try {
            server.join(60 * 1000); // 60s
            client.join(30 * 1000); // a further 30s
        } catch (InterruptedException ie ) {
            fail("InterruptedException: " + ie);
        }

        String result = testMailServer.getResult();
        String expectedResult = "220 test SMTP EmailTaskTest\r\n" +
        "HELO " + local + "\r\n" +
        "250 " + local + " Hello " + local + " [127.0.0.1], pleased to meet you\r\n" +
        "MAIL FROM: <EmailTaskTest@ant.apache.org>\r\n" +
        "250\r\n" +
        "RCPT TO: <abuse@mail-abuse.org>\r\n" +
        "250\r\n" +
        "DATA\r\n" +
        "354\r\n" +
        "Subject: Test subject\r\n" +
            "From: Mail Message <EmailTaskTest@ant.apache.org>\r\n" +
            "To: Ceki G\u00fclc\u00fc <abuse@mail-abuse.org>\r\n" +
        "X-Mailer: org.apache.tools.mail.MailMessage (ant.apache.org)\r\n" +
        "\r\n" +
        "\r\n" +
        "\r\n" +
        ".\r\n" +
        "250\r\n" +
        "QUIT\r\n" +
        "221\r\n";
        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        PrintStream bos1 = new PrintStream(baos1, true);
        PrintStream bos2 = new PrintStream(baos2, true);

        bos1.print(expectedResult);
        bos2.print(result);

        assertEquals( "expected message length != actual message length "
            + "in testAsciiCharset()", expectedResult.length(), result.length() );
        assertEquals( "baos1 and baos2 should be the same in testAsciiCharset()",
            baos1.toString(), baos2.toString() ); // order of headers cannot be guaranteed
        if (testMailClient.isFailed()) {
            fail(testMailClient.getFailMessage());
        }
    }




    /**
     * A private test class that pretends to be a mail transfer agent
     */
    private class ServerThread implements Runnable {

        private StringBuffer sb = null;
        private boolean loop = false;
        ServerSocket ssock = null;
        Socket sock = null;
        BufferedWriter out = null;
        BufferedReader in = null;
        private boolean data = false;  // state engine: false=envelope, true=message

        public void run() {

            try {
                ssock = new ServerSocket(TEST_PORT);
                sock = ssock.accept(); // wait for connection
                in = new BufferedReader( new InputStreamReader(
                    sock.getInputStream()) );
                out = new BufferedWriter( new OutputStreamWriter(
                    sock.getOutputStream() ) );
                sb = new StringBuffer();
                send( "220 test SMTP EmailTaskTest\r\n" );
                loop = true;
                while ( loop ) {
                    String response = in.readLine();
                    if ( response == null ) {
                        loop = false;
                        break;
                    }
                    sb.append( response + "\r\n" );

                    if ( !data && response.startsWith( "HELO" ) ) {
                        send( "250 " + local + " Hello " + local + " " +
                        "[127.0.0.1], pleased to meet you\r\n" );
                    } else if ( !data && response.startsWith("MAIL") ) {
                        send( "250\r\n" );
                    } else if ( !data && response.startsWith("RCPT")) {
                        send( "250\r\n" );
                    } else if (!data && response.startsWith("DATA")) {
                        send( "354\r\n" );
                        data = true;
                    } else if (data && response.equals(".") ) {
                        send( "250\r\n" );
                        data = false;
                    } else if (!data && response.startsWith("QUIT")) {
                        send( "221\r\n" );
                        loop = false;
                    } else if (!data) {
                        //throw new IllegalStateException("Command unrecognized: "
                        //    + response);
                        send( "500 5.5.1 Command unrecognized: \"" +
                            response + "\"\r\n" );
                        loop = false;
                    } else {
                        // sb.append( response + "\r\n" );
                    }

                } // while
            } catch (IOException ioe) {
                fail();
            } finally {
                disconnect();
            }
        }

        private void send(String retmsg) throws IOException {
            out.write( retmsg );
            out.flush();
            sb.append( retmsg );
        }

        private void disconnect() {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                    out = null;
                } catch (IOException e) {
                    // ignore
                }
            }
            if (in != null) {
                try {
                    in.close();
                    in = null;
                } catch (IOException e) {
                    // ignore
                }
            }
            if (sock != null) {
                try {
                    sock.close();
                    sock = null;
                } catch (IOException e) {
                    // ignore
                }
            }
            if (ssock != null) {
                try {
                    ssock.close();
                    ssock = null;
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        public synchronized String getResult() {
            loop = false;
            return sb.toString();
        }

    }

    /**
     *  A private test class that wraps MailMessage
     */
    private class ClientThread implements Runnable {

        private MailMessage msg;
        private boolean fail = false;
        private String failMessage = null;

        protected String from = null;
        protected String subject = null;
        protected String message = null;

        protected Vector replyToList = new Vector();
        protected Vector toList = new Vector();
        protected Vector ccList = new Vector();
        protected Vector bccList = new Vector();


        public void run() {
            for (int i = 9; i > 0; i--) {
                try {
                    msg = new MailMessage("localhost", TEST_PORT);
                } catch (java.net.ConnectException ce) {
                    try {
                        Thread.sleep(10 * 1000);
                    } catch (InterruptedException ie) {
                        // ignore
                    }
                } catch (IOException ioe) {
                    fail = true;
                    failMessage = "IOException: " + ioe;
                    return;
                }
                if (msg != null) {
                    break;
                }
            }

            if (msg == null) {
                fail = true;
                failMessage = "java.net.ConnectException: Connection refused";
                return;
            }

            try {
                msg.from(from);

                Enumeration e;

                e = replyToList.elements();
                while (e.hasMoreElements()) {
                    msg.replyto(e.nextElement().toString());
                }

                e = toList.elements();
                while (e.hasMoreElements()) {
                    msg.to(e.nextElement().toString());
                }

                e = ccList.elements();
                while (e.hasMoreElements()) {
                    msg.cc(e.nextElement().toString());
                }

                e = bccList.elements();
                while (e.hasMoreElements()) {
                    msg.bcc(e.nextElement().toString());
                }

                if (subject != null) {
                    msg.setSubject(subject);
                }

                if (message != null ) {
                    PrintStream out = msg.getPrintStream();
                    out.println( message );
                }

                msg.sendAndClose();
            } catch (IOException ioe) {
                fail = true;
                failMessage = "IOException: " + ioe;
                return;
            }
        }

        public boolean isFailed() {
            return fail;
        }

        public String getFailMessage() {
            return failMessage;
        }

        public void replyTo(String replyTo) {
            replyToList.add(replyTo);
        }

        public void to(String to) {
            toList.add(to);
        }

        public void cc(String cc) {
            ccList.add(cc);
        }

        public void bcc(String bcc) {
            bccList.add(bcc);
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public void from(String from) {
            this.from = from;
        }

        public void setMessage(String message) {
            this.message = message;
        }

    }

}
