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
        this.buildFile = buildFile;
    }

    /**
     * Parses the project file.
     */
    private void parse() throws BuildException {
        try {
            parser = getParserFactory().newSAXParser().getParser();
            parser.setDocumentHandler(new RootHandler());
            parser.parse(new InputSource(new FileReader(buildFile)));
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
            throw new BuildException("File \"" + buildFile.toString() + "\" not found");
        }
        catch(IOException exc) {
            throw new BuildException("Error reading project file", exc);
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

        public void endElement(String name) throws SAXException {

            // Let parent resume handling SAX events
            parser.setDocumentHandler(parentHandler);
        }
    }

    /**
     * Handler for the root element. It's only child must be the "project" element.
     */
    private class RootHandler extends HandlerBase {
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
            String baseDir = new File(buildFile.getAbsolutePath()).getParent();

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

            project.setDefaultTarget(def);

            project.setName(name);
            if (name != null) project.addReference(name, project);

            if (id != null) project.addReference(id, project);

            if (project.getProperty("basedir") != null) {
                project.setBasedir(project.getProperty("basedir"));
            } else {
                project.setBasedir(baseDir);
            }

        }

        public void startElement(String name, AttributeList attrs) throws SAXParseException {
            if (name.equals("taskdef")) {
                handleTaskdef(name, attrs);
            } else if (name.equals("property")) {
                handleProperty(name, attrs);
            } else if (name.equals("target")) {
                handleTarget(name, attrs);
            } else {
                throw new SAXParseException("Unexpected element \"" + name + "\"", locator);
            }
        }

        private void handleTaskdef(String name, AttributeList attrs) throws SAXParseException {
            new TaskHandler(this, null).init(name, attrs);
        }

        private void handleProperty(String name, AttributeList attrs) throws SAXParseException {
            new TaskHandler(this, null).init(name, attrs);
        }

        private void handleTarget(String tag, AttributeList attrs) throws SAXParseException {
            new TargetHandler(this).init(tag, attrs);
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

        public TaskHandler(DocumentHandler parentHandler, Target target) {
            super(parentHandler);

            this.target = target;
        }

        public void init(String tag, AttributeList attrs) throws SAXParseException {
            task = project.createTask(tag);
            configure(task, attrs);
            task.setLocation(new Location(buildFile.toString(), locator.getLineNumber(), locator.getColumnNumber()));
            task.init();

            // Top level tasks don't have associated targets
            if (target != null) {
                task.setOwningTarget(target);
                target.addTask(task);
            }
        }

        public void characters(char[] buf, int start, int end) throws SAXParseException {
            String text = new String(buf, start, end).trim();
            if (text.length() == 0) return;

            IntrospectionHelper ih = 
                IntrospectionHelper.getHelper(task.getClass());

            try {
                ih.addText(task, text);
            } catch (BuildException exc) {
                throw new SAXParseException(exc.getMessage(), locator, exc);
            }
        }

        public void startElement(String name, AttributeList attrs) throws SAXParseException {
            new NestedElementHandler(this, task).init(name, attrs);
        }
    }

    /**
     * Handler for all nested properties.
     */
    private class NestedElementHandler extends AbstractHandler {
        private DocumentHandler parentHandler;

        private Object target;
        private Object child;

        public NestedElementHandler(DocumentHandler parentHandler, Object target) {
            super(parentHandler);

            this.target = target;
        }

        public void init(String propType, AttributeList attrs) throws SAXParseException {
            Class targetClass = target.getClass();
            IntrospectionHelper ih = 
                IntrospectionHelper.getHelper(targetClass);

            try {
                child = ih.createElement(target, propType.toLowerCase());
                configure(child, attrs);
            } catch (BuildException exc) {
                throw new SAXParseException(exc.getMessage(), locator, exc);
            }
        }

        public void characters(char[] buf, int start, int end) throws SAXParseException {
            String text = new String(buf, start, end).trim();
            if (text.length() == 0) return;

            IntrospectionHelper ih = 
                IntrospectionHelper.getHelper(child.getClass());

            try {
                ih.addText(child, text);
            } catch (BuildException exc) {
                throw new SAXParseException(exc.getMessage(), locator, exc);
            }
        }

        public void startElement(String name, AttributeList attrs) throws SAXParseException {
            new NestedElementHandler(this, child).init(name, attrs);
        }
    }

    private void configure(Object target, AttributeList attrs) throws BuildException {
        if( target instanceof TaskAdapter )
            target=((TaskAdapter)target).getProxy();

        IntrospectionHelper ih = 
            IntrospectionHelper.getHelper(target.getClass());

        for (int i = 0; i < attrs.getLength(); i++) {
            // reflect these into the target
            String value=replaceProperties(attrs.getValue(i), 
                                           project.getProperties() );
            try {
                ih.setAttribute(project, target, 
                                attrs.getName(i).toLowerCase(), value);

            } catch (BuildException be) {
                if (attrs.getName(i).equals("id")) {
                    project.addReference(attrs.getValue(i), target);
                } else {
                    be.setLocation(new Location(buildFile.toString(), 
                                                locator.getLineNumber(), 
                                                locator.getColumnNumber()));
                    throw be;
                }
            }
        }
    }


    /** Replace ${NAME} with the property value
     */
    public static String replaceProperties( String value, Hashtable keys )
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
                String v= (keys.containsKey(n)) ? (String) keys.get( n ) 
                    : "${"+n+"}";
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
}
