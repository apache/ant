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
import java.io.*;
import java.util.*;

/**
 * Will set a Project property. Used to be a hack in ProjectHelper
 * Will not override values set by the command line or parent projects.
 *
 * @author costin@dnt.ro
 * @author Sam Ruby <rubys@us.ibm.com>
 * @author Glenn McAllister <glennm@ca.ibm.com>
 */
public class Property extends Task {

    String name;
    String value;
    String file;
    String resource;

    boolean userProperty=false; // set read-only properties

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
	return name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
	return value;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public void init() throws BuildException {
        try {
            if ((name != null) && (value != null)) {
                addProperty(name, value);
            }

            if (file != null) loadFile(file);

            if (resource != null) loadResource(resource);

        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    private void loadFile (String name) throws BuildException {
        Properties props = new Properties();
        log("Loading " + name, Project.MSG_VERBOSE);
        try {
            if (new File(name).exists()) {
                props.load(new FileInputStream(name));
                addProperties(props);
            } else {
                log("Unable to find " + name, Project.MSG_VERBOSE);
            }
        } catch(Exception ex) {
            throw new BuildException(ex.getMessage(), ex, location);
        }
    }

    private void loadResource( String name ) {
        Properties props = new Properties();
        log("Resource Loading " + name, Project.MSG_VERBOSE);
        try {
            InputStream is = this.getClass().getResourceAsStream(name);
            if (is != null) {
                props.load(is);
                addProperties(props);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void addProperties(Properties props) {
        resolveAllProperties(props);
        Enumeration e = props.keys();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            String value = (String) props.getProperty(name);
            String v = ProjectHelper.replaceProperties(value, project.getProperties());
            addProperty(name, value);
        }
    }

    public void setUserProperty( boolean userP ) {
	userProperty=userP;
    }

    private void addProperty(String n, String v) {
        if( userProperty ) {
            if (project.getUserProperty(n) == null) {
                project.setUserProperty(n, v);
            } else {
                log("Override ignored for " + name, Project.MSG_VERBOSE);
            } 
        } else {
            if (project.getProperty(n) == null) {
                project.setProperty(n, v);
            } else {
                log("Override ignored for " + name, Project.MSG_VERBOSE);
            }
        }
    }

    private void resolveAllProperties(Hashtable props) {
        Hashtable toResolve = new Hashtable();
        Enumeration e = props.keys();
        boolean more = true;
        
        while (more) {
            while (e.hasMoreElements()) {
                Vector propsInValue = new Vector();
                String name = (String) e.nextElement();
                String value = (String) props.get(name);

                if (extractProperties(value, propsInValue)) {
                    for (int i=0; i < propsInValue.size(); i++) {
                        String elem = (String) propsInValue.elementAt(i);
                        if (project.getProperties().containsKey(elem) ||
                            props.containsKey(elem)) {
                            toResolve.put(name, value);
                            break;
                        }
                    }
                }

                if (toResolve.size() > 0) {
                    Enumeration tre = toResolve.keys();
                    while (tre.hasMoreElements()) {
                        String name2 = (String) tre.nextElement();
                        String value2 = (String) toResolve.get(name2);
                        String v = ProjectHelper.replaceProperties(value2,
                                                                   project.getProperties());
                        v = ProjectHelper.replaceProperties(v, props);
                        props.put(name, v);
                    }

                    toResolve.clear();
                    e = props.keys();
                } else {
                    more = false;
                }
            }
        }
    }

    private boolean extractProperties(String source, Vector properties) {
        // This is an abreviated version of 
        // ProjectHelper.replaceProperties method
        int i=0;
        int prev=0;
        int pos;

        while( (pos=source.indexOf( "$", prev )) >= 0 ) {
            if( pos == (source.length() - 1)) {
                prev = pos + 1;
            } else if (source.charAt( pos + 1 ) != '{' ) {
                prev=pos+2;
            } else {
                int endName=source.indexOf( '}', pos );
                String n=source.substring( pos+2, endName );
                properties.addElement(n);
                prev=endName+1;
            }
        }
        
        return (properties.size() > 0);
    }
    
}
