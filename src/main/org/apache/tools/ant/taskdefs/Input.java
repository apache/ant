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

package org.apache.tools.ant.taskdefs;

import  java.io.BufferedReader;
import  java.io.InputStreamReader;
import  java.io.IOException;
import  java.util.StringTokenizer;
import  java.util.Vector;
import org.apache.tools.ant.Task;

import org.apache.tools.ant.BuildException;

import org.apache.tools.ant.Project;


/**
 * Ant task to read input line from console.
 *
 * @author <a href="mailto:usch@usch.net">Ulrich Schmidt</a>
 *
 * @ant.task category="control"
 */
public class Input extends Task {
    private String validargs = null;
    private String message = "";
    private String addproperty = null;
    private String input = null;

    /**
     * Defines valid input parameters as comma separated String. If set, input
     * task will reject any input not defined as accepted and requires the user
     * to reenter it. Validargs are case sensitive. If you want 'a' and 'A' to
     * be accepted you need to define both values as accepted arguments.
     *
     * @param validargs A comma separated String defining valid input args.
     */
    public void setValidargs (String validargs) {
        this.validargs = validargs;
    }

    /**
     * Defines the name of a property to be created from input. Behaviour is
     * according to property task which means that existing properties
     * cannot be overriden.
     *
     * @param addproperty Name for the property to be created from input
     */
    public void setAddproperty (String addproperty) {
        this.addproperty = addproperty;
    }

    /**
     * Sets the Message which gets displayed to the user during the build run.
     * @param message The message to be displayed.
     */
    public void setMessage (String message) {
        this.message = message;
    }

    /**
     * Sets surrogate input to allow automated testing.
     * @param input The surrogate input used for testing.
     */
    public void setTestinput (String testinput) {
        this.input = testinput;
    }

    /**
     * No arg constructor.
     */
    public Input () {
    }

    /**
     * Actual test method executed by jakarta-ant.
     * @exception BuildException
     */
    public void execute () throws BuildException {
        Vector accept = null;
        if (validargs != null) {
            accept = new Vector();
            StringTokenizer stok = new StringTokenizer(validargs, ",", false);
            while (stok.hasMoreTokens()) {
                accept.addElement(stok.nextToken());
            }
        }
        log(message, Project.MSG_WARN);
        if (input == null) {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                input = in.readLine();
                if (accept != null) {
                    while (!accept.contains(input)) {
                        log(message, Project.MSG_WARN);
                        input = in.readLine();
                    }
                }
            } catch (IOException e) {
                throw  new BuildException("Failed to read input from Console.", e);
            }
        }
        // not quite the original intention of this task but for the sake
        // of testing ;-)
        else {
            if (accept != null && (!accept.contains(input))) {
                throw  new BuildException("Invalid input please reenter.");
            }
        }
        // adopted from org.apache.tools.ant.taskdefs.Property
        if (addproperty != null) {
            if (project.getProperty(addproperty) == null) {
                project.setProperty(addproperty, input);
            }
            else {
                log("Override ignored for " + addproperty, Project.MSG_VERBOSE);
            }
        }
    }

    // copied n' pasted from org.apache.tools.ant.taskdefs.Exit
    /**
     * Set a multiline message.
     */
    public void addText(String msg) {
        message += project.replaceProperties(msg);
    }
}



