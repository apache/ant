/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant;

import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

/**
 *  Generates a "log.xml" file in the current directory with
 *  an XML description of what happened during a build.
 *
 *  @see Project#addBuildListener(BuildListener)
 */
public class XmlLogger implements BuildListener {

    private static final DocumentBuilder builder = getDocumentBuilder();

    private static DocumentBuilder getDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        catch(Exception exc) {
            throw new ExceptionInInitializerError(exc);
        }
    }

    // XML constants for tag names and attribute names
    private static final String BUILD_TAG = "build";
    private static final String TARGET_TAG = "target";
    private static final String TASK_TAG = "task";
    private static final String MESSAGE_TAG = "message";
    private static final String NAME_ATTR = "name";
    private static final String TIME_ATTR = "time";
    private static final String PRIORITY_ATTR = "priority";
    private static final String LOCATION_ATTR = "location";
    private static final String ERROR_ATTR = "error";

    private Document doc;
    private Element buildElement;
    private Element targetElement;
    private Element taskElement;

    private long buildStartTime;
    private long targetStartTime;
    private long taskStartTime;

    /**
     *  Constructs a new BuildListener that logs build events to an XML file.
     */
    public XmlLogger() {
    }

    public void buildStarted(BuildEvent event) {
        buildStartTime = System.currentTimeMillis();

        doc = builder.newDocument();
        buildElement = doc.createElement(BUILD_TAG);
    }

    public void buildFinished(BuildEvent event) {
        long totalTime = System.currentTimeMillis() - buildStartTime;
        buildElement.setAttribute(TIME_ATTR, formatTime(totalTime));

        if (event.getException() != null) {
            buildElement.setAttribute(ERROR_ATTR, event.getException().toString());
        }

        try {
                Writer out = new FileWriter("log.xml");
                out.write("<?xml:stylesheet type=\"text/xsl\" href=\"log.xsl\"?>\n\n");
                write(buildElement, out, 0);
                out.flush();
                out.close();

        }
        catch(IOException exc) {
            throw new BuildException("Unable to close log file", exc);
        }
    }

    public void targetStarted(BuildEvent event) {
        targetStartTime = System.currentTimeMillis();
        targetElement = doc.createElement(TARGET_TAG);
        targetElement.setAttribute(NAME_ATTR, event.getTarget().getName());
    }

    public void targetFinished(BuildEvent event) {
        long totalTime = System.currentTimeMillis() - targetStartTime;
        targetElement.setAttribute(TIME_ATTR, formatTime(totalTime));
        buildElement.appendChild(targetElement);

        targetElement = null;
    }

    public void taskStarted(BuildEvent event) {
        taskStartTime = System.currentTimeMillis();
        taskElement = doc.createElement(TASK_TAG);

        String name = event.getTask().getClass().getName();
        int pos = name.lastIndexOf(".");
        if (pos != -1) {
            name = name.substring(pos + 1);
        }
        taskElement.setAttribute(NAME_ATTR, name);

        taskElement.setAttribute(LOCATION_ATTR, event.getTask().getLocation().toString());
    }

    public void taskFinished(BuildEvent event) {
        long totalTime = System.currentTimeMillis() - taskStartTime;
        taskElement.setAttribute(TIME_ATTR, formatTime(totalTime));
        targetElement.appendChild(taskElement);

        taskElement = null;
    }

    public void messageLogged(BuildEvent event) {
        Element messageElement = doc.createElement(MESSAGE_TAG);

        String name = "debug";
        switch(event.getPriority()) {
            case Project.MSG_ERR: name = "error"; break;
            case Project.MSG_WARN: name = "warn"; break;
            case Project.MSG_INFO: name = "info"; break;
            default: name = "debug"; break;
        }
        messageElement.setAttribute(PRIORITY_ATTR, name);

        Text messageText = doc.createTextNode(event.getMessage());
        messageElement.appendChild(messageText);

        if (taskElement != null) {
            taskElement.appendChild(messageElement);
        }
        else if (targetElement != null) {
            targetElement.appendChild(messageElement);
        }
        else {
            buildElement.appendChild(messageElement);
        }
    }

    /**
     *  Writes a DOM element to a file.
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
                out.write(((Text)child).getData());
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

    private static String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;


        if (minutes > 0) {
            return Long.toString(minutes) + " minutes " + Long.toString(seconds%60) + " seconds";
        }
        else {
            return Long.toString(seconds) + " seconds";
        }

    }
}