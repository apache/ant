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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * This task acts as a loader for java applications but allows to use the same JVM 
 * for the called application thus resulting in much faster operation.
 *
 * @author Stefano Mazzocchi <a href="mailto:stefano@pache.org">stefano@apache.org</a>
 */
public class Java extends Task {

    private String classname = null;
    private String args = null;
    
    /**
     * Do the execution.
     */
    public void execute() throws BuildException {
        
        project.log("Calling " + classname, "java", project.MSG_VERBOSE);
        
        if (classname == null) {
            throw new BuildException("Class name must not be null.");
        }

        Vector argList = tokenize(args);
        
        project.log("Java args: " + argList.toString(), "java", project.MSG_VERBOSE);
        
        run(classname, argList);
    }
    
    /**
     * Set the source file.
     */
    public void setClass(String s) {
        this.classname = s;
    }

    /**
     * Set the destination file.
     */
    public void setArgs(String s) {
        this.args = s;
    }

    /**
     * Executes the given classname with the given arguments as it
     * was a command line application.
     */
    protected void run(String classname, Vector args) throws BuildException {
        try {
            Class[] param = { Class.forName("[Ljava.lang.String;") };
            Class c = Class.forName(classname);
            Method main = c.getMethod("main", param);
            Object[] a = { array(args) };
            main.invoke(null, a);
        } catch (NullPointerException e) {
            throw new BuildException("Could not find main() method in " + classname);
        } catch (ClassNotFoundException e) {
            throw new BuildException("Could not find " + classname + ". Make sure you have it in your classpath");
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            if (!(t instanceof SecurityException)) {
                throw new BuildException(t.toString());
            }
            // else ignore because the security exception is thrown
            // if the invoked application tried to call System.exit()
        } catch (Exception e) {
            throw new BuildException(e.toString());
        }
    }

    /**
     * Transforms an argument string into a vector of strings.
     */
    protected Vector tokenize(String args) {
        Vector v = new Vector();
        StringTokenizer t = new StringTokenizer(args, " ");
        
        while (t.hasMoreTokens()) {
            v.addElement(t.nextToken());
        }

        return v;
    }
    
    /**
     * Transforms a vector of strings into an array.
     */
    protected String[] array(Vector v) {
        String[] s = new String[v.size()];
        Enumeration e = v.elements();
        
        for (int i = 0; e.hasMoreElements(); i++) {
            s[i] = (String) e.nextElement();
        }
        
        return s;
    }
}
