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
package org.apache.tools.ant.taskdefs.optional.metamata;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.*;
import org.apache.tools.ant.util.regexp.*;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.DOMElementWriter;

import org.w3c.dom.*;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;


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
 * @author <a href="sbailliez@imediation.com">Stephane Bailliez</a>
 */
class MAuditStreamHandler implements ExecuteStreamHandler {

    protected MAudit task;

    /** reader for stdout */
    protected BufferedReader br;

    /** matcher that will be used to extract the info from the line */
    protected RegexpMatcher matcher;

    /**
     * this is where the XML output will go, should mostly be a file
     * the caller is responsible for flushing and closing this stream
     */
    protected OutputStream xmlOut = null;

    /**
     * the multimap. The key in the map is the filepath that caused the audit
     * error and the value is a vector of MAudit.Violation entries.
     */
    protected Hashtable auditedFiles = new Hashtable();

    MAuditStreamHandler(MAudit task, OutputStream xmlOut){
        this.task = task;
        this.xmlOut = xmlOut;
        /** the matcher should be the Oro one. I don't know about the other one */
        matcher = (new RegexpMatcherFactory()).newRegexpMatcher();
        matcher.setPattern(MAudit.AUDIT_PATTERN);
    }

    /** Ignore. */
    public void setProcessInputStream(OutputStream os) {}

    /** Ignore. */
    public void setProcessErrorStream(InputStream is) {}

    /** Set the inputstream */
    public void setProcessOutputStream(InputStream is) throws IOException {
        br = new BufferedReader(new InputStreamReader(is));
    }

    /** Invokes parseOutput. This will block until the end :-(*/
    public void start() throws IOException {
        parseOutput(br);
    }

    /**
     * Pretty dangerous business here. It serializes what was extracted from
     * the MAudit output and write it to the output.
     */
    public void stop() {
        // serialize the content as XML, move this to another method
        // this is the only code that could be needed to be overrided
        Document doc = getDocumentBuilder().newDocument();
        Element rootElement = doc.createElement("classes");
        Enumeration keys = auditedFiles.keys();
        Hashtable filemapping = task.getFileMapping();
        rootElement.setAttribute("audited", String.valueOf(filemapping.size()));
        rootElement.setAttribute("reported", String.valueOf(auditedFiles.size()));
        int errors = 0;
        while (keys.hasMoreElements()){
            String filepath = (String)keys.nextElement();
            Vector v = (Vector)auditedFiles.get(filepath);
            String fullclassname = (String)filemapping.get(filepath);
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
            clazz.setAttribute("violations", String.valueOf(v.size()));
            errors += v.size();
            for (int i = 0; i < v.size(); i++){
                MAudit.Violation violation = (MAudit.Violation)v.elementAt(i);
                Element error = doc.createElement("violation");
                error.setAttribute("line", String.valueOf(violation.line));
                error.setAttribute("message", violation.error);
                clazz.appendChild(error);
            }
            rootElement.appendChild(clazz);
        }
        rootElement.setAttribute("violations", String.valueOf(errors));

        // now write it to the outputstream, not very nice code
        if (xmlOut != null) {
            Writer wri = null;
            try {
                wri = new OutputStreamWriter(xmlOut, "UTF-8");
                wri.write("<?xml version=\"1.0\"?>\n");
                (new DOMElementWriter()).write(rootElement, wri, 0, "  ");
                wri.flush();
            } catch(IOException exc) {
                task.log("Unable to write log file", Project.MSG_ERR);
            } finally {
                if (xmlOut != System.out && xmlOut != System.err) {
                    if (wri != null) {
                        try {
                            wri.close();
                        } catch (IOException e) {}
                    }
                }
            }
        }

    }

    protected static DocumentBuilder getDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        catch(Exception exc) {
            throw new ExceptionInInitializerError(exc);
        }
    }

    /** read each line and process it */
    protected void parseOutput(BufferedReader br) throws IOException {
        String line = null;
        while ( (line = br.readLine()) != null ){
            processLine(line);
        }
    }

    // we suppose here that there is only one report / line.
    // There will obviouslly be a problem if the message is on several lines...
    protected void processLine(String line){
        Vector matches = matcher.getGroups(line);
        if (matches != null) {
            String file = (String)matches.elementAt(1);
            int lineNum = Integer.parseInt((String)matches.elementAt(2));
            String msg = (String)matches.elementAt(3);
            addViolationEntry(file, MAudit.createViolation(lineNum, msg) );
        } else {
            // this doesn't match..report it as info, it could be
            // either the copyright, summary or a multiline message (damn !)
            task.log(line, Project.MSG_INFO);
        }
    }

    /** add a violation entry for the file */
    protected void addViolationEntry(String file, MAudit.Violation entry){
            Vector violations = (Vector)auditedFiles.get(file);
            // if there is no decl for this file yet, create it.
            if (violations == null){
                violations = new Vector();
                auditedFiles.put(file, violations);
            }
            violations.add( entry );
    }

}
