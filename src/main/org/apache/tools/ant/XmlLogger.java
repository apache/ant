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
package org.apache.tools.ant;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.tools.ant.util.DOMElementWriter;
import org.apache.tools.ant.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
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
    private static DocumentBuilder builder = getDocumentBuilder();

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
    private Map<Task, TimedElement> tasks = new Hashtable<>();

    /** Mapping for when targets started (Target to TimedElement). */
    private Map<Target, TimedElement> targets = new Hashtable<>();

    /**
     * Mapping of threads to stacks of elements
     * (Thread to Stack of TimedElement).
     */
    private Map<Thread, Stack<TimedElement>> threadStacks = new Hashtable<>();

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

        @Override
        public String toString() {
            return element.getTagName() + ":" + element.getAttribute("name");
        }
    }

    /**
     * Fired when the build starts, this builds the top-level element for the
     * document and remembers the time of the start of the build.
     *
     * @param event Ignored.
     */
    @Override
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
    @Override
    public void buildFinished(BuildEvent event) {
        long totalTime = System.currentTimeMillis() - buildElement.startTime;
        buildElement.element.setAttribute(TIME_ATTR, DefaultLogger.formatTime(totalTime));

        if (event.getException() != null) {
            buildElement.element.setAttribute(ERROR_ATTR, event.getException().toString());
            // print the stacktrace in the build file it is always useful...
            // better have too much info than not enough.
            Throwable t = event.getException();
            Text errText = doc.createCDATASection(StringUtils.getStackTrace(t));
            Element stacktrace = doc.createElement(STACKTRACE_TAG);
            stacktrace.appendChild(errText);
            synchronizedAppend(buildElement.element, stacktrace);
        }
        String outFilename = getProperty(event, "XmlLogger.file", "log.xml");
        String xslUri = getProperty(event, "ant.XmlLogger.stylesheet.uri", "log.xsl");

        try (OutputStream stream =
            outStream == null ? Files.newOutputStream(Paths.get(outFilename)) : outStream;
                Writer out = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            if (!xslUri.isEmpty()) {
                out.write("<?xml-stylesheet type=\"text/xsl\" href=\"" + xslUri
                    + "\"?>\n\n");
            }
            new DOMElementWriter().write(buildElement.element, out, 0, "\t");
            out.flush();
        } catch (IOException exc) {
            throw new BuildException("Unable to write log file", exc);
        }
        buildElement = null;
    }

    private String getProperty(BuildEvent event, String propertyName, String defaultValue) {
        String rv = defaultValue;
        if (event != null && event.getProject() != null && event.getProject().getProperty(propertyName) != null) {
            rv = event.getProject().getProperty(propertyName);
        }
        return rv;
    }

    /**
     * Returns the stack of timed elements for the current thread.
     * @return the stack of timed elements for the current thread
     */
    private Stack<TimedElement> getStack() {
        /* For debugging purposes uncomment:
        if (threadStacks.containsKey(Thread.currentThread())) {
            org.w3c.dom.Comment s = doc.createComment("stack=" + threadStacks(Thread.currentThread()));
            buildElement.element.appendChild(s);
        }
        */
        return threadStacks.computeIfAbsent(Thread.currentThread(), k -> new Stack<>());
    }

    /**
     * Fired when a target starts building, this pushes a timed element
     * for the target onto the stack of elements for the current thread,
     * remembering the current time and the name of the target.
     *
     * @param event An event with any relevant extra information.
     *              Will not be <code>null</code>.
     */
    @Override
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
    @Override
    public void targetFinished(BuildEvent event) {
        Target target = event.getTarget();
        TimedElement targetElement = targets.get(target);
        if (targetElement != null) {
            long totalTime = System.currentTimeMillis() - targetElement.startTime;
            targetElement.element.setAttribute(TIME_ATTR, DefaultLogger.formatTime(totalTime));

            TimedElement parentElement = null;
            Stack<TimedElement> threadStack = getStack();
            if (!threadStack.empty()) {
                TimedElement poppedStack = threadStack.pop();
                if (poppedStack != targetElement) {
                    throw new RuntimeException("Mismatch - popped element = " + poppedStack //NOSONAR
                            + " finished target element = " + targetElement);
                }
                if (!threadStack.empty()) {
                    parentElement = threadStack.peek();
                }
            }
            if (parentElement == null) {
                synchronizedAppend(buildElement.element, targetElement.element);
            } else {
                synchronizedAppend(parentElement.element,
                                   targetElement.element);
            }
        }
        targets.remove(target);
    }

    /**
     * Fired when a task starts building, this pushes a timed element
     * for the task onto the stack of elements for the current thread,
     * remembering the current time and the name of the task.
     *
     * @param event An event with any relevant extra information.
     *              Will not be <code>null</code>.
     */
    @Override
    public void taskStarted(BuildEvent event) {
        TimedElement taskElement = new TimedElement();
        taskElement.startTime = System.currentTimeMillis();
        taskElement.element = doc.createElement(TASK_TAG);

        Task task = event.getTask();
        String name = event.getTask().getTaskName();
        if (name == null) {
            name = "";
        }
        taskElement.element.setAttribute(NAME_ATTR, name);
        taskElement.element.setAttribute(LOCATION_ATTR, event.getTask().getLocation().toString());
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
    @Override
    public void taskFinished(BuildEvent event) {
        Task task = event.getTask();
        TimedElement taskElement = tasks.get(task);
        if (taskElement == null) {
            throw new RuntimeException("Unknown task " + task + " not in " + tasks); //NOSONAR
        }
        long totalTime = System.currentTimeMillis() - taskElement.startTime;
        taskElement.element.setAttribute(TIME_ATTR, DefaultLogger.formatTime(totalTime));
        Target target = task.getOwningTarget();
        TimedElement targetElement = null;
        if (target != null) {
            targetElement = targets.get(target);
        }
        if (targetElement == null) {
            synchronizedAppend(buildElement.element, taskElement.element);
        } else {
            synchronizedAppend(targetElement.element, taskElement.element);
        }
        Stack<TimedElement> threadStack = getStack();
        if (!threadStack.empty()) {
            TimedElement poppedStack = threadStack.pop();
            if (poppedStack != taskElement) {
                throw new RuntimeException("Mismatch - popped element = " + poppedStack //NOSONAR
                        + " finished task element = " + taskElement);
            }
        }
        tasks.remove(task);
    }

    /**
     * Get the TimedElement associated with a task.
     *
     * Where the task is not found directly, search for unknown elements which
     * may be hiding the real task
     */
    private TimedElement getTaskElement(Task task) {
        TimedElement element = tasks.get(task);
        if (element != null) {
            return element;
        }
        final Set<Task> knownTasks = new HashSet<>(tasks.keySet());
        for (final Task t : knownTasks) {
            if (t instanceof UnknownElement && ((UnknownElement) t).getTask() == task) {
                return tasks.get(t);
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
    @Override
    public void messageLogged(BuildEvent event) {
        int priority = event.getPriority();
        if (priority > msgOutputLevel) {
            return;
        }
        Element messageElement = doc.createElement(MESSAGE_TAG);

        String name;
        switch (priority) {
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

        Throwable ex = event.getException();
        if (Project.MSG_DEBUG <= msgOutputLevel && ex != null) {
            Text errText = doc.createCDATASection(StringUtils.getStackTrace(ex));
            Element stacktrace = doc.createElement(STACKTRACE_TAG);
            stacktrace.appendChild(errText);
            synchronizedAppend(buildElement.element, stacktrace);
        }
        Text messageText = doc.createCDATASection(event.getMessage());
        messageElement.appendChild(messageText);

        TimedElement parentElement = null;

        Task task = event.getTask();

        Target target = event.getTarget();
        if (task != null) {
            parentElement = getTaskElement(task);
        }
        if (parentElement == null && target != null) {
            parentElement = targets.get(target);
        }
        if (parentElement != null) {
            synchronizedAppend(parentElement.element, messageElement);
        } else {
            synchronizedAppend(buildElement.element, messageElement);
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
    @Override
    public void setMessageOutputLevel(int level) {
        msgOutputLevel = level;
    }

    /**
     * Set the output stream to which logging output is sent when operating
     * as a logger.
     *
     * @param output the output PrintStream.
     */
    @Override
    public void setOutputPrintStream(PrintStream output) {
        this.outStream = new PrintStream(output, true);
    }

    /**
     * Ignore emacs mode, as it has no meaning in XML format
     *
     * @param emacsMode true if logger should produce emacs compatible
     *        output
     */
    @Override
    public void setEmacsMode(boolean emacsMode) {
    }

    /**
     * Ignore error print stream. All output will be written to
     * either the XML log file or the PrintStream provided to
     * setOutputPrintStream
     *
     * @param err the stream we are going to ignore.
     */
    @Override
    public void setErrorPrintStream(PrintStream err) {
    }

    private void synchronizedAppend(Node parent, Node child) {
        synchronized (parent) {
            parent.appendChild(child);
        }
    }

}
