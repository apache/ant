/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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

package org.apache.ant.component.core;

import java.util.*;
import java.io.*;
import org.apache.ant.core.execution.*;
import java.net.*;

/**
 * 
 */ 
public class Property extends ExecutionTask {
    private String name;
    private String value;
    private URL file;
    private String resource;
//    private Path classpath;
    private String env;
//    private Reference ref = null;
    
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

    public void setFile(URL file) {
        this.file = file;
    }

    public URL getFile() {
        return file;
    }

    public void setLocation(File location) {
        setValue(location.getAbsolutePath());
    }

//    public void setRefid(Reference ref) {
//        this.ref = ref;
//    }
//
//    public Reference getRefid() {
//        return ref;
//    }
//
    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }

    public void setEnvironment(String env) {
        this.env = env;
    }

    public String getEnvironment() {
        return env;
    }

//    public void setClasspath(Path classpath) {
//        if (this.classpath == null) {
//            this.classpath = classpath;
//        } else {
//            this.classpath.append(classpath);
//        }
//    }
//    
//    public Path createClasspath() {
//        if (this.classpath == null) {
//            this.classpath = new Path(project);
//        }
//        return this.classpath.createPath();
//    }
//    
//    public void setClasspathRef(Reference r) {
//        createClasspath().setRefid(r);
//    }
//

    public void execute() throws ExecutionException {
        ExecutionFrame frame = getExecutionFrame();
        if ((name != null) && (value != null)) {
            frame.setDataValue(name, value);
        }

        if (file != null) {
            loadFile(file);
        }

//        if (resource != null) loadResource(resource);
//
//        if (env != null) loadEnvironment(env);
//
//        if ((name != null) && (ref != null)) {
//            Object obj = ref.getReferencedObject(getProject());
//            if (obj != null) {
//                addProperty(name, obj.toString());
//            }
//        }
    }

    protected void loadFile (URL url) throws ExecutionException {
        Properties props = new Properties();
        log("Loading " + url, BuildEvent.MSG_VERBOSE);
        try {
            InputStream stream = null;
            if (url.getProtocol().equals("file")) {
                File file = new File(url.getFile());
            
                if (file.exists()) {
                    stream = new FileInputStream(file);
                } 
                else {
                    throw new ExecutionException("Unable to find " + file.getAbsolutePath());
                }
            }
            else {
                stream = url.openStream();
            }
            
            if (stream != null) {
                try { 
                    props.load(stream);
                    resolveAllProperties(props);
                    addProperties(props);
                } finally {
                    stream.close();
                }
            }
        } catch (Exception ex) {
            throw new ExecutionException("Unable to load property file: " + url, ex);
        }
    }

    protected void addProperties(Properties properties) throws ExecutionException {
        ExecutionFrame frame = getExecutionFrame();
        for (Iterator i = properties.keySet().iterator(); i.hasNext();) {
            String propertyName = (String)i.next();
            String propertyValue = properties.getProperty(propertyName);
            frame.setDataValue(propertyName, frame.replacePropertyRefs(propertyValue));
        }
    }

    private void resolveAllProperties(Properties props) throws ExecutionException {
        for (Iterator propIterator = props.keySet().iterator(); propIterator.hasNext();) {
            String name = (String)propIterator.next();
            String value = props.getProperty(name);

            boolean resolved = false;
            while (!resolved) {
                List fragments = new ArrayList();
                List propertyRefs = new ArrayList();
                ExecutionFrame.parsePropertyString(value, fragments, propertyRefs);
                
                resolved = true;
                if (propertyRefs.size() != 0) {
                    StringBuffer sb = new StringBuffer();
                    Iterator i = fragments.iterator();
                    Iterator j = propertyRefs.iterator();
                    while (i.hasNext()) {
                        String fragment = (String)i.next();
                        if (fragment == null) {
                            String propertyName = (String)j.next();
                            if (propertyName.equals(name)) {
                                throw new ExecutionException("Property " + name 
                                                             + " from " + file 
                                                             + " was circularly defined.");
                            }
                            if (props.containsKey(propertyName)) {
                                fragment = props.getProperty(propertyName);
                                resolved = false;
                            }
                            else {
                                fragment = "${" + propertyName + "}";
                            }
                        }
                        sb.append(fragment);
                    }
                    value = sb.toString();
                    props.put(name, value);
                }
            }
        }
    }    
}

