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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.apache.tools.ant.util.DOMElementWriter;

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
    private Hashtable tasks = new Hashtable();
    private Hashtable targets = new Hashtable();
    private Hashtable threadStacks = new Hashtable();
    private TimedElement buildElement = null;

    static private class TimedElement {
        long startTime;
        Element element;
    }

    /**
     *  Constructs a new BuildListener that logs build events to an XML file.
     */
    public XmlLogger() {
    }

    public void buildStarted(BuildEvent event) {
        buildElement = new TimedElement();
        buildElement.startTime = System.currentTimeMillis();

        doc = builder.newDocument();
        buildElement.element = doc.createElement(BUILD_TAG);
    }

    public void buildFinished(BuildEvent event) {
        long totalTime = System.currentTimeMillis() - buildElement.startTime;
        buildElement.element.setAttribute(TIME_ATTR, DefaultLogger.formatTime(totalTime));

        if (event.getException() != null) {
            buildElement.element.setAttribute(ERROR_ATTR, event.getException().toString());
        }

        try {
            String outFilename = 
                event.getProject().getProperty("XmlLogger.file");

            if (outFilename == null) {
                outFilename = "log.xml";
            }
            
            // specify output in UTF8 otherwise accented characters will blow
            // up everything
            Writer out =
                new OutputStreamWriter(new FileOutputStream(outFilename),
                                       "UTF8");
            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
            out.write("<?xml:stylesheet type=\"text/xsl\" href=\"log.xsl\"?>\n\n");
                      (new DOMElementWriter()).write(buildElement.element, out, 0, "\t");
            out.flush();
            out.close();
            
        } catch(IOException exc) {
            throw new BuildException("Unable to close log file", exc);
        }
        buildElement = null;
    }

    private Stack getStack() {    
        Stack threadStack = (Stack)threadStacks.get(Thread.currentThread());
        if (threadStack == null) {
            threadStack = new Stack();
            threadStacks.put(Thread.currentThread(), threadStack);
        }
        return threadStack;
    }
    
    public void targetStarted(BuildEvent event) {
        Target target = event.getTarget();
        TimedElement targetElement = new TimedElement();
        targetElement.startTime = System.currentTimeMillis();
        targetElement.element = doc.createElement(TARGET_TAG);
        targetElement.element.setAttribute(NAME_ATTR, target.getName());
        targets.put(target, targetElement);
        getStack().push(targetElement);
    }

    public void targetFinished(BuildEvent event) {
        Target target = event.getTarget();
        TimedElement targetElement = (TimedElement)targets.get(target);
        if (targetElement != null) {
            long totalTime = System.currentTimeMillis() - targetElement.startTime;
            targetElement.element.setAttribute(TIME_ATTR, DefaultLogger.formatTime(totalTime));

            TimedElement parentElement = null;            
            Stack threadStack = getStack();
            if (!threadStack.empty()) {
                TimedElement poppedStack = (TimedElement)threadStack.pop();
                if (poppedStack != targetElement) {
                    throw new RuntimeException("Mismatch - popped element = " + poppedStack.element +
                    " finished task element = " + targetElement.element);
                }                                           
                if (!threadStack.empty()) {
                    parentElement = (TimedElement)threadStack.peek();
                }
            }
            if (parentElement == null) {
                buildElement.element.appendChild(targetElement.element);
            }
            else {                
                parentElement.element.appendChild(targetElement.element);
            }
        }
    }

    public void taskStarted(BuildEvent event) {
        Task task = event.getTask();
        TimedElement taskElement = new TimedElement();
        taskElement.startTime = System.currentTimeMillis();
        taskElement.element = doc.createElement(TASK_TAG);
        
        String name = event.getTask().getTaskName();
        taskElement.element.setAttribute(NAME_ATTR, name);
        taskElement.element.setAttribute(LOCATION_ATTR, event.getTask().getLocation().toString());
        tasks.put(task, taskElement);
        getStack().push(taskElement);
    }

    public void taskFinished(BuildEvent event) {
        Task task = event.getTask();
        TimedElement taskElement = (TimedElement)tasks.get(task);
        if (taskElement != null) {
            long totalTime = System.currentTimeMillis() - taskElement.startTime;
            taskElement.element.setAttribute(TIME_ATTR, DefaultLogger.formatTime(totalTime));
            Target target = task.getOwningTarget();
            TimedElement targetElement = (TimedElement)targets.get(target);
            if (targetElement == null) {
                buildElement.element.appendChild(taskElement.element);
            }
            else {
                targetElement.element.appendChild(taskElement.element);
            }
            Stack threadStack = getStack();
            if (!threadStack.empty()) {
                TimedElement poppedStack = (TimedElement)threadStack.pop();
                if (poppedStack != taskElement) {
                    throw new RuntimeException("Mismatch - popped element = " + poppedStack.element +
                    " finished task element = " + taskElement.element);
                }                                           
            }
        }
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

        Text messageText = doc.createCDATASection(event.getMessage());
        messageElement.appendChild(messageText);

        TimedElement parentElement = null;
        
        Task task = event.getTask();
        Target target = event.getTarget();
        if (task != null) {
            parentElement = (TimedElement)tasks.get(task);
        }
        if (parentElement == null && target != null) {
            parentElement = (TimedElement)targets.get(target);
        }
         
        if (parentElement == null) {
            Stack threadStack = (Stack)threadStacks.get(Thread.currentThread());
            if (threadStack != null) {
                if (!threadStack.empty()) {
                    parentElement = (TimedElement)threadStack.peek();
                }
            }
        }
        
        if (parentElement != null) {
            parentElement.element.appendChild(messageElement);
        }
        else {
            buildElement.element.appendChild(messageElement);
        }
    }

}
