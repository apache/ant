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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;

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
 * EXPERIMENTAL
 * Create or modifies ClassLoader. The required pathRef parameter
 * will be used to add classpath elements.
 *
 * The classpath is a regular path. Currently only file components are
 * supported ( future extensions may allow URLs ).
 *
 * You can modify the core loader by not specifying any name or using
 * "ant.coreLoader". ( the core loader is used to load system ant
 * tasks and for taskdefs that don't specify an explicit path ).
 *
 * Taskdef and typedef can use the loader you create if the name follows
 * the "ant.loader.NAME" pattern. NAME will be used as a pathref when
 * calling taskdef.
 *
 * This tasks will not modify the core loader if "build.sysclasspath=only"
 *
 * The typical use is:
 * <pre>
 *  &lt;path id="ant.deps" &gt;
 *     &lt;fileset dir="myDir" &gt;
 *        &lt;include name="junit.jar, bsf.jar, js.jar, etc"/&gt;
 *     &lt;/fileset&gt;
 *  &lt;/path&gt;
 *
 *  &lt;classloader pathRef="ant.deps" /&gt;
 *
 * </pre>
 *
 * @author Costin Manolache
 */
public class Classloader extends Task {
    public static final String SYSTEM_LOADER_REF="ant.coreLoader";

    private String name=null;
    private Path path;
    private boolean reset=false;
    private boolean reverse=false;
    private String parentName=null;

    public Classloader() {
    }

    /** Name of the loader. If none, the default loader will be modified
     *
     * @param name
     */
    public void setName(String name) {
        this.name=name;
    }

    /** Reset the classloader, if it already exists. A new loader will
     * be created and all the references to the old one will be removed.
     * ( it is not possible to remove paths from a loader ). The new
     * path will be used.
     *
     * @param b
     */
    public void setReset(boolean b) {
        this.reset=b;
    }

    // TODO
    public void setReverse(boolean b ) {
        this.reverse=b;
    }

    // TODO: add exceptions for delegation or reverse

    // TODO
    public void setParentName( String name ) {
        this.parentName=name;
    }


    /** Specify which path will be used. If the loader already exists
     *  and is an AntClassLoader ( or any other loader we can extend ),
     *  the path will be added to the loader.
     */
    public void setPathRef( Reference pathRef ) throws BuildException {
        path=(Path)pathRef.getReferencedObject(project);
    }


    public void execute() {
        try {
            // Gump friendly - don't mess with the core loader if only classpath
            if( "only".equals( project.getProperty("build.sysclasspath")) &&
                    (name==null || SYSTEM_LOADER_REF.equals( name ))) {
                log( "Changing the system loader is disabled " +
                        "by build.sysclasspath=only",
                        Project.MSG_WARN);
                return;
            }

            String loaderName=(name==null) ? SYSTEM_LOADER_REF : name;

            Object obj=project.getReference(loaderName);
            if( reset ) {
                // Are any other references held ? Can we 'close' the loader
                // so it removes the locks on jars ?
                obj=null; // a new one will be created.
            }

            // XXX maybe use reflection to addPathElement ( other patterns ?)
            if( obj!=null && !(obj instanceof AntClassLoader )) {
                log( "Referenced object is not an AntClassLoader",
                        Project.MSG_ERR);
                return;
            }

            AntClassLoader acl=(AntClassLoader)obj;

            if( acl==null ) {
                // Construct a class loader
                Object parent=null;
                if( parentName != null ) {
                    parent=project.getReference(parentName);
                    if( !(parent instanceof ClassLoader) ) {
                        parent=null;
                    }
                }
                if( parent==null ) {
                    parent=this.getClass().getClassLoader();
                }

                acl=new AntClassLoader( (ClassLoader)parent,
                        project, path, reverse );
                project.addReference( loaderName, acl );
                if( name==null ) {
                    // This allows the core loader to load optional tasks
                    // without delegating
                    acl.addLoaderPackageRoot( "org.apache.tools.ant.taskdefs.optional");
                    project.setCoreLoader(acl);
                }
            }
            if( path != null ) {
                String list[]=path.list();
                for( int i=0; i<list.length; i++ ) {
                    File f= new File( list[i] );
                    if( f.exists() ) {
                        acl.addPathElement(f.getAbsolutePath());
                        log("Adding to class loader " +  acl + " " + f.getAbsolutePath(),
                                Project.MSG_DEBUG);
                    }
                }
            }

            // XXX add exceptions

        } catch( Exception ex ) {
            ex.printStackTrace();
        }
    }
}
