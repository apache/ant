/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Enumeration;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.tools.ant.util.DOMElementWriter;
import org.apache.tools.ant.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * Generates a file in the current directory with
 * an XML description of what happened during a build.
 * The default filename is "log.xml", but this can be overridden
 * with the property <code>XmlLogger.file</code>.
 *
 * This implementation assumes in its sanity checking that only one
 * thread runs a particular target/task at a time. This is enforced
 * by the way that parallel builds and antcalls are done - and
 * indeed all but the simplest of tasks could run into problems
 * if executed in parallel.
 *
 * @see Project#addBuildListener(BuildListener)
 */
public class XmlLogger implements BuildLogger {

    private int msgOutputLevel = Project.MSG_DEBUG;
    private PrintStream outStream;

    /** DocumentBuilder to use when creating the document to start with. */
    private static final DocumentBuilder builder = getDocumentBuilder();

    /**
     * Returns a default DocumentBuilder instance or throws an
     * ExceptionInInitializerError if it can't be created.
     *
     * @return a default DocumentBuilder instance.
     */
    private static DocumentBuilder getDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (Exception exc) {
            throw new ExceptionInInitializerError(exc);
        }
    }

    /** XML element name for a build. */
    private static final String BUILD_TAG = "build";
    /** XML element name for a target. */
    private static final String TARGET_TAG = "target";
    /** XML element name for a task. */
    private static final String TASK_TAG = "task";
    /** XML element name for a message. */
    private static final String MESSAGE_TAG = "message";
    /** XML attribute name for a name. */
    private static final String NAME_ATTR = "name";
    /** XML attribute name for a time. */
    private static final String TIME_ATTR = "time";
    /** XML attribute name for a message priority. */
    private static final String PRIORITY_ATTR = "priority";
    /** XML attribute name for a file location. */
    private static final String LOCATION_ATTR = "location";
    /** XML attribute name for an error description. */
    private static final String ERROR_ATTR = "error";
    /** XML element name for a stack trace. */
    private static final String STACKTRACE_TAG = "stacktrace";

    /** The complete log document for this build. */
    private Document doc = builder.newDocument();
    /** Mapping for when tasks started (Task to TimedElement). */
    private Hashtable tasks = new Hashtable();
    /** Mapping for when targets started (Task to TimedElement). */
    private Hashtable targets = new Hashtable();
    /**
     * Mapping of threads to stacks of elements
     * (Thread to Stack of TimedElement).
     */
    private Hashtable threadStacks = new Hashtable();
    /**
     * When the build started.
     */
    private TimedElement buildElement = null;

    /** Utility class representing the time an element started. */
    private static class TimedElement {
        /**
         * Start time in milliseconds
         * (as returned by <code>System.currentTimeMillis()</code>).
         */
        private long startTime;
        /** Element created at the start time. */
        private Element element;
    }

    /**
     *  Constructs a new BuildListener that logs build events to an XML file.
     */
    public XmlLogger() {
    }

    /**
     * Fired when the build starts, this builds the top-level element for the
     * document and remembers the time of the start of the build.
     *
     * @param event Ignored.
     */
    public void buildStarted(BuildEvent event) {
        buildElement = new TimedElement();
        buildElement.startTime = System.currentTimeMillis();
        buildElement.element = doc.createElement(BUILD_TAG);
    }

    /**
     * Fired when the build finishes, this adds the time taken and any
     * error stacktrace to the build element and writes the document to disk.
     *
     * @param event An event with any relevant extra information.
     *              Will not be <code>null</code>.
     */
    public void buildFinished(BuildEvent event) {
        long totalTime = System.currentTimeMillis() - buildElement.startTime;
        buildElement.element.setAttribute(TIME_ATTR,
                DefaultLogger.formatTime(totalTime));

        if (event.getException() != null) {
            buildElement.element.setAttribute(ERROR_ATTR,
                    event.getException().toString());
            // print the stacktrace in the build file it is always useful...
            // better have too much info than not enough.
            Throwable t = event.getException();
            Text errText = doc.createCDATASection(StringUtils.getStackTrace(t));
            Element stacktrace = doc.createElement(STACKTRACE_TAG);
            stacktrace.appendChild(errText);
            buildElement.element.appendChild(stacktrace);
        }

        String outFilename = event.getProject().getProperty("XmlLogger.file");
        if (outFilename == null) {
            outFilename = "log.xml";
        }
        String xslUri
                = event.getProject().getProperty("ant.XmlLogger.stylesheet.uri");
        if (xslUri == null) {
            xslUri = "log.xsl";
        }
        Writer out = null;
        try {
            // specify output in UTF8 otherwise accented characters will blow
            // up everything
            OutputStream stream = outStream;
            if (stream == null) {
                stream = new FileOutputStream(outFilename);
            }
            out = new OutputStreamWriter(stream, "UTF8");
            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
            if (xslUri.length() > 0) {
                out.write("<?xml-stylesheet type=\"text/xsl\" href=\""
                        + xslUri + "\"?>\n\n");
            }
            (new DOMElementWriter()).write(buildElement.element, out, 0, "\t");
            out.flush();
        } catch (IOException exc) {
            throw new BuildException("Unable to write log file", exc);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
        buildElement = null;
    }

    /**
     * Returns the stack of timed elements for the current thread.
     * @return the stack of timed elements for the current thread
     */
    private Stack getStack() {
        Stack threadStack = (Stack) threadStacks.get(Thread.currentThread());
        if (threadStack == null) {
            threadStack = new Stack();
            threadStacks.put(Thread.currentThread(), threadStack);
        }
        return threadStack;
    }

    /**
     * Fired when a target starts building, this pushes a timed element
     * for the target onto the stack of elements for the current thread,
     * rememebering the current time and the name of the target.
     *
     * @param event An event with any relevant extra information.
     *              Will not be <code>null</code>.
     */
    public void targetStarted(BuildEvent event) {
        Target target = event.getTarget();
        TimedElement targetElement = new TimedElement();
        targetElement.startTime = System.currentTimeMillis();
        targetElement.element = doc.createElement(TARGET_TAG);
        targetElement.element.setAttribute(NAME_ATTR, target.getName());
        targets.put(target, targetElement);
        getStack().push(targetElement);
    }

    /**
     * Fired when a target finishes building, this adds the time taken
     * and any error stacktrace to the appropriate target element in the log.
     *
     * @param event An event with any relevant extra information.
     *              Will not be <code>null</code>.
     */
    public void targetFinished(BuildEvent event) {
        Target target = event.getTarget();
        TimedElement targetElement = (TimedElement) targets.get(target);
        if (targetElement != null) {
            long totalTime
                    = System.currentTimeMillis() - targetElement.startTime;
            targetElement.element.setAttribute(TIME_ATTR,
                    DefaultLogger.formatTime(totalTime));

            TimedElement parentElement = null;
            Stack threadStack = getStack();
            if (!threadStack.empty()) {
                TimedElement poppedStack = (TimedElement) threadStack.pop();
                if (poppedStack != targetElement) {
                    throw new RuntimeException("Mismatch - popped element = "
                            + poppedStack.element + " finished target element = "
                            + targetElement.element);
                }
                if (!threadStack.empty()) {
                    parentElement = (TimedElement) threadStack.peek();
                }
            }
            if (parentElement == null) {
                buildElement.element.appendChild(targetElement.element);
            } else {
                parentElement.element.appendChild(targetElement.element);
            }
        }
    }

    /**
     * Fired when a task starts building, this pushes a timed element
     * for the task onto the stack of elements for the current thread,
     * rememebering the current time and the name of the task.
     *
     * @param event An event with any relevant extra information.
     *              Will not be <code>null</code>.
     */
    public void taskStarted(BuildEvent event) {
        TimedElement taskElement = new TimedElement();
        taskElement.startTime = System.currentTimeMillis();
        taskElement.element = doc.createElement(TASK_TAG);

        Task task = event.getTask();
        String name = event.getTask().getTaskName();
        taskElement.element.setAttribute(NAME_ATTR, name);
        taskElement.element.setAttribute(LOCATION_ATTR,
                event.getTask().getLocation().toString());
        tasks.put(task, taskElement);
        getStack().push(taskElement);
    }

    /**
     * Fired when a task finishes building, this adds the time taken
     * and any error stacktrace to the appropriate task element in the log.
     *
     * @param event An event with any relevant extra information.
     *              Will not be <code>null</code>.
     */
    public void taskFinished(BuildEvent event) {
        Task task = event.getTask();
        TimedElement taskElement = (TimedElement) tasks.get(task);
        if (taskElement != null) {
            long totalTime = System.currentTimeMillis() - taskElement.startTime;
            taskElement.element.setAttribute(TIME_ATTR,
                    DefaultLogger.formatTime(totalTime));
            Target target = task.getOwningTarget();
            TimedElement targetElement = null;
            if (target != null) {
                targetElement = (TimedElement) targets.get(target);
            }
            if (targetElement == null) {
                buildElement.element.appendChild(taskElement.element);
            } else {
                targetElement.element.appendChild(taskElement.element);
            }
            Stack threadStack = getStack();
            if (!threadStack.empty()) {
                TimedElement poppedStack = (TimedElement) threadStack.pop();
                if (poppedStack != taskElement) {
                    throw new RuntimeException("Mismatch - popped element = "
                            + poppedStack.element + " finished task element = "
                            + taskElement.element);
                }
            }
        }
    }


    /**
     * Get the TimedElement associated with a task.
     *
     * Where the task is not found directly, search for unknown elements which
     * may be hiding the real task
     */
    private TimedElement getTaskElement(Task task) {
        TimedElement element = (TimedElement) tasks.get(task);
        if (element != null) {
            return element;
        }

        for (Enumeration e = tasks.keys(); e.hasMoreElements();) {
            Task key = (Task) e.nextElement();
            if (key instanceof UnknownElement) {
                if (((UnknownElement) key).getTask() == task) {
                    return (TimedElement) tasks.get(key);
                }
            }
        }

        return null;
    }

    /**
     * Fired when a message is logged, this adds a message element to the
     * most appropriate parent element (task, target or build) and records
     * the priority and text of the message.
     *
     * @param event An event with any relevant extra information.
     *              Will not be <code>null</code>.
     */
    public void messageLogged(BuildEvent event) {
        int priority = event.getPriority();
        if (priority > msgOutputLevel) {
            return;
        }
        Element messageElement = doc.createElement(MESSAGE_TAG);

        String name = "debug";
        switch (event.getPriority()) {
            case Project.MSG_ERR:
                name = "error";
                break;
            case Project.MSG_WARN:
                name = "warn";
                break;
            case Project.MSG_INFO:
                name = "info";
                break;
            default:
                name = "debug";
                break;
        }
        messageElement.setAttribute(PRIORITY_ATTR, name);

        Text messageText = doc.createCDATASection(event.getMessage());
        messageElement.appendChild(messageText);

        TimedElement parentElement = null;

        Task task = event.getTask();

        Target target = event.getTarget();
        if (task != null) {
            parentElement = getTaskElement(task);
        }
        if (parentElement == null && target != null) {
            parentElement = (TimedElement) targets.get(target);
        }

        /*
        if (parentElement == null) {
            Stack threadStack
                    = (Stack) threadStacks.get(Thread.currentThread());
            if (threadStack != null) {
                if (!threadStack.empty()) {
                    parentElement = (TimedElement) threadStack.peek();
                }
            }
        }
        */

        if (parentElement != null) {
            parentElement.element.appendChild(messageElement);
        } else {
            buildElement.element.appendChild(messageElement);
        }
    }

    // -------------------------------------------------- BuildLogger interface

    /**
     * Set the logging level when using this as a Logger
     *
     * @param level the logging level -
     *        see {@link org.apache.tools.ant.Project#MSG_ERR Project}
     *        class for level definitions
     */
    public void setMessageOutputLevel(int level) {
        msgOutputLevel = level;
    }

    /**
     * Set the output stream to which logging output is sent when operating
     * as a logger.
     *
     * @param output the output PrintStream.
     */
    public void setOutputPrintStream(PrintStream output) {
        this.outStream = new PrintStream(output, true);
    }

    /**
     * Ignore emacs mode, as it has no meaning in XML format
     */
    public void setEmacsMode(boolean emacsMode) {
    }

    /**
     * Ignore error print stream. All output will be written to
     * either the XML log file or the PrintStream provided to
     * setOutputPrintStream
     */
    public void setErrorPrintStream(PrintStream err) {
    }

}
