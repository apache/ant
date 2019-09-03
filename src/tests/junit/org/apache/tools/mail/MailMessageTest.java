/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.mail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.apache.tools.ant.DummyMailServer;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.Vector;

/**
 * JUnit testcases for org.apache.tools.mail.MailMessage.
 *
 * @since Ant 1.6
 */
public class MailMessageTest {

    private String local = null;

    @Before
    public void setUp() {
        try {
            local = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (java.net.UnknownHostException uhe) {
            // ignore
        }
    }

    /**
     *  Test an example that is similar to the one given in the API
     *  If this testcase takes >90s to complete, it is very likely that
     *  the two threads are blocked waiting for each other and Thread.join()
     *  timed out.
     * @throws InterruptedException if something goes wrong
     */
    @Test
    public void testAPIExample() throws InterruptedException {
        final DummyMailServer testMailServer = DummyMailServer.startMailServer(this.local);
        final ClientThread testMailClient;
        try {
            testMailClient = new ClientThread(testMailServer.getPort());

            testMailClient.from("Mail Message <EmailTaskTest@ant.apache.org>");
            testMailClient.to("to@you.com");
            testMailClient.cc("cc1@you.com");
            testMailClient.cc("cc2@you.com");
            testMailClient.bcc("bcc@you.com");
            testMailClient.setSubject("Test subject");
            testMailClient.setMessage("test line 1\n"
                    + "test line 2");

            Thread client = new Thread(testMailClient);
            client.start();
            client.join(30 * 1000); // a further 30s

        } finally {
            testMailServer.disconnect();
        }
        String result = testMailServer.getResult();
        String expectedResult = "220 test SMTP EmailTaskTest\r\n"
                + "HELO " + local + "\r\n"
                + "250 " + local + " Hello " + local + " [127.0.0.1], pleased to meet you\r\n"
                + "MAIL FROM: <EmailTaskTest@ant.apache.org>\r\n"
                + "250\r\n"
                + "RCPT TO: <to@you.com>\r\n"
                + "250\r\n"
                + "RCPT TO: <cc1@you.com>\r\n"
                + "250\r\n"
                + "RCPT TO: <cc2@you.com>\r\n"
                + "250\r\n"
                + "RCPT TO: <bcc@you.com>\r\n"
                + "250\r\n"
                + "DATA\r\n"
                + "354\r\n"
                + "Subject: Test subject\r\n" + "From: Mail Message <EmailTaskTest@ant.apache.org>\r\n"
                + "To: to@you.com\r\n"
                + "Cc: cc1@you.com, cc2@you.com\r\n"
                + "X-Mailer: org.apache.tools.mail.MailMessage (ant.apache.org)\r\n"
                + "\r\n"
                + "test line 1\r\n"
                + "test line 2\r\n"
                + "\r\n"
                + ".\r\n"
                + "250\r\n"
                + "QUIT\r\n"
                + "221\r\n";
        assertEquals(expectedResult, result); // order of headers cannot be guaranteed
        assertFalse(testMailClient.getFailMessage(), testMailClient.isFailed());
    }

    /**
     *  Test a MailMessage with no cc or bcc lines
     * @throws InterruptedException if something goes wrong
     */
    @Test
    public void testToOnly() throws InterruptedException {
        final DummyMailServer testMailServer = DummyMailServer.startMailServer(this.local);
        final ClientThread testMailClient;
        try {
            testMailClient = new ClientThread(testMailServer.getPort());

            testMailClient.from("Mail Message <EmailTaskTest@ant.apache.org>");
            testMailClient.to("to@you.com");
            testMailClient.setSubject("Test subject");
            testMailClient.setMessage("test line 1\n" + "test line 2");

            Thread client = new Thread(testMailClient);
            client.start();

            client.join(30 * 1000); // a further 30s
        } finally {
            testMailServer.disconnect();
        }

        String result = testMailServer.getResult();
        String expectedResult = "220 test SMTP EmailTaskTest\r\n"
                + "HELO " + local + "\r\n"
                + "250 " + local + " Hello " + local + " [127.0.0.1], pleased to meet you\r\n"
                + "MAIL FROM: <EmailTaskTest@ant.apache.org>\r\n"
                + "250\r\n"
                + "RCPT TO: <to@you.com>\r\n"
                + "250\r\n"
                + "DATA\r\n"
                + "354\r\n"
                + "Subject: Test subject\r\n"
                + "From: Mail Message <EmailTaskTest@ant.apache.org>\r\n"
                + "To: to@you.com\r\n"
                + "X-Mailer: org.apache.tools.mail.MailMessage (ant.apache.org)\r\n"
                + "\r\n"
                + "test line 1\r\n"
                + "test line 2\r\n"
                + "\r\n"
                + ".\r\n"
                + "250\r\n"
                + "QUIT\r\n"
                + "221\r\n";
        assertEquals(expectedResult, result); // order of headers cannot be guaranteed
        assertFalse(testMailClient.getFailMessage(), testMailClient.isFailed());
    }

    /**
     *  Test a MailMessage with no to or bcc lines
     * @throws InterruptedException if something goes wrong
     */
    @Test
    public void testCcOnly() throws InterruptedException {
        final DummyMailServer testMailServer = DummyMailServer.startMailServer(this.local);
        final ClientThread testMailClient;
        try {
            testMailClient = new ClientThread(testMailServer.getPort());
            testMailClient.from("Mail Message <EmailTaskTest@ant.apache.org>");
            testMailClient.cc("cc@you.com");
            testMailClient.setSubject("Test subject");
            testMailClient.setMessage("test line 1\n" + "test line 2");

            Thread client = new Thread(testMailClient);
            client.start();

            client.join(30 * 1000); // a further 30s
        } finally {
            testMailServer.disconnect();
        }

        String result = testMailServer.getResult();
        String expectedResult = "220 test SMTP EmailTaskTest\r\n"
                + "HELO " + local + "\r\n"
                + "250 " + local + " Hello " + local + " [127.0.0.1], pleased to meet you\r\n"
                + "MAIL FROM: <EmailTaskTest@ant.apache.org>\r\n"
                + "250\r\n"
                + "RCPT TO: <cc@you.com>\r\n"
                + "250\r\n"
                + "DATA\r\n"
                + "354\r\n"
                + "Subject: Test subject\r\n"
                + "From: Mail Message <EmailTaskTest@ant.apache.org>\r\n"
                + "Cc: cc@you.com\r\n"
                + "X-Mailer: org.apache.tools.mail.MailMessage (ant.apache.org)\r\n"
                + "\r\n"
                + "test line 1\r\n"
                + "test line 2\r\n"
                + "\r\n"
                + ".\r\n"
                + "250\r\n"
                + "QUIT\r\n"
                + "221\r\n";
        assertEquals(expectedResult, result);
        assertFalse(testMailClient.getFailMessage(), testMailClient.isFailed());
    }

    /**
     *  Test a MailMessage with no to or cc lines
     * @throws InterruptedException if something goes wrong
     */
    @Test
    public void testBccOnly() throws InterruptedException {
        final DummyMailServer testMailServer = DummyMailServer.startMailServer(this.local);
        final ClientThread testMailClient;
        try {
            testMailClient = new ClientThread(testMailServer.getPort());
            testMailClient.from("Mail Message <EmailTaskTest@ant.apache.org>");
            testMailClient.bcc("bcc@you.com");
            testMailClient.setSubject("Test subject");
            testMailClient.setMessage("test line 1\n" + "test line 2");

            Thread client = new Thread(testMailClient);
            client.start();
            client.join(30 * 1000); // a further 30s
        } finally {
            testMailServer.disconnect();
        }

        String result = testMailServer.getResult();
        String expectedResult = "220 test SMTP EmailTaskTest\r\n"
                + "HELO " + local + "\r\n"
                + "250 " + local + " Hello " + local + " [127.0.0.1], pleased to meet you\r\n"
                + "MAIL FROM: <EmailTaskTest@ant.apache.org>\r\n"
                + "250\r\n"
                + "RCPT TO: <bcc@you.com>\r\n"
                + "250\r\n"
                + "DATA\r\n"
                + "354\r\n"
                + "Subject: Test subject\r\n"
                + "From: Mail Message <EmailTaskTest@ant.apache.org>\r\n"
                + "X-Mailer: org.apache.tools.mail.MailMessage (ant.apache.org)\r\n"
                + "\r\n"
                + "test line 1\r\n"
                + "test line 2\r\n"
                + "\r\n"
                + ".\r\n"
                + "250\r\n"
                + "QUIT\r\n"
                + "221\r\n";
        assertEquals(expectedResult, result);
        assertFalse(testMailClient.getFailMessage(), testMailClient.isFailed());
    }

    /**
     *  Test a MailMessage with no subject line
     *  Subject is an optional field (RFC 822 s4.1)
     * @throws InterruptedException if something goes wrong
     */
    @Test
    public void testNoSubject() throws InterruptedException {
        final DummyMailServer testMailServer = DummyMailServer.startMailServer(this.local);
        final ClientThread testMailClient;

        try {
            testMailClient = new ClientThread(testMailServer.getPort());
            testMailClient.from("Mail Message <EmailTaskTest@ant.apache.org>");
            testMailClient.to("to@you.com");
            testMailClient.setMessage("test line 1\n" + "test line 2");

            Thread client = new Thread(testMailClient);
            client.start();

            client.join(30 * 1000); // a further 30s
        } finally {
            testMailServer.disconnect();
        }

        String result = testMailServer.getResult();
        String expectedResult = "220 test SMTP EmailTaskTest\r\n"
                + "HELO " + local + "\r\n"
                + "250 " + local + " Hello " + local + " [127.0.0.1], pleased to meet you\r\n"
                + "MAIL FROM: <EmailTaskTest@ant.apache.org>\r\n"
                + "250\r\n"
                + "RCPT TO: <to@you.com>\r\n"
                + "250\r\n"
                + "DATA\r\n"
                + "354\r\n"
                + "From: Mail Message <EmailTaskTest@ant.apache.org>\r\n"
                + "To: to@you.com\r\n"
                + "X-Mailer: org.apache.tools.mail.MailMessage (ant.apache.org)\r\n"
                + "\r\n"
                + "test line 1\r\n"
                + "test line 2\r\n"
                + "\r\n"
                + ".\r\n"
                + "250\r\n"
                + "QUIT\r\n"
                + "221\r\n";
        assertEquals(expectedResult, result);
        assertFalse(testMailClient.getFailMessage(), testMailClient.isFailed());
    }

    /**
     *  Test a MailMessage with empty body message
     * @throws InterruptedException if something goes wrong
     */
    @Test
    public void testEmptyBody() throws InterruptedException {
        final DummyMailServer testMailServer = DummyMailServer.startMailServer(this.local);
        final ClientThread testMailClient;
        try {
            testMailClient = new ClientThread(testMailServer.getPort());
            testMailClient.from("Mail Message <EmailTaskTest@ant.apache.org>");
            testMailClient.to("to@you.com");
            testMailClient.setSubject("Test subject");
            testMailClient.setMessage("");

            Thread client = new Thread(testMailClient);
            client.start();

            client.join(30 * 1000); // a further 30s
        } finally {
            testMailServer.disconnect();
        }

        String result = testMailServer.getResult();
        String expectedResult = "220 test SMTP EmailTaskTest\r\n"
                + "HELO " + local + "\r\n"
                + "250 " + local + " Hello " + local + " [127.0.0.1], pleased to meet you\r\n"
                + "MAIL FROM: <EmailTaskTest@ant.apache.org>\r\n"
                + "250\r\n"
                + "RCPT TO: <to@you.com>\r\n"
                + "250\r\n"
                + "DATA\r\n"
                + "354\r\n"
                + "Subject: Test subject\r\n"
                + "From: Mail Message <EmailTaskTest@ant.apache.org>\r\n"
                + "To: to@you.com\r\n"
                + "X-Mailer: org.apache.tools.mail.MailMessage (ant.apache.org)\r\n"
                + "\r\n"
                + "\r\n"
                + "\r\n"
                + ".\r\n"
                + "250\r\n"
                + "QUIT\r\n"
                + "221\r\n";
        assertEquals(expectedResult, result);
        assertFalse(testMailClient.getFailMessage(), testMailClient.isFailed());
    }

    /**
     *  Test a MailMessage with US-ASCII character set
     *  The next four testcase can be kinda hard to debug as Ant will often
     *  print the junit failure in US-ASCII.
     * @throws InterruptedException if something goes wrong
     */
    @Test
    public void testAsciiCharset() throws InterruptedException {
        final DummyMailServer testMailServer = DummyMailServer.startMailServer(this.local);
        final ClientThread testMailClient;

        try {
            testMailClient = new ClientThread(testMailServer.getPort());
            testMailClient.from("Mail Message <EmailTaskTest@ant.apache.org>");
            testMailClient.to("Ceki G\u00fclc\u00fc <abuse@mail-abuse.org>");
            testMailClient.setSubject("Test subject");
            testMailClient.setMessage("");

            Thread client = new Thread(testMailClient);
            client.start();

            client.join(30 * 1000); // a further 30s
        } finally {
            testMailServer.disconnect();
        }

        String result = testMailServer.getResult();
        String expectedResult = "220 test SMTP EmailTaskTest\r\n"
                + "HELO " + local + "\r\n"
                + "250 " + local + " Hello " + local + " [127.0.0.1], pleased to meet you\r\n"
                + "MAIL FROM: <EmailTaskTest@ant.apache.org>\r\n"
                + "250\r\n"
                + "RCPT TO: <abuse@mail-abuse.org>\r\n"
                + "250\r\n"
                + "DATA\r\n"
                + "354\r\n"
                + "Subject: Test subject\r\n"
                + "From: Mail Message <EmailTaskTest@ant.apache.org>\r\n"
                + "To: Ceki G\u00fclc\u00fc <abuse@mail-abuse.org>\r\n"
                + "X-Mailer: org.apache.tools.mail.MailMessage (ant.apache.org)\r\n"
                + "\r\n"
                + "\r\n"
                + "\r\n"
                + ".\r\n"
                + "250\r\n"
                + "QUIT\r\n"
                + "221\r\n";
        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        PrintStream bos1 = new PrintStream(baos1, true);
        PrintStream bos2 = new PrintStream(baos2, true);

        bos1.print(expectedResult);
        bos2.print(result);

        assertEquals("baos1 and baos2 should be the same in testAsciiCharset()",
            baos1.toString(), baos2.toString()); // order of headers cannot be guaranteed
        assertFalse(testMailClient.getFailMessage(), testMailClient.isFailed());
    }

    /**
     *  A private test class that wraps MailMessage
     */
    private class ClientThread implements Runnable {

        private final int port;
        private MailMessage msg;
        private boolean fail = false;
        private String failMessage = null;

        protected String from = null;
        protected String subject = null;
        protected String message = null;

        protected Vector<String> replyToList = new Vector<>();
        protected Vector<String> toList = new Vector<>();
        protected Vector<String> ccList = new Vector<>();
        protected Vector<String> bccList = new Vector<>();

        ClientThread(int port) {
            this.port = port;
        }

        public void run() {
            for (int i = 9; i > 0; i--) {
                try {
                    msg = new MailMessage("localhost", port);
                } catch (java.net.ConnectException ce) {
                    try {
                        Thread.sleep(10 * 1000);
                    } catch (InterruptedException ie) {
                        throw new AssumptionViolatedException("Thread interrupted", ie);
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

                replyToList.forEach(e -> msg.replyto(e));

                for (String e : toList) {
                    msg.to(e);
                }

                for (String e : ccList) {
                    msg.cc(e);
                }

                for (String e : bccList) {
                    msg.bcc(e);
                }

                if (subject != null) {
                    msg.setSubject(subject);
                }

                if (message != null) {
                    PrintStream out = msg.getPrintStream();
                    out.println(message);
                }

                msg.sendAndClose();
            } catch (IOException ioe) {
                fail = true;
                failMessage = "IOException: " + ioe;
            }
        }

        public boolean isFailed() {
            return fail;
        }

        public String getFailMessage() {
            return failMessage;
        }

        @SuppressWarnings("unused")
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
