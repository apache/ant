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
package org.apache.tools.ant.taskdefs.optional;

import com.ibm.bsf.*;
import java.io.*;
import java.util.*;
import org.apache.tools.ant.*;

/**
 * Execute a script
 *
 * @author Sam Ruby <a href="mailto:rubys@us.ibm.com">rubys@us.ibm.com</a>
 */
public class Script extends Task {
    private String language;
    private String script = "";
    private Hashtable beans = new Hashtable();
    
    /**
     * Add a list of named objects to the list to be exported to the script
     */
    private void addBeans(Hashtable dictionary) {
        for (Enumeration e=dictionary.keys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();

            boolean isValid = key.length()>0 &&
                Character.isJavaIdentifierStart(key.charAt(0));

            for (int i=1; isValid && i<key.length(); i++)
                isValid = Character.isJavaIdentifierPart(key.charAt(i));

            if (isValid) beans.put(key, dictionary.get(key));
        }
    }

    /**
     * Do the work.
     *
     * @exception BuildException if someting goes wrong with the build
     */
    public void execute() throws BuildException {
        try {
            addBeans(project.getProperties());
            addBeans(project.getUserProperties());
            addBeans(project.getTargets());
            addBeans(project.getReferences());

            BSFManager manager = new BSFManager ();

            for (Enumeration e = beans.keys() ; e.hasMoreElements() ;) {
                String key = (String)e.nextElement();
                Object value = beans.get(key);
                manager.declareBean(key, value, value.getClass());
            }

            // execute the script
            manager.exec(language, "<ANT>", 0, 0, script);
        } catch (BSFException be) {
            Throwable t = be;
            Throwable te = be.getTargetException();
            if (te != null) {
                if  (te instanceof BuildException) {
                    throw (BuildException) te;
                } else {
                    t = te;
                }
            }
            throw new BuildException(t);
        }
    }

    /**
     * Defines the language (required).
     *
     * @param msg Sets the value for the script variable.
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Load the script from an external file 
     *
     * @param msg Sets the value for the script variable.
     */
    public void setSrc(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) 
            throw new BuildException("file " + fileName + " not found.");

        int count = (int)file.length();
        byte data[] = new byte[count];

        try {
            FileInputStream inStream = new FileInputStream(file);
            inStream.read(data);
            inStream.close();
        } catch (IOException e) {
            throw new BuildException(e);
        }
        
        script += new String(data);
    }

    /**
     * Defines the script.
     *
     * @param msg Sets the value for the script variable.
     */
    public void addText(String text) {
        this.script += text;
    }
}
