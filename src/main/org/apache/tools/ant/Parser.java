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

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Dummy parser abstraction class for ANT to be used until the Java API for
 * XML Parsing are released.
 * 
 * @author pier@apache.org
 */
public abstract class Parser {
    // The properties file to be loaded
    private static String properties="org/apache/tools/ant/parser.properties";

    /**
     * Create a working instance of a parser 
     */
    public static Parser getParser(Project project)
    throws BuildException {
        InputStream in=project.getClass().getResourceAsStream("parser.properties");
        if (in==null) throw new BuildException("Cannot find properties file");

        String name;
        try {
            Properties prop=new Properties();
            prop.load(in);
            name=prop.getProperty("parser");
            if (name==null) throw new BuildException("Parser name not found");
        } catch(IOException e) {
            throw new BuildException("Cannot load properties file");
        }
        try {
            return((Parser)Class.forName(name).newInstance());
        } catch (ClassNotFoundException e) {
            throw new BuildException("Class "+name+" cannot be found");
        } catch (InstantiationException e) {
            throw new BuildException("Class "+name+" cannot be instantiated");
        } catch (IllegalAccessException e) {
            throw new BuildException("Class "+name+" cannot be accessed");
        } catch (ClassCastException e) {
            throw new BuildException("Class "+name+" doesn't extend Parser");
        }
    }
    
    /**
     * Parse the specified file and return a DOM Document.
     */
    public abstract Document parse(File f)
    throws SAXException, IOException;
}

        
