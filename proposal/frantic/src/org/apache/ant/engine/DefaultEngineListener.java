/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999, 2000 The Apache Software Foundation.  All rights
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
package org.apache.ant.engine;


import java.io.*;
import java.text.*;
import java.util.*;
import org.apache.ant.AntException;
import org.apache.ant.tasks.Task;

public class DefaultEngineListener implements AntEngineListener {
    
    protected PrintStream       outputStream;
    protected long              startTime;
    
    protected StringBuffer      sb              = new StringBuffer();
    protected int               indentSpaces    = 4;
    protected boolean           indent          = true;
    protected int               curIndent       = 0;
    
    protected SimpleDateFormat  timestamp       = new SimpleDateFormat("HH:mm:ss:SSS");
    
    public DefaultEngineListener() {
        this(System.out);
    }
    
    public DefaultEngineListener(PrintStream outputStream) {
        this.outputStream = outputStream;
    }
    
    public void setIndentSpaces(int spaces) {
        this.indentSpaces = spaces;
    }
    
    public int getIndentSpaces() {
        return indentSpaces;
    }
    
    public void setIndent(boolean on) {
        this.indent = on;
    }
    
    public boolean isIndent() {
        return indent;
    }
    
    protected String padLeft(String s, int length) {
        sb.setLength(0);
        sb.append(s);
        while (sb.length() < length) {
            sb.insert(0, ' ');
        }
        return sb.toString();
    }
    
    protected void output(String message) {
        if (!indent) {
            outputStream.println(message);
            return;
        }
        
        // shouldn't happen, but let's be on the safe side
        if (curIndent < 0) {
            curIndent = 0;
        }
        
        outputStream.println(
                             padLeft(message, message.length() + (indentSpaces * curIndent)));
    }
    
    ////////////////////////////////////////////////////////////////////////////
    //                     AntEngineListener Implementation                   //
    ////////////////////////////////////////////////////////////////////////////
    
    public void engineStart(AntEvent e) {
        Date now = new Date();
        output("Engine Started: " + timestamp.format(now));
        startTime = now.getTime();
    }
    
    public void engineFinish(AntEvent e) {
        Date now = new Date();
        long elapsed = System.currentTimeMillis() - startTime;
        
        output("Engine Finished: " + timestamp.format(now));
        output("Elapsed Time: " + (elapsed / 1000F) + " seconds");
    }
    
    public void taskStart(AntEvent e) {
        output("Task Started: " + e.getTask().getFullyQualifiedName());
        curIndent++;
    }
    
    public void taskExecute(AntEvent e){
        output("Task Execution: " + e.getTask().getFullyQualifiedName());
    }
    
    public void taskFinish(AntEvent e){
        curIndent--;
        output("Task Finished: " + e.getTask().getFullyQualifiedName());
    }
    
    public void taskMessage(AntEvent e, String message){
        curIndent++;
        output("Task Message: " + e.getTask().getFullyQualifiedName() + ": " +
               message);
        curIndent--;
    }
    
    public void taskException(AntEvent e, AntException exception){
        output("Task Exception: " + e.getTask().getFullyQualifiedName() + ": " +
               exception.getMessage());
    }
}
