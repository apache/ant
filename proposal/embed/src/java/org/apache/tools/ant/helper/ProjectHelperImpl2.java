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

    // singletons - since all state is in the context
    static AntHandler elementHandler=new ElementHandler();
    static AntHandler targetHandler=new TargetHandler();
    static AntHandler nestedElementHandler=new NestedElementHandler();
    static AntHandler mainHandler=new MainHandler();
    static AntHandler projectHandler=new ProjectHandler();
    
    /** Method to add several 'special' tasks that are specific
     *  to this helper. In future we could use the properties file
     */
    private void hookSpecialTasks(Project project) {
        try {
            Class c=Class.forName("org.apache.tools.ant.taskdefs.SystemPath");
            project.addTaskDefinition( "systemPath" , c );
            c=Class.forName("org.apache.tools.ant.taskdefs.Import");
            project.addTaskDefinition( "import" , c );
            c=Class.forName("org.apache.tools.ant.taskdefs.Taskdef2");
            project.addTaskDefinition( "taskdef" , c );
        } catch (Exception ex ) {
            ex.printStackTrace();
        }
    }
    

    public void parse(Project project, Object source) throws BuildException {
        hookSpecialTasks(project);
        AntXmlContext context=new AntXmlContext(project, this);
        
        project.addReference( "ant.parsing.context", context );

        parse(project, source,new RootHandler(context));

        // XXX How to deal with description ??
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

        System.out.println("Parsing with PH2: " + source );
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
     * the configuration file. 
     *
     * The context will hold all state information. At each time
     * there is one active handler for the current element. It can
     * use onStartChild() to set an alternate handler for the child.
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

        public void onEndChild(String uri, String tag, String qname,
                                     AntXmlContext context)
            throws SAXParseException
        {
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

    /** Context information for the ant processing.
     */
    public static class AntXmlContext {
        /** The project to configure. */
        private Project project;

        /** The configuration file to parse. */
        public File buildFile;

        /** 
         * Parent directory of the build file. Used for resolving entities
         * and setting the project's base directory.
         */
        public File buildFileParent;

        /** Name of the current project */
        public String currentProjectName;

        /** 
         * Locator for the configuration file parser. 
         * Used for giving locations of errors etc.
         */
        Locator locator;

        // Do we need those ?
        public ProjectHelperImpl2 helper;
        org.xml.sax.XMLReader parser;

         /**
          * Target that all other targets will depend upon implicitly.
          *
          * <p>This holds all tasks and data type definitions that have
          * been placed outside of targets.</p>
          */
        Target implicitTarget = new Target();

        /** Current target ( no need for a stack as the processing model
            allows only one level of target ) */
        public Target currentTarget=null;

        /** The stack of RuntimeConfigurable2 wrapping the
            objects. 
        */
        Vector wStack=new Vector();
        
        // Import stuff
        public boolean ignoreProjectTag=false;
        public Hashtable importedFiles = new Hashtable();
        public int importlevel = 0;

        public AntXmlContext(Project project, ProjectHelperImpl2 helper) {
            this.project=project;
            implicitTarget.setName("");
            this.helper=helper;
        }

        public Project getProject() {
            return project;
        }

        public RuntimeConfigurable2 currentWrapper() {
            if( wStack.size() < 1 ) return null;
            return (RuntimeConfigurable2)wStack.elementAt( wStack.size() - 1 );
        }

        public RuntimeConfigurable2 parentWrapper() {
            if( wStack.size() < 2 ) return null;
            return (RuntimeConfigurable2)wStack.elementAt( wStack.size() - 2 );
        }

        public void pushWrapper( RuntimeConfigurable2 wrapper ) {
            wStack.addElement(wrapper);
        }

        public void popWrapper() {
            if( wStack.size() > 0 ) 
                wStack.removeElementAt( wStack.size() - 1 );
        }

        public Vector getWrapperStack() {
            return wStack;
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
        void configureId(Object element, Attributes attr) {
            String id = attr.getValue("id");
            if (id != null) {
                project.addReference(id, element);
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
            currentHandler=ProjectHelperImpl2.mainHandler;
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
        
            context.getProject().log("resolving systemId: " + systemId, Project.MSG_VERBOSE);
        
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
                    context.getProject().log(file.getAbsolutePath()+" could not be found", 
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
            if( currentHandler!=null )
                currentHandler.onEndChild( uri, name, qName, context );
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
                return ProjectHelperImpl2.projectHandler;
            } else {
//                 if( context.importlevel > 0 ) {
//                     // we are in an imported file. Allow top-level <target>.
//                     if( qname.equals( "target" ) )
//                         return ProjectHelperImpl2.targetHandler;
//                 }
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

            Project project=context.getProject();

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
                return ProjectHelperImpl2.targetHandler;
            } else {
                return ProjectHelperImpl2.elementHandler;
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

            Project project=context.getProject();
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
                        context.getProject().addReference(value, target);
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
            return ProjectHelperImpl2.elementHandler;
        }
        public void onEndElement(String uri, String tag, AntXmlContext context) {
            context.currentTarget=context.implicitTarget;
        }
    }

    /**
     * Handler for all project elements ( tasks, data types )
     */
    public static class ElementHandler extends AntHandler {

        /**
         * Constructor.
         */
        public ElementHandler() {
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
            RuntimeConfigurable2 parentWrapper=context.currentWrapper();
            RuntimeConfigurable2 wrapper=null;
            
            if (context.getProject().getDataTypeDefinitions().get(qname) != null) {
                try {
                    Object element = context.getProject().createDataType(qname);
                    if (element == null) {
                        // can it happen ? We just checked that the type exists
                        throw new BuildException("Unknown data type "+qname);
                    }
                
                    wrapper = new RuntimeConfigurable2(element, qname);
                    wrapper.setAttributes2(attrs);
                    context.currentTarget.addDataType(wrapper);
                } catch (BuildException exc) {
                    throw new SAXParseException(exc.getMessage(), context.locator, exc);
                }
            } else {
                Task task=null;
                try {
                    task = context.getProject().createTask(qname);
                } catch (BuildException e) {
                    // swallow here, will be thrown again in 
                    // UnknownElement.maybeConfigure if the problem persists.
                }

                if (task == null) {
                    task = new UnknownElement(qname);
                    task.setProject(context.getProject());
                    //XXX task.setTaskType(qname);
                    task.setTaskName(qname);
                }

                task.setLocation(new Location(context.locator.getSystemId(),
                                              context.locator.getLineNumber(),
                                              context.locator.getColumnNumber()));
                context.configureId(task, attrs);
                
                task.setOwningTarget(context.currentTarget);

                Object parent=null;
                if( parentWrapper!=null ) {
                    parent=parentWrapper.getProxy();
                }

                if( parent instanceof TaskContainer ) {
                    // Task included in a TaskContainer
                    ((TaskContainer)parent).addTask( task );
                } else {
                    // Task included in a target ( including the default one ).
                    context.currentTarget.addTask( task );
                }
                // container.addTask(task);
                task.init();

                wrapper=new RuntimeConfigurable2(task, task.getTaskName());
                wrapper.setAttributes2(attrs);

                if (parentWrapper != null) {
                    parentWrapper.addChild(wrapper);
                }
            }

            context.pushWrapper( wrapper );
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
            RuntimeConfigurable2 wrapper=context.currentWrapper();
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
            // this element
            RuntimeConfigurable2 wrapper=context.currentWrapper();
            
            Object element=wrapper.getProxy();
            if (element instanceof TaskContainer) {
                // task can contain other tasks - no other nested elements possible
                return ProjectHelperImpl2.elementHandler;
            }
            else {
                return ProjectHelperImpl2.nestedElementHandler;
            }
        }

        public void onEndElement(String uri, String tag, AntXmlContext context) {
            context.popWrapper();
        }

        public void onEndChild(String uri, String tag, String qname,
                                     AntXmlContext context)
            throws SAXParseException
        {
        }
    }

    /**
     * Handler for all nested properties. Same as ElementHandler, except that
     * it doesn't deal with DataTypes and doesn't support TaskContainer.
     *
     * This is the original behavior - I just made few changes to avoid duplicated
     * code.
     */
    public static class NestedElementHandler extends ElementHandler {
        /**
         * Constructor.
         */
        public NestedElementHandler() {
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
            RuntimeConfigurable2 parentWrapper=context.currentWrapper();
            RuntimeConfigurable2 wrapper=null;
            try {
                Object element;
                Object parent=parentWrapper.getProxy();
                if (parent instanceof TaskAdapter) {
                    parent = ((TaskAdapter) parent).getProxy();
                }
                
                String elementName = qname.toLowerCase(Locale.US);
                if (parent instanceof UnknownElement) {
                    UnknownElement uc = new UnknownElement(elementName);
                    uc.setProject(context.getProject());
                    ((UnknownElement) parent).addChild(uc);
                    element = uc;
                } else {
                    Class parentClass = parent.getClass();
                    IntrospectionHelper ih = 
                        IntrospectionHelper.getHelper(parentClass);
                    element = ih.createElement(context.getProject(), parent, elementName);
                }

                context.configureId(element, attrs);

                wrapper = new RuntimeConfigurable2(element, qname);
                wrapper.setAttributes2(attrs);
                parentWrapper.addChild(wrapper);
            } catch (BuildException exc) {
                throw new SAXParseException(exc.getMessage(), context.locator, exc);
            }
            context.pushWrapper( wrapper );
        }
    }
}
