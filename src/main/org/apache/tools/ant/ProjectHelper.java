/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999, 2000 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant;

import java.io.*;
import java.util.*;
import org.xml.sax.*;
import org.w3c.dom.*;
import org.apache.tools.ant.taskdefs.*;
import javax.xml.parsers.*;

/**
 * Configures a Project (complete with Targets and Tasks) based on
 * a XML build file.
 *
 * @author duncan@x180.com
 */

public class ProjectHelper {

    private static SAXParserFactory parserFactory = null;

    private org.xml.sax.Parser parser;
    private Project project;
    private File buildFile;
    private File buildFileParent;
    private Locator locator;

    /**
     * Configures the Project with the contents of the specified XML file.
     */
    public static void configureProject(Project project, File buildFile) throws BuildException {
        new ProjectHelper(project, buildFile).parse();
    }

    /**
     * Constructs a new Ant parser for the specified XML file.
     */
    private ProjectHelper(Project project, File buildFile) {
        this.project = project;
        this.buildFile = new File(buildFile.getAbsolutePath());
        buildFileParent = new File(this.buildFile.getParent());
    }

    /**
     * Parses the project file.
     */
    private void parse() throws BuildException {
        FileInputStream inputStream = null;
        InputSource inputSource = null;
        
        try {
            SAXParser saxParser = getParserFactory().newSAXParser();
            parser = saxParser.getParser();

            String uri = "file:" + buildFile.getAbsolutePath().replace('\\', '/');
            for (int index = uri.indexOf('#'); index != -1; index = uri.indexOf('#')) {
                uri = uri.substring(0, index) + "%23" + uri.substring(index+1);
            }
            
            inputStream = new FileInputStream(buildFile);
            inputSource = new InputSource(inputStream);
            inputSource.setSystemId(uri);
            project.log("parsing buildfile " + buildFile + " with URI = " + uri, Project.MSG_VERBOSE);
            saxParser.parse(inputSource, new RootHandler());
        }
        catch(ParserConfigurationException exc) {
            throw new BuildException("Parser has not been configured correctly", exc);
        }
        catch(SAXParseException exc) {
            Location location =
                new Location(buildFile.toString(), exc.getLineNumber(), exc.getColumnNumber());

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
        catch(IOException exc) {
            throw new BuildException("Error reading project file", exc);
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
     * The common superclass for all sax event handlers in Ant. Basically
     * throws an exception in each method, so subclasses should override
     * what they can handle.
     *
     * Each type of xml element (task, target, etc) in ant will
     * have its own subclass of AbstractHandler.
     *
     * In the constructor, this class    takes over the handling of sax
     * events from the parent handler, and returns
     * control back to the parent in the endElement method.
     */
    private class AbstractHandler extends HandlerBase {
        protected DocumentHandler parentHandler;

        public AbstractHandler(DocumentHandler parentHandler) {
            this.parentHandler = parentHandler;

            // Start handling SAX events
            parser.setDocumentHandler(this);
        }

        public void startElement(String tag, AttributeList attrs) throws SAXParseException {
            throw new SAXParseException("Unexpected element \"" + tag + "\"", locator);
        }

        public void characters(char[] buf, int start, int end) throws SAXParseException {
            String s = new String(buf, start, end).trim();

            if (s.length() > 0) {
                throw new SAXParseException("Unexpected text \"" + s + "\"", locator);
            }
        }

        /**
         * Called when this element and all elements nested into it have been
         * handeled.
         */
        protected void finished() {}

        public void endElement(String name) throws SAXException {

            finished();
            // Let parent resume handling SAX events
            parser.setDocumentHandler(parentHandler);
        }
    }

    /**
     * Handler for the root element. It's only child must be the "project" element.
     */
    private class RootHandler extends HandlerBase {

        /**
         * resolve file: URIs as relative to the build file.
         */
        public InputSource resolveEntity(String publicId,
                                         String systemId) {
        
            project.log("resolving systemId: " + systemId, Project.MSG_VERBOSE);
        
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
                    file = new File(buildFileParent, path);
                }
                
                try {
                    InputSource inputSource = new InputSource(new FileInputStream(file));
                    inputSource.setSystemId("file:" + entitySystemId);
                    return inputSource;
                } catch (FileNotFoundException fne) {
                    project.log(file.getAbsolutePath()+" could not be found", 
                                Project.MSG_WARN);
                }
            }
            // use default if not file or file not found
            return null;
        }

        public void startElement(String tag, AttributeList attrs) throws SAXParseException {
            if (tag.equals("project")) {
                new ProjectHandler(this).init(tag, attrs);
            } else {
                throw new SAXParseException("Config file is not of expected XML type", locator);
            }
        }

        public void setDocumentLocator(Locator locator) {
            ProjectHelper.this.locator = locator;
        }
    }

    /**
     * Handler for the top level "project" element.
     */
    private class ProjectHandler extends AbstractHandler {
        public ProjectHandler(DocumentHandler parentHandler) {
            super(parentHandler);
        }

        public void init(String tag, AttributeList attrs) throws SAXParseException {
            String def = null;
            String name = null;
            String id = null;
            String baseDir = null;

            for (int i = 0; i < attrs.getLength(); i++) {
                String key = attrs.getName(i);
                String value = attrs.getValue(i);

                if (key.equals("default")) {
                    def = value;
                } else if (key.equals("name")) {
                    name = value;
                } else if (key.equals("id")) {
                    id = value;
                } else if (key.equals("basedir")) {
                    baseDir = value;
                } else {
                    throw new SAXParseException("Unexpected attribute \"" + attrs.getName(i) + "\"", locator);
                }
            }

            if (def == null) {
                throw new SAXParseException("The default attribute of project is required", 
                                            locator);
            }
            

            project.setDefaultTarget(def);

            if (name != null) {
                project.setName(name);
                project.addReference(name, project);
            }

            if (id != null) project.addReference(id, project);

            if (project.getProperty("basedir") != null) {
                project.setBasedir(project.getProperty("basedir"));
            } else {
                if (baseDir == null) {
                    project.setBasedir(buildFileParent.getAbsolutePath());
                } else {
                    // check whether the user has specified an absolute path
                    if ((new File(baseDir)).isAbsolute()) {
                        project.setBasedir(baseDir);
                    } else {
                        project.setBasedir((new File(buildFileParent, baseDir)).getAbsolutePath());
                    }
                }
            }

        }

        public void startElement(String name, AttributeList attrs) throws SAXParseException {
            if (name.equals("taskdef")) {
                handleTaskdef(name, attrs);
            } else if (name.equals("property")) {
                handleProperty(name, attrs);
            } else if (name.equals("target")) {
                handleTarget(name, attrs);
            } else if (project.getDataTypeDefinitions().get(name) != null) {
                handleDataType(name, attrs);
            } else {
                throw new SAXParseException("Unexpected element \"" + name + "\"", locator);
            }
        }

        private void handleTaskdef(String name, AttributeList attrs) throws SAXParseException {
            (new TaskHandler(this, null)).init(name, attrs);
        }

        private void handleProperty(String name, AttributeList attrs) throws SAXParseException {
            (new TaskHandler(this, null)).init(name, attrs);
        }

        private void handleTarget(String tag, AttributeList attrs) throws SAXParseException {
            new TargetHandler(this).init(tag, attrs);
        }

        private void handleDataType(String name, AttributeList attrs) throws SAXParseException {
            new DataTypeHandler(this).init(name, attrs);
        }

    }

    /**
     * Handler for "target" elements.
     */
    private class TargetHandler extends AbstractHandler {
        private Target target;

        public TargetHandler(DocumentHandler parentHandler) {
            super(parentHandler);
        }

        public void init(String tag, AttributeList attrs) throws SAXParseException {
            String name = null;
            String depends = "";
            String ifCond = null;
            String unlessCond = null;
            String id = null;
            String description = null;

            for (int i = 0; i < attrs.getLength(); i++) {
                String key = attrs.getName(i);
                String value = attrs.getValue(i);

                if (key.equals("name")) {
                    name = value;
                } else if (key.equals("depends")) {
                    depends = value;
                } else if (key.equals("if")) {
                    ifCond = value;
                } else if (key.equals("unless")) {
                    unlessCond = value;
                } else if (key.equals("id")) {
                    id = value;
                } else if (key.equals("description")) {
                    description = value;
                } else {
                    throw new SAXParseException("Unexpected attribute \"" + key + "\"", locator);
                }
            }

            if (name == null) {
                throw new SAXParseException("target element appears without a name attribute", locator);
            }

            target = new Target();
            target.setName(name);
            target.setIf(ifCond);
            target.setUnless(unlessCond);
            target.setDescription(description);
            project.addTarget(name, target);

            if (id != null && !id.equals(""))
                project.addReference(id, target);

            // take care of dependencies

            if (depends.length() > 0) {
                StringTokenizer tok =
                    new StringTokenizer(depends, ",", false);
                while (tok.hasMoreTokens()) {
                    target.addDependency(tok.nextToken().trim());
                }
            }
        }

        public void startElement(String name, AttributeList attrs) throws SAXParseException {
            new TaskHandler(this, target).init(name, attrs);
        }
    }

    /**
     * Handler for all task elements.
     */
    private class TaskHandler extends AbstractHandler {
        private Target target;
        private Task task;
        private RuntimeConfigurable wrapper = null;

        public TaskHandler(DocumentHandler parentHandler, Target target) {
            super(parentHandler);

            this.target = target;
        }

        public void init(String tag, AttributeList attrs) throws SAXParseException {
            try {
                task = project.createTask(tag);
            } catch (BuildException e) {
                // swallow here, will be thrown again in 
                // UnknownElement.maybeConfigure if the problem persists.
            }

            if (task == null) {
                task = new UnknownElement(tag);
                task.setProject(project);
            }

            task.setLocation(new Location(buildFile.toString(), locator.getLineNumber(), locator.getColumnNumber()));
            configureId(task, attrs);

            // Top level tasks don't have associated targets
            if (target != null) {
                task.setOwningTarget(target);
                target.addTask(task);
                task.init();
                wrapper = task.getRuntimeConfigurableWrapper();
                wrapper.setAttributes(attrs);
            } else {
                task.init();
                configure(task, attrs, project);
            }
        }

        protected void finished() {
            if (task != null && target == null) {
                task.execute();
            }
        }

        public void characters(char[] buf, int start, int end) throws SAXParseException {
            if (wrapper == null) {
                try {
                    addText(task, buf, start, end);
                } catch (BuildException exc) {
                    throw new SAXParseException(exc.getMessage(), locator, exc);
                }
            } else {
                wrapper.addText(buf, start, end);
            }
        }

        public void startElement(String name, AttributeList attrs) throws SAXParseException {
            new NestedElementHandler(this, task, wrapper).init(name, attrs);
        }
    }

    /**
     * Handler for all nested properties.
     */
    private class NestedElementHandler extends AbstractHandler {
        private Object target;
        private Object child;
        private RuntimeConfigurable parentWrapper;
        private RuntimeConfigurable childWrapper = null;

        public NestedElementHandler(DocumentHandler parentHandler, 
                                    Object target,
                                    RuntimeConfigurable parentWrapper) {
            super(parentHandler);

            if (target instanceof TaskAdapter) {
                this.target = ((TaskAdapter) target).getProxy();
            } else {
                this.target = target;
            }
            this.parentWrapper = parentWrapper;
        }

        public void init(String propType, AttributeList attrs) throws SAXParseException {
            Class targetClass = target.getClass();
            IntrospectionHelper ih = 
                IntrospectionHelper.getHelper(targetClass);

            try {
                if (target instanceof UnknownElement) {
                    child = new UnknownElement(propType.toLowerCase());
                    ((UnknownElement) target).addChild((UnknownElement) child);
                } else {
                    child = ih.createElement(target, propType.toLowerCase());
                }

                configureId(child, attrs);

                if (parentWrapper != null) {
                    childWrapper = new RuntimeConfigurable(child);
                    childWrapper.setAttributes(attrs);
                    parentWrapper.addChild(childWrapper);
                } else {
                    configure(child, attrs, project);
                }
            } catch (BuildException exc) {
                throw new SAXParseException(exc.getMessage(), locator, exc);
            }
        }

        public void characters(char[] buf, int start, int end) throws SAXParseException {
            if (parentWrapper == null) {
                try {
                    addText(child, buf, start, end);
                } catch (BuildException exc) {
                    throw new SAXParseException(exc.getMessage(), locator, exc);
                }
            } else {
                childWrapper.addText(buf, start, end);
            }
        }

        public void startElement(String name, AttributeList attrs) throws SAXParseException {
            new NestedElementHandler(this, child, childWrapper).init(name, attrs);
        }
    }

    /**
     * Handler for all data types at global level.
     */
    private class DataTypeHandler extends AbstractHandler {
        private Object element;

        public DataTypeHandler(DocumentHandler parentHandler) {
            super(parentHandler);
        }

        public void init(String propType, AttributeList attrs) throws SAXParseException {
            try {
                element = project.createDataType(propType);
                if (element == null) {
                    throw new BuildException("Unknown data type "+propType);
                }
                
                configureId(element, attrs);
                configure(element, attrs, project);
            } catch (BuildException exc) {
                throw new SAXParseException(exc.getMessage(), locator, exc);
            }
        }

        public void characters(char[] buf, int start, int end) throws SAXParseException {
            try {
                addText(element, buf, start, end);
            } catch (BuildException exc) {
                throw new SAXParseException(exc.getMessage(), locator, exc);
            }
        }

        public void startElement(String name, AttributeList attrs) throws SAXParseException {
            new NestedElementHandler(this, element, null).init(name, attrs);
        }
    }

    public static void configure(Object target, AttributeList attrs, 
                                 Project project) throws BuildException {
        if( target instanceof TaskAdapter )
            target=((TaskAdapter)target).getProxy();

        IntrospectionHelper ih = 
            IntrospectionHelper.getHelper(target.getClass());

        for (int i = 0; i < attrs.getLength(); i++) {
            // reflect these into the target
            String value=replaceProperties(project, attrs.getValue(i), 
                                           project.getProperties() );
            try {
                ih.setAttribute(project, target, 
                                attrs.getName(i).toLowerCase(), value);

            } catch (BuildException be) {
                // id attribute must be set externally
                if (!attrs.getName(i).equals("id")) {
                    throw be;
                }
            }
        }
    }

    /**
     * Adds the content of #PCDATA sections to an element.
     */
    public static void addText(Object target, char[] buf, int start, int end)
        throws BuildException {
        addText(target, new String(buf, start, end));
    }

    /**
     * Adds the content of #PCDATA sections to an element.
     */
    public static void addText(Object target, String text)
        throws BuildException {

        if (text == null || text.trim().length() == 0) {
            return;
        }

        if(target instanceof TaskAdapter)
            target = ((TaskAdapter) target).getProxy();

        IntrospectionHelper.getHelper(target.getClass()).addText(target, text);
    }


    /** Replace ${NAME} with the property value
     */
    public static String replaceProperties(Project project, String value, Hashtable keys )
        throws BuildException
    {
        // XXX use Map instead of proj, it's too heavy

        // XXX need to replace this code with something better.
        StringBuffer sb=new StringBuffer();
        int i=0;
        int prev=0;
        // assert value!=nil
        int pos;
        while( (pos=value.indexOf( "$", prev )) >= 0 ) {
            if(pos>0) {
                sb.append( value.substring( prev, pos ) );
            }
            if( pos == (value.length() - 1)) {
                sb.append('$');
                prev = pos + 1;
            }
            else if (value.charAt( pos + 1 ) != '{' ) {
                sb.append( value.charAt( pos + 1 ) );
                prev=pos+2; // XXX
            } else {
                int endName=value.indexOf( '}', pos );
                if( endName < 0 ) {
                    throw new BuildException("Syntax error in prop: " +
                                             value );
                }
                String n=value.substring( pos+2, endName );
                if (!keys.containsKey(n)) {
                    project.log("Property ${" + n + "} has not been set", Project.MSG_VERBOSE);
                }
                
                String v = (keys.containsKey(n)) ? (String) keys.get(n) : "${"+n+"}"; 
                
                //System.out.println("N: " + n + " " + " V:" + v);
                sb.append( v );
                prev=endName+1;
            }
        }
        if( prev < value.length() ) sb.append( value.substring( prev ) );
        //      System.out.println("After replace: " + sb.toString());
        // System.out.println("Before replace: " + value);
        return sb.toString();
    }

    private static SAXParserFactory getParserFactory() {
        if (parserFactory == null) {
            parserFactory = SAXParserFactory.newInstance();
        }

        return parserFactory;
    }

    /**
     * Scan AttributeList for the id attribute and maybe add a
     * reference to project.  
     *
     * <p>Moved out of {@link #configure configure} to make it happen
     * at parser time.</p> 
     */
    private void configureId(Object target, AttributeList attr) {
        String id = attr.getValue("id");
        if (id != null) {
            project.addReference(id, target);
        }
    }

}
