/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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

package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.*;
import java.text.NumberFormat;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.apache.tools.ant.BuildException;

import junit.framework.Test;
import junit.framework.TestCase;

/**
 * Prints XML output of the test to a specified Writer.
 *
 * @author <a href="mailto:stefan.bodewig@megabit.net">Stefan Bodewig</a>
 */

public class XMLJUnitResultFormatter implements JUnitResultFormatter {

    private static DocumentBuilder getDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        catch(Exception exc) {
            throw new ExceptionInInitializerError(exc);
        }
    }

    /**
     * Formatter for timings.
     */
    private NumberFormat nf = NumberFormat.getInstance();
    /**
     * The XML document.
     */
    private Document doc;
    /**
     * The wrapper for the whole testsuite.
     */
    private Element rootElement;
    /**
     * Element for the current test.
     */
    private Element currentTest;
    /**
     * Timing helper.
     */
    private long lastTestStart = 0;
    /**
     * Where to write the log to.
     */
    private OutputStream out;

    public XMLJUnitResultFormatter() {}

    public void setOutput(OutputStream out) {
        this.out = out;
    }

    /**
     * The whole testsuite started.
     */
    public void startTestSuite(JUnitTest suite) {
        doc = getDocumentBuilder().newDocument();
        rootElement = doc.createElement("testsuite");
        rootElement.setAttribute("name", xmlEscape(suite.getName()));
    }

    /**
     * The whole testsuite ended.
     */
    public void endTestSuite(JUnitTest suite) throws BuildException {
        rootElement.setAttribute("tests", ""+suite.runCount());
        rootElement.setAttribute("failures", ""+suite.failureCount());
        rootElement.setAttribute("errors", ""+suite.errorCount());
        rootElement.setAttribute("time", 
                                 nf.format(suite.getRunTime()/1000.0)+" sec");
        if (out != null) {
            Writer wri = null;
            try {
                wri = new OutputStreamWriter(out);
                wri.write("<?xml version=\"1.0\"?>\n");
                write(rootElement, wri, 0);
                wri.flush();
            } catch(IOException exc) {
                throw new BuildException("Unable to write log file", exc);
            } finally {
                if (out != System.out && out != System.err) {
                    if (wri != null) {
                        try {
                            wri.close();
                        } catch (IOException e) {}
                    }
                }
            }
        }
    }

    /**
     * Interface TestListener.
     *
     * <p>A new Test is started.
     */
    public void startTest(Test t) {
        lastTestStart = System.currentTimeMillis();
        currentTest = doc.createElement("testcase");
        currentTest.setAttribute("name", xmlEscape(((TestCase) t).name()));
        rootElement.appendChild(currentTest);
    }

    /**
     * Interface TestListener.
     *
     * <p>A Test is finished.
     */
    public void endTest(Test test) {
        currentTest.setAttribute("time", 
                                 nf.format((System.currentTimeMillis()-lastTestStart)
                                           / 1000.0));
    }

    /**
     * Interface TestListener.
     *
     * <p>A Test failed.
     */
    public void addFailure(Test test, Throwable t) {
        formatError("failure", test, t);
    }

    /**
     * Interface TestListener.
     *
     * <p>An error occured while running the test.
     */
    public void addError(Test test, Throwable t) {
        formatError("error", test, t);
    }

    private void formatError(String type, Test test, Throwable t) {
        if (test != null) {
            endTest(test);
        }

        Element nested = doc.createElement(type);
        if (test != null) {
            currentTest.appendChild(nested);
        } else {
            rootElement.appendChild(nested);
        }

        String message = t.getMessage();
        if (message != null && message.length() > 0) {
            nested.setAttribute("message", xmlEscape(t.getMessage()));
        }
        nested.setAttribute("type", xmlEscape(t.getClass().getName()));

        StringWriter swr = new StringWriter();
        t.printStackTrace(new PrintWriter(swr, true));
        Text trace = doc.createTextNode(swr.toString());
        nested.appendChild(trace);
    }


    /**
     * Translates <, & , " and > to corresponding entities.
     */
    private String xmlEscape(String orig) {
        if (orig == null) return "";
        StringBuffer temp = new StringBuffer();
        StringCharacterIterator sci = new StringCharacterIterator(orig);
        for (char c = sci.first(); c != CharacterIterator.DONE;
             c = sci.next()) {

            switch (c) {
            case '<':
                temp.append("&lt;");
                break;
            case '>':
                temp.append("&gt;");
                break;
            case '\"':
                temp.append("&quot;");
                break;
            case '&':
                temp.append("&amp;");
                break;
            default:
                temp.append(c);
                break;
            }
        }
        return temp.toString();
    }

    /**
     *  Writes a DOM element to a stream.
     */
    private static void write(Element element, Writer out, int indent) throws IOException {
        // Write indent characters
        for (int i = 0; i < indent; i++) {
            out.write("\t");
        }

        // Write element
        out.write("<");
        out.write(element.getTagName());

        // Write attributes
        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr attr = (Attr) attrs.item(i);
            out.write(" ");
            out.write(attr.getName());
            out.write("=\"");
            out.write(attr.getValue());
            out.write("\"");
        }
        out.write(">");

        // Write child attributes and text
        boolean hasChildren = false;
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child.getNodeType() == Node.ELEMENT_NODE) {
                if (!hasChildren) {
                    out.write("\n");
                    hasChildren = true;
                }
                write((Element)child, out, indent + 1);
            }

            if (child.getNodeType() == Node.TEXT_NODE) {
                out.write("<![CDATA[");
                out.write(((Text)child).getData());
                out.write("]]>");
            }
        }

        // If we had child elements, we need to indent before we close
        // the element, otherwise we're on the same line and don't need
        // to indent
        if (hasChildren) {
            for (int i = 0; i < indent; i++) {
                out.write("\t");
            }
        }

        // Write element close
        out.write("</");
        out.write(element.getTagName());
        out.write(">\n");
    }

} // XMLJUnitResultFormatter
