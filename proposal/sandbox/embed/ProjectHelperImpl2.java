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

package org.apache.tools.ant.helper;

import org.apache.tools.ant.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Stack;
import org.xml.sax.Locator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.SAXException;
import org.xml.sax.DocumentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.AttributeList;
import org.xml.sax.helpers.XMLReaderAdapter;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.AttributeListImpl;

import org.apache.tools.ant.util.JAXPUtils;

/**
 * Sax2 based project reader
 *
 * @author duncan@x180.com
 * @author Costin Manolache
 */
public class ProjectHelperImpl2 extends ProjectHelper {
    /* Stateless */


    public void parse(Project project, Object source) throws BuildException {
        // Hook our internal tasks. XXX refactor 
        try {
            Class c=Class.forName("org.apache.tools.ant.types.SystemPath");
            project.addDataTypeDefinition( "systemPath" , c );
            c=Class.forName("org.apache.tools.ant.tasks.Import");
            project.addTaskDefinition( "import" , c );
        } catch (Exception ex ) {
        }
        AntXmlContext context=new AntXmlContext(this);
        AntXmlContext.resetImports();
        
        project.addReference( "ant.parsing.context", context );
        parse(project, source,new RootHandler(context));
        context.implicitTarget.execute();
    }

    /**
     * Parses the project file, configuring the project as it goes.
     * 
     * @exception BuildException if the configuration is invalid or cannot 
     *                           be read
     */
    public void parse(Project project, Object source, RootHandler handler) throws BuildException {
        
        AntXmlContext context=handler.context;

        if(source instanceof File) {
            context.buildFile=(File)source;
//         } else if( source instanceof InputStream ) {
//         } else if( source instanceof URL ) {
//         } else if( source instanceof InputSource ) {
        } else {
            throw new BuildException( "Source " + source.getClass().getName() +
                                      " not supported by this plugin" );
        }

        FileInputStream inputStream = null;
        InputSource inputSource = null;

        context.project = project;
        context.buildFile = new File(context.buildFile.getAbsolutePath());
        context.buildFileParent = new File(context.buildFile.getParent());
        
        try {
            /**
             * SAX 2 style parser used to parse the given file. 
             */
            context.parser =JAXPUtils.getXMLReader();

            String uri = "file:" + context.buildFile.getAbsolutePath().replace('\\', '/');
            for (int index = uri.indexOf('#'); index != -1; index = uri.indexOf('#')) {
                uri = uri.substring(0, index) + "%23" + uri.substring(index+1);
            }
            
            inputStream = new FileInputStream(context.buildFile);
            inputSource = new InputSource(inputStream);
            inputSource.setSystemId(uri);
            project.log("parsing buildfile " + context.buildFile + " with URI = " + uri, Project.MSG_VERBOSE);

            DefaultHandler hb = handler;

            context.parser.setContentHandler(hb);
            context.parser.setEntityResolver(hb);
            context.parser.setErrorHandler(hb);
            context.parser.setDTDHandler(hb);
            context.parser.parse(inputSource);
        } catch(SAXParseException exc) {
            Location location =
                new Location(exc.getSystemId(), exc.getLineNumber(), exc.getColumnNumber());

            Throwable t = exc.getException();
            if (t instanceof BuildException) {
                BuildException be = (BuildException) t;
                if (be.getLocation() == Location.UNKNOWN_LOCATION) {
                    be.setLocation(location);
                }
                throw be;
            }
            
            throw new BuildException(exc.getMessage(), t, location);
        }
        catch(SAXException exc) {
            Throwable t = exc.getException();
            if (t instanceof BuildException) {
                throw (BuildException) t;
            }
            throw new BuildException(exc.getMessage(), t);
        }
        catch(FileNotFoundException exc) {
            throw new BuildException(exc);
        }
        catch(UnsupportedEncodingException exc) {
              throw new BuildException("Encoding of project file is invalid.",exc);
        }
        catch(IOException exc) {
            throw new BuildException("Error reading project file: " +exc.getMessage(), exc);
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (IOException ioe) {
                    // ignore this
                }
            }
        }
    }

    /**
     * The common superclass for all SAX event handlers used to parse
     * the configuration file. Each method just throws an exception, 
     * so subclasses should override what they can handle.
     *
     * Each type of XML element (task, target, etc.) in Ant has
     * a specific subclass.
     *
     * In the constructor, this class takes over the handling of SAX
     * events from the parent handler and returns
     * control back to the parent in the endElement method.
     */
    public static class AntHandler  {
        /**
         * Handles the start of an element. This base implementation does nothing.
         * 
         * @param tag The name of the element being started. 
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         * 
         * @exception SAXParseException if this method is not overridden, or in
         *                              case of error in an overridden version
         */
        public void onStartElement(String uri, String tag, String qname,
                                   Attributes attrs,
                                   AntXmlContext context)
            throws SAXParseException
        {
        }

        /**
         * Handles the start of an element. This base implementation just
         * throws an exception - you must override this method if you expect
         * child elements.
         * 
         * @param tag The name of the element being started. 
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         * 
         * @exception SAXParseException if this method is not overridden, or in
         *                              case of error in an overridden version
         */
        public AntHandler onStartChild(String uri, String tag, String qname,
                                       Attributes attrs,
                                       AntXmlContext context)
            throws SAXParseException
        {
            throw new SAXParseException("Unexpected element \"" + qname + " \"", context.locator);
        }

        /**
         * Called when this element and all elements nested into it have been
         * handled (i.e. at the </end_tag_of_the_element> ).
         */
        public void onEndElement(String uri, String tag, AntXmlContext context) {
        }

        /**
         * Handles text within an element. This base implementation just
         * throws an exception, you must override it if you expect content.
         * 
         * @param buf A character array of the text within the element.
         *            Will not be <code>null</code>.
         * @param start The start element in the array.
         * @param count The number of characters to read from the array.
         * 
         * @exception SAXParseException if this method is not overridden, or in
         *                              case of error in an overridden version
         */
        public void characters(char[] buf, int start, int count, AntXmlContext context)
            throws SAXParseException
        {
            String s = new String(buf, start, count).trim();

            if (s.length() > 0) {
                throw new SAXParseException("Unexpected text \"" + s + "\"", context.locator);
            }
        }
    }

    /** Context information for ant deserialization
     */
    public static class AntXmlContext {
        /** The project to configure. */
        Project project;
        /** The configuration file to parse. */
        public File buildFile;
        /** 
         * Parent directory of the build file. Used for resolving entities
         * and setting the project's base directory.
         */
        public File buildFileParent;

        public String currentProjectName;

        /** 
         * Locator for the configuration file parser. 
         * Used for giving locations of errors etc.
         */
        Locator locator;

        public ProjectHelperImpl2 helper;
        
        org.xml.sax.XMLReader parser;

         /**
          * Target that all other targets will depend upon implicitly.
          *
          * <p>This holds all tasks and data type definitions that have
          * been placed outside of targets.</p>
          */
        Target implicitTarget = new Target();

        public Target currentTarget=null;
        
        public boolean ignoreProjectTag=false;
        public static Hashtable importedFiles = new Hashtable();
        public static int importlevel = 0;

        public static void resetImports() {
            importedFiles.clear();
        }

        public AntXmlContext(ProjectHelperImpl2 helper) {
            implicitTarget.setName("");
            this.helper=helper;
        }

        /**
         * Scans an attribute list for the <code>id</code> attribute and 
         * stores a reference to the target object in the project if an
         * id is found.
         * <p>
         * This method was moved out of the configure method to allow
         * it to be executed at parse time.
         * 
         * @see #configure(Object,AttributeList,Project)
         */
        void configureId(Object target, Attributes attr) {
            String id = attr.getValue("id");
            if (id != null) {
                project.addReference(id, target);
            }
        }

    }
    
    /**
     * Handler for ant processing. Uses a stack of AntHandlers to
     * implement each element ( the original parser used a recursive behavior,
     * with the implicit execution stack )
     */
    public static class RootHandler extends DefaultHandler {
        Stack antHandlers=new Stack();
        AntHandler currentHandler=null;
        AntXmlContext context;
        
        public RootHandler(AntXmlContext context) {
            currentHandler=new MainHandler();
            antHandlers.push( currentHandler );
            this.context=context;
        }
        
        /**
         * Resolves file: URIs relative to the build file.
         * 
         * @param publicId The public identifer, or <code>null</code>
         *                 if none is available. Ignored in this 
         *                 implementation.
         * @param systemId The system identifier provided in the XML 
         *                 document. Will not be <code>null</code>.
         */
        public InputSource resolveEntity(String publicId,
                                         String systemId) {
        
            context.project.log("resolving systemId: " + systemId, Project.MSG_VERBOSE);
        
            if (systemId.startsWith("file:")) {
                String path = systemId.substring(5);
                int index = path.indexOf("file:");
                
                // we only have to handle these for backward compatibility
                // since they are in the FAQ.
                while (index != -1) {
                    path = path.substring(0, index) + path.substring(index + 5);
                    index = path.indexOf("file:");
                }
                
                String entitySystemId = path;
                index = path.indexOf("%23");
                // convert these to #
                while (index != -1) {
                    path = path.substring(0, index) + "#" + path.substring(index + 3);
                    index = path.indexOf("%23");
                }

                File file = new File(path);
                if (!file.isAbsolute()) {
                    file = new File(context.buildFileParent, path);
                }
                
                try {
                    InputSource inputSource = new InputSource(new FileInputStream(file));
                    inputSource.setSystemId("file:" + entitySystemId);
                    return inputSource;
                } catch (FileNotFoundException fne) {
                    context.project.log(file.getAbsolutePath()+" could not be found", 
                                        Project.MSG_WARN);
                }
            }
            // use default if not file or file not found
            return null;
        }

        /**
         * Handles the start of a project element. A project handler is created
         * and initialised with the element name and attributes.
         * 
         * @param tag The name of the element being started. 
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         * 
         * @exception SAXParseException if the tag given is not 
         *                              <code>"project"</code>
         */
        public void startElement(String uri, String tag, String qname, Attributes attrs)
            throws SAXParseException
        {
            AntHandler next=currentHandler.onStartChild(uri, tag, qname, attrs, context);
            antHandlers.push( currentHandler );
            currentHandler=next;
            currentHandler.onStartElement( uri, tag, qname, attrs, context );
        }

        /**
         * Sets the locator in the project helper for future reference.
         * 
         * @param locator The locator used by the parser.
         *                Will not be <code>null</code>.
         */
        public void setDocumentLocator(Locator locator) {
            context.locator = locator;
        }

        /**
         * Handles the end of an element. Any required clean-up is performed
         * by the onEndElement() method and then the original handler is restored to
         * the parser.
         * 
         * @param name The name of the element which is ending.
         *             Will not be <code>null</code>.
         * 
         * @exception SAXException in case of error (not thrown in 
         *                         this implementation)
         * 
         */
        public void endElement(String uri, String name, String qName) throws SAXException {
            currentHandler.onEndElement(uri, name, context);
            AntHandler prev=(AntHandler)antHandlers.pop();
            currentHandler=prev;
        }

        public void characters(char[] buf, int start, int count)
            throws SAXParseException
        {
            currentHandler.characters( buf, start, count, context );
        }
    }

    public static class MainHandler extends AntHandler {

        public AntHandler onStartChild(String uri, String name, String qname,
                                       Attributes attrs,
                                       AntXmlContext context)
            throws SAXParseException
        {
            if (qname.equals("project")) {
                return new ProjectHandler();
            } else {
                throw new SAXParseException("Unexpected element \"" + qname + "\" " + name, context.locator);
            }
        }        
    }
    
    /**
     * Handler for the top level "project" element.
     */
    public static class ProjectHandler extends AntHandler {
        
        /**
         * Initialisation routine called after handler creation
         * with the element name and attributes. The attributes which
         * this handler can deal with are: <code>"default"</code>,
         * <code>"name"</code>, <code>"id"</code> and <code>"basedir"</code>.
         * 
         * @param tag Name of the element which caused this handler
         *            to be created. Should not be <code>null</code>.
         *            Ignored in this implementation.
         * @param attrs Attributes of the element which caused this
         *              handler to be created. Must not be <code>null</code>.
         * 
         * @exception SAXParseException if an unexpected attribute is 
         *            encountered or if the <code>"default"</code> attribute
         *            is missing.
         */
        public void onStartElement(String uri, String tag, String qname,
                                   Attributes attrs,
                                   AntXmlContext context)
            throws SAXParseException
        {
            String id = null;
            String baseDir = null;

            Project project=context.project;

            for (int i = 0; i < attrs.getLength(); i++) {
                String key = attrs.getQName(i);
                String value = attrs.getValue(i);
                
                if (key.equals("default")) {
                    if ( value != null && !value.equals("")) {
                        if( !context.ignoreProjectTag )
                            project.setDefaultTarget(value);
                    }
                } else if (key.equals("name")) {
                    if (value != null) {
                        context.currentProjectName=value;

                        if( !context.ignoreProjectTag ) {
                            project.setName(value);
                            project.addReference(value, project);
                        } 
                    }
                } else if (key.equals("id")) {
                    if (value != null) {
                        // What's the difference between id and name ?
                        if( !context.ignoreProjectTag ) {
                            project.addReference(value, project);
                        }
                    }
                } else if (key.equals("basedir")) {
                    if( !context.ignoreProjectTag )
                        baseDir = value;
                } else {
                    // XXX ignore attributes in a different NS ( maybe store them ? )
                    throw new SAXParseException("Unexpected attribute \"" + attrs.getQName(i) + "\"", context.locator);
                }
            }

            if( context.ignoreProjectTag ) {
                // no further processing
                return;
            }
            // set explicitely before starting ?
            if (project.getProperty("basedir") != null) {
                project.setBasedir(project.getProperty("basedir"));
            } else {
                // Default for baseDir is the location of the build file.
                if (baseDir == null) {
                    project.setBasedir(context.buildFileParent.getAbsolutePath());
                } else {
                    // check whether the user has specified an absolute path
                    if ((new File(baseDir)).isAbsolute()) {
                        project.setBasedir(baseDir);
                    } else {
                        project.setBaseDir(project.resolveFile(baseDir,
                                                               context.buildFileParent));
                    }
                }
            }
            
            project.addTarget("", context.implicitTarget);
            context.currentTarget=context.implicitTarget;
        }

        /**
         * Handles the start of a top-level element within the project. An
         * appropriate handler is created and initialised with the details
         * of the element.
         * 
         * @param tag The name of the element being started. 
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         * 
         * @exception SAXParseException if the tag given is not 
         *            <code>"taskdef"</code>, <code>"typedef"</code>,
         *            <code>"property"</code>, <code>"target"</code>
         *            or a data type definition
         */
        public AntHandler onStartChild(String uri, String name, String qname,
                                       Attributes attrs,
                                       AntXmlContext context)
            throws SAXParseException
        {
            if (qname.equals("target")) {
                return new TargetHandler();
            } else {
                return new ElementHandler(context.implicitTarget,null,context.implicitTarget);
            } 
        }
    }

    /**
     * Handler for "target" elements.
     */
    public static class TargetHandler extends AntHandler {

        /**
         * Initialisation routine called after handler creation
         * with the element name and attributes. The attributes which
         * this handler can deal with are: <code>"name"</code>,
         * <code>"depends"</code>, <code>"if"</code>,
         * <code>"unless"</code>, <code>"id"</code> and 
         * <code>"description"</code>.
         * 
         * @param tag Name of the element which caused this handler
         *            to be created. Should not be <code>null</code>.
         *            Ignored in this implementation.
         * @param attrs Attributes of the element which caused this
         *              handler to be created. Must not be <code>null</code>.
         * 
         * @exception SAXParseException if an unexpected attribute is encountered
         *            or if the <code>"name"</code> attribute is missing.
         */
        public void onStartElement(String uri, String tag, String qname,
                                   Attributes attrs,
                                   AntXmlContext context)
            throws SAXParseException
        {
            String name = null;
            String depends = "";

            Project project=context.project;
            Target target = new Target();
            context.currentTarget=target;

            for (int i = 0; i < attrs.getLength(); i++) {
                String key = attrs.getQName(i);
                String value = attrs.getValue(i);

                if (key.equals("name")) {
                    name = value;
                    if( "".equals( name ) )
                        throw new BuildException("name attribute must not be empty");
                } else if (key.equals("depends")) {
                    depends = value;
                } else if (key.equals("if")) {
                    target.setIf(value);
                } else if (key.equals("unless")) {
                    target.setUnless(value);
                } else if (key.equals("id")) {
                    if (value != null && !value.equals("")) {
                        context.project.addReference(value, target);
                    }
                } else if (key.equals("description")) {
                    target.setDescription(value);
                } else {
                    throw new SAXParseException("Unexpected attribute \"" + key + "\"", context.locator);
                }
            }

            if (name == null) {
                throw new SAXParseException("target element appears without a name attribute",
                                            context.locator);
            }
            
            Hashtable currentTargets = project.getTargets();

            // If the name has already beend defined ( import for example )
            if(currentTargets.containsKey(name)) {
                // Alter the name.
                if( context.currentProjectName != null ) {
                    String newName=context.currentProjectName + "." + name;
                    project.log("Already defined in main or a previous import, define "
                                + name + " as " + newName,
                                Project.MSG_VERBOSE);
                    name=newName;
                } else {
                    project.log("Already defined in main or a previous import, ignore "
                                + name,
                                Project.MSG_VERBOSE);
                    name=null;
                }
            }

            if( name != null ) {
                target.setName(name);
                project.addOrReplaceTarget(name, target);
            }


            project.log("Targets are now: "+ currentTargets ,
                        Project.MSG_VERBOSE);

            // take care of dependencies
            if (depends.length() > 0) {
                target.setDepends(depends);
            }
        }

        /**
         * Handles the start of an element within a target.
         * 
         * @param tag The name of the element being started. 
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         * 
         * @exception SAXParseException if an error occurs when initialising
         *                              the appropriate child handler
         */
        public AntHandler onStartChild(String uri, String name, String qname,
                                       Attributes attrs,
                                       AntXmlContext context)
            throws SAXParseException
        {
            return new ElementHandler(context.currentTarget, null, context.currentTarget);
        }
        public void onEndElement(String uri, String tag, AntXmlContext context) {
            context.currentTarget=context.implicitTarget;
        }
    }

    /**
     * Handler for all project elements ( tasks, data types )
     */
    public static class ElementHandler extends AntHandler {
        /** Containing target, if any. */
        protected Target target;

        /** 
         * Container for the task, if any. If target is 
         * non-<code>null</code>, this must be too.
         */
        protected TaskContainer container;

        /**
         * element created by this handler.
         */
        protected Object element;

        /**
         * Wrapper for the parent element, if any. The wrapper for this 
         * element will be added to this wrapper as a child.
         */
        protected RuntimeConfigurable2 parentWrapper;

        /**
         * Wrapper for this element which takes care of actually configuring
         * the element, if this element is contained within a target. 
         * Otherwise the configuration is performed with the configure method.
         * @see ProjectHelper#configure(Object,Attributes,Project)
         */
        protected RuntimeConfigurable2 wrapper = null;
        
        /**
         * Constructor.
         * 
         * @param parentHandler The handler which should be restored to the 
         *                      parser at the end of the element. 
         *                      Must not be <code>null</code>.
         * 
         * @param container     Container for the element. 
         *                      Must not be <code>null</code>
         * 
         * @param parentWrapper Wrapper for the parent element, if any.
         *                      May be <code>null</code>.
         * 
         * @param target        Target this element is part of.
         *                      Must not be <code>null</code>.
         */
        public ElementHandler(TaskContainer container, RuntimeConfigurable2 parentWrapper, Target target) {
            this.container = container;
            this.parentWrapper = parentWrapper;
            this.target = target;
        }

        /**
         * Initialisation routine called after handler creation
         * with the element name and attributes. This configures
         * the element with its attributes and sets it up with
         * its parent container (if any). Nested elements are then
         * added later as the parser encounters them.
         * 
         * @param tag Name of the element which caused this handler
         *            to be created. Must not be <code>null</code>.
         *            
         * @param attrs Attributes of the element which caused this
         *              handler to be created. Must not be <code>null</code>.
         * 
         * @exception SAXParseException in case of error (not thrown in
         *                              this implementation)
         */
        public void onStartElement(String uri, String tag, String qname,
                                   Attributes attrs,
                                   AntXmlContext context)
            throws SAXParseException
        {
            if (context.project.getDataTypeDefinitions().get(qname) != null) {
                try {
                    element = context.project.createDataType(qname);
                    if (element == null) {
                        throw new BuildException("Unknown data type "+qname);
                    }
                
                    wrapper = new RuntimeConfigurable2(element, qname);
                    wrapper.setAttributes2(attrs);
                    target.addDataType(wrapper);
                } catch (BuildException exc) {
                    throw new SAXParseException(exc.getMessage(), context.locator, exc);
                }
            } else {
                Task task=null;
                try {
                    task = context.project.createTask(qname);
                } catch (BuildException e) {
                    // swallow here, will be thrown again in 
                    // UnknownElement.maybeConfigure if the problem persists.
                }

                if (task == null) {
                    task = new UnknownElement(qname);
                    task.setProject(context.project);
                    //XXX task.setTaskType(qname);
                    task.setTaskName(qname);
                }
                element=task;

                task.setLocation(new Location(context.locator.getSystemId(),
                                              context.locator.getLineNumber(),
                                              context.locator.getColumnNumber()));
                context.configureId(task, attrs);
                
                task.setOwningTarget(target);
                container.addTask(task);
                task.init();

                wrapper=new RuntimeConfigurable2(task, task.getTaskName());
                wrapper.setAttributes2(attrs);
                
                if (parentWrapper != null) {
                    parentWrapper.addChild(wrapper);
                }
            }
        }


        /**
         * Adds text to the task, using the wrapper
         * 
         * @param buf A character array of the text within the element.
         *            Will not be <code>null</code>.
         * @param start The start element in the array.
         * @param count The number of characters to read from the array.
         * 
         * @exception SAXParseException if the element doesn't support text
         * 
         * @see ProjectHelper#addText(Project,Object,char[],int,int)
         */
        public void characters(char[] buf, int start, int count,
                               AntXmlContext context)
            throws SAXParseException
        {
                wrapper.addText(buf, start, count);
        }
        
        /**
         * Handles the start of an element within a target. Task containers
         * will always use another task handler, and all other tasks
         * will always use a nested element handler.
         * 
         * @param tag The name of the element being started. 
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         * 
         * @exception SAXParseException if an error occurs when initialising
         *                              the appropriate child handler
         */
        public AntHandler onStartChild(String uri, String tag, String qname,
                                       Attributes attrs,
                                       AntXmlContext context)
            throws SAXParseException
        {
            if (element instanceof TaskContainer) {
                // task can contain other tasks - no other nested elements possible
                return new ElementHandler((TaskContainer)element, wrapper, target);
            }
            else {
                return new NestedElementHandler(element, wrapper, target);
            }
        }
    }

    /**
     * Handler for all nested properties.
     */
    public static class NestedElementHandler extends ElementHandler {
        /** Parent object (task/data type/etc). */
        private Object parent;

        /**
         * Constructor.
         * 
         * @param parentHandler The handler which should be restored to the 
         *                      parser at the end of the element. 
         *                      Must not be <code>null</code>.
         * 
         * @param parent        Parent of this element (task/data type/etc).
         *                      Must not be <code>null</code>.
         * 
         * @param parentWrapper Wrapper for the parent element, if any.
         *                      Must not be <code>null</code>.
         * 
         * @param target        Target this element is part of.
         *                      Must not be <code>null</code>.
         */
        public NestedElementHandler(Object parent,
                                    RuntimeConfigurable2 parentWrapper,
                                    Target target) {
            super(null, parentWrapper, target);
            if (parent instanceof TaskAdapter) {
                this.parent = ((TaskAdapter) parent).getProxy();
            } else {
                this.parent = parent;
            }
        }

        /**
         * Initialisation routine called after handler creation
         * with the element name and attributes. This configures
         * the element with its attributes and sets it up with
         * its parent container (if any). Nested elements are then
         * added later as the parser encounters them.
         * 
         * @param tag Name of the element which caused this handler
         *            to be created. Must not be <code>null</code>.
         *            
         * @param attrs Attributes of the element which caused this
         *              handler to be created. Must not be <code>null</code>.
         * 
         * @exception SAXParseException in case of error, such as a 
         *            BuildException being thrown during configuration.
         */
        public void onStartElement(String uri, String propType, String qname,
                                   Attributes attrs,
                                   AntXmlContext context)
            throws SAXParseException
        {

            try {
                String elementName = qname.toLowerCase(Locale.US);
                if (parent instanceof UnknownElement) {
                    UnknownElement uc = new UnknownElement(elementName);
                    uc.setProject(context.project);
                    ((UnknownElement) parent).addChild(uc);
                    element = uc;
                } else {
                    Class parentClass = parent.getClass();
                    IntrospectionHelper ih = 
                        IntrospectionHelper.getHelper(parentClass);
                    element = ih.createElement(context.project, parent, elementName);
                }

                context.configureId(element, attrs);

                wrapper = new RuntimeConfigurable2(element, qname);
                wrapper.setAttributes2(attrs);
                parentWrapper.addChild(wrapper);
            } catch (BuildException exc) {
                throw new SAXParseException(exc.getMessage(), context.locator, exc);
            }
        }
    }
}
