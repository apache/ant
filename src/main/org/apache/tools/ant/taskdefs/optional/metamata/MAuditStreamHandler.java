/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.metamata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.taskdefs.StreamPumper;
import org.apache.tools.ant.util.DOMElementWriter;
import org.apache.tools.ant.util.DateUtils;

/**
 * This is a very bad stream handler for the MAudit task.
 * All report to stdout that does not match a specific report pattern is dumped
 * to the Ant output as warn level. The report that match the pattern is stored
 * in a map with the key being the filepath that caused the error report.
 * <p>
 * The limitation with the choosen implementation is clear:
 * <ul>
 * <li>it does not handle multiline report( message that has \n ). the part until
 * the \n will be stored and the other part (which will not match the pattern)
 * will go to Ant output in Warn level.
 * <li>it does not report error that goes to stderr.
 * </ul>
 *
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
class MAuditStreamHandler implements ExecuteStreamHandler {

    /** parent task */
    private MAudit task;

    /** reader for stdout */
    private BufferedReader br;

    /**
     * this is where the XML output will go, should mostly be a file
     * the caller is responsible for flushing and closing this stream
     */
    private OutputStream xmlOut = null;

    /** error stream, might be useful to spit out error messages */
    private OutputStream errStream;

    /** thread pumping out error stream */
    private Thread errThread;

    /**
     * the multimap. The key in the map is the filepath that caused the audit
     * error and the value is a vector of MAudit.Violation entries.
     */
    private Hashtable auditedFiles = new Hashtable();

    /** program start timestamp for reporting purpose */
    private Date program_start;

    MAuditStreamHandler(MAudit task, OutputStream xmlOut) {
        this.task = task;
        this.xmlOut = xmlOut;
    }

    /** Ignore. */
    public void setProcessInputStream(OutputStream os) {
    }

    /** Ignore. */
    public void setProcessErrorStream(InputStream is) {
        errStream = new LogOutputStream(task, Project.MSG_ERR);
        errThread = createPump(is, errStream);
    }

    /** Set the inputstream */
    public void setProcessOutputStream(InputStream is) throws IOException {
        br = new BufferedReader(new InputStreamReader(is));
    }

    /** Invokes parseOutput. This will block until the end :-(*/
    public void start() throws IOException {
        program_start = new Date();
        errThread.start();
        parseOutput(br);
    }

    /**
     * Pretty dangerous business here. It serializes what was extracted from
     * the MAudit output and write it to the output.
     */
    public void stop() {
        // make sure to flush err stream
        try {
            errThread.join();
        } catch (InterruptedException e) {
        }
        try {
            errStream.flush();
        } catch (IOException e) {
        }
        // serialize the content as XML, move this to another method
        // this is the only code that could be needed to be overriden
        Document doc = getDocumentBuilder().newDocument();
        Element rootElement = doc.createElement("classes");
        Enumeration keys = auditedFiles.keys();
        Hashtable filemapping = task.getFileMapping();
        final Date now = new Date();
        rootElement.setAttribute("snapshot_created", DateUtils.format(now, DateUtils.ISO8601_DATETIME_PATTERN));
        rootElement.setAttribute("elapsed_time", String.valueOf(now.getTime() - program_start.getTime()));
        rootElement.setAttribute("program_start", DateUtils.format(now, DateUtils.ISO8601_DATETIME_PATTERN));
        rootElement.setAttribute("audited", String.valueOf(filemapping.size()));
        rootElement.setAttribute("reported", String.valueOf(auditedFiles.size()));
        int errors = 0;
        while (keys.hasMoreElements()) {
            String filepath = (String) keys.nextElement();
            Vector v = (Vector) auditedFiles.get(filepath);
            String fullclassname = (String) filemapping.get(filepath);
            if (fullclassname == null) {
                task.getProject().log("Could not find class mapping for " + filepath, Project.MSG_WARN);
                continue;
            }
            int pos = fullclassname.lastIndexOf('.');
            String pkg = (pos == -1) ? "" : fullclassname.substring(0, pos);
            String clazzname = (pos == -1) ? fullclassname : fullclassname.substring(pos + 1);
            Element clazz = doc.createElement("class");
            clazz.setAttribute("package", pkg);
            clazz.setAttribute("name", clazzname);
            final int violationCount = v.size();
            clazz.setAttribute("violations", String.valueOf(violationCount));
            errors += violationCount;
            for (int i = 0; i < violationCount; i++) {
                MAuditParser.Violation violation = (MAuditParser.Violation) v.elementAt(i);
                Element error = doc.createElement("violation");
                error.setAttribute("line", violation.line);
                error.setAttribute("message", violation.error);
                clazz.appendChild(error);
            }
            rootElement.appendChild(clazz);
        }
        rootElement.setAttribute("violations", String.valueOf(errors));

        // now write it to the outputstream, not very nice code
        DOMElementWriter domWriter = new DOMElementWriter();
        try {
            domWriter.write(rootElement, xmlOut);
        } catch (IOException e){
            throw new BuildException(e);
        }
    }

    protected static DocumentBuilder getDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (Exception exc) {
            throw new ExceptionInInitializerError(exc);
        }
    }

    /**
     * Creates a stream pumper to copy the given input stream to the given output stream.
     */
    protected Thread createPump(InputStream is, OutputStream os) {
        final Thread result = new Thread(new StreamPumper(is, os));
        result.setDaemon(true);
        return result;
    }


    /** read each line and process it */
    protected void parseOutput(BufferedReader br) throws IOException {
        String line = null;
        final MAuditParser parser = new MAuditParser();
        while ((line = br.readLine()) != null) {
            final MAuditParser.Violation violation = parser.parseLine(line);
            if (violation != null) {
                addViolation(violation.file, violation);
            } else {
                // this doesn't match..report it as info, it could be
                // either the copyright, summary or a multiline message (damn !)
                task.log(line, Project.MSG_INFO);
            }
        }
    }

    /** add a violation entry for the file */
    private void addViolation(String file, MAuditParser.Violation entry) {
        Vector violations = (Vector) auditedFiles.get(file);
        // if there is no decl for this file yet, create it.
        if (violations == null) {
            violations = new Vector();
            auditedFiles.put(file, violations);
        }
        violations.addElement(entry);
    }

}
