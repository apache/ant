/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.types;

import org.apache.tools.ant.*;

import java.io.*;
import java.util.*;
import org.xml.sax.Locator;
import org.xml.sax.InputSource;
//import org.xml.sax.HandlerBase;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.SAXException;
import org.xml.sax.DocumentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.AttributeList;
import org.xml.sax.helpers.XMLReaderAdapter;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.AttributeListImpl;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Specify a system path, to be used to load optional.jar and all
 * related libraries.
 *
 * Using the specified path it'll try to load or reload all optional
 * tasks. The typical use is:
 * <pre>
 *  &lt;path id="ant.deps" &gt;
 *     &lt;fileset ... /&gt;
 *  &lt;/path&gt;
 *
 *  &lt;systemPath pathRef="ant.deps" /&gt;
 *
 *  &lt;junit ... /&gt;
 * </pre>
 *
 * This requires that ant-sax2.jar is included in ant/lib.
 *
 * It has a single property, a reference to a &lt;path&gt; containing all
 * the jars that you need. It'll automatically reload optional.jar
 * tasks in a different (non-delegating) loader.
 *
 * @author Costin Manolache
 */
public class SystemPath extends DataType {
    public static final String SYSTEM_LOADER_REF="ant.system.loader";
    
    public SystemPath() {
    }

    /** Specify which path will be used.
     */
    public void setPathRef( Reference pathRef ) throws BuildException {
        Path path=(Path)pathRef.getReferencedObject(project);

        initSystemLoader(path);

    }
    
    /** Will prepare the class loader to allow dynamic modifications
     *   of the classpath. Optional tasks are loaded in a different loader.
     */
    private void initSystemLoader(Path path) {
        try {
            if( project.getReference( SYSTEM_LOADER_REF ) != null )
                return; // already done that.
            
            // reverse loader
            AntClassLoader acl=new AntClassLoader( this.getClass().getClassLoader(), true );
            acl.addLoaderPackageRoot( "org.apache.tools.ant.taskdefs.optional");
            project.addReference( SYSTEM_LOADER_REF, acl );
            
            
            String list[]=path.list();
            for( int i=0; i<list.length; i++ ) {
                File f= new File( list[i] );
                if( f.exists() ) {
                    acl.addPathElement(f.getAbsolutePath());
                }
            }
            
            // XXX find the classpath
            String antHome=project.getProperty( "ant.home" );
            File optionalJar=new File( antHome + "/lib/optional.jar" );
            if( optionalJar.exists() )
                acl.addPathElement(optionalJar.getAbsolutePath() );

            // reinit the loader for optional, if they were in /lib/
            Hashtable tasks=project.getTaskDefinitions();

            //System.out.println("Replacing jars" );
            // reload all optional tasks in this loader.
            // Some tasks weren't defined by the normal init(), since deps were missing.
            String defs = "/org/apache/tools/ant/taskdefs/defaults.properties";

            try {
                Properties props = new Properties();
                InputStream in = this.getClass().getResourceAsStream(defs);
                if (in == null) {
                    throw new BuildException("Can't load default task list");
                }
                props.load(in);
                in.close();
                
                Enumeration enum = props.propertyNames();
                while (enum.hasMoreElements()) {
                    String key = (String) enum.nextElement();
                    String value = props.getProperty(key);
                    if( ! value.startsWith( "org.apache.tools.ant.taskdefs.optional" ))
                        continue;
                    // other classes that needs to be replaced ??
                    try {
                        Class taskClass = acl.loadClass(value);
                        project.addTaskDefinition(key, taskClass);
                        //System.out.println("Loaded " + key + " " + taskClass.getClassLoader() );
                    } catch (NoClassDefFoundError ncdfe) {
                        log("Could not load a dependent class ("
                            + ncdfe.getMessage() + ") for task " + key, Project.MSG_DEBUG);
                    } catch (ClassNotFoundException cnfe) {
                        log("Could not load class (" + value
                            + ") for task " + key, Project.MSG_DEBUG);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            } catch (IOException ioe) {
                throw new BuildException("Can't load default task list");
            }
            
        } catch( Exception ex ) {
            ex.printStackTrace();
        }
    }
}
