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

package org.apache.tools.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Locale;
import org.xml.sax.Locator;
import org.xml.sax.InputSource;
import org.xml.sax.HandlerBase;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;
import org.xml.sax.DocumentHandler;
import org.xml.sax.AttributeList;
import org.xml.sax.helpers.XMLReaderAdapter;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;

// Note: Use of local classes can create problems with various compilers
// like gcc or even jikes. In addition it makes the code harder to read
// for many beginners ( at least for me - Costin ).
// I changed the code to avoid the tricks that the compiler does, now
// jikes works ( for me ). Note that declaring the classes 'private'
// is probably overriden by the compiler - a feature of the internal class impl.

/**
 * "Original" implementation of the project helper. Or at least
 * what is present in ant1.4.
 *
 * @author duncan@x180.com
 */
public class ProjectHelperImpl extends ProjectHelper {
    private static SAXParserFactory parserFactory = null;

    protected Project project;
    protected Object source;
    protected File buildFile;
    protected File buildFileParent;
    private org.xml.sax.Parser parser;
    private Locator locator;

    /** Return a handler for project. This can be used by xml helpers to fallback to
        the original behavior ( non-namespace aware ).
        When the <project> callback is received, if no namespaces are used ( or no
        new attributes, etc ) then the easiest way to achieve backward compatibility
        is to use the original.

        A helper needs to call this method, which will switch the HandlerBase in
        the SAX parser and return it to the original on </project>

        @experimental This is likely to change
    */
    public HandlerBase defaultProjectHandler( Project project,
                                              org.xml.sax.Parser parser,
                                              String tag, AttributeList attrs,
                                              DocumentHandler parent )
        throws SAXParseException
    {
        this.project=project;
        this.parser=parser;
        ProjectHandler h=new ProjectHandler(this, parent);
        h.init( tag, attrs );
        return h;
    }

    /**
     * Parses the project file.
     */
    public void parse(Project project, Object source) throws BuildException {
        if( ! (source instanceof File) )
            throw new BuildException( "Only File source is supported by the default helper");
        
        File buildFile=(File)source;
        this.project = project;
        this.buildFile = new File(buildFile.getAbsolutePath());
        buildFileParent = new File(this.buildFile.getParent());
        
        FileInputStream inputStream = null;
        InputSource inputSource = null;
        
        try {
            SAXParser saxParser = getParserFactory().newSAXParser();
            try {
                parser = saxParser.getParser();
            } catch (SAXException exc) {
                parser = new XMLReaderAdapter(saxParser.getXMLReader());
            }

            String uri = "file:" + buildFile.getAbsolutePath().replace('\\', '/');
            for (int index = uri.indexOf('#'); index != -1; index = uri.indexOf('#')) {
                uri = uri.substring(0, index) + "%23" + uri.substring(index+1);
            }
            
            inputStream = new FileInputStream(buildFile);
            inputSource = new InputSource(inputStream);
            inputSource.setSystemId(uri);
            project.log("parsing buildfile " + buildFile + " with URI = " + uri, Project.MSG_VERBOSE);

            HandlerBase hb = new RootHandler(this);
            parser.setDocumentHandler(hb);
            parser.setEntityResolver(hb);
            parser.setErrorHandler(hb);
            parser.setDTDHandler(hb);
            parser.parse(inputSource);
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
    private static class AbstractHandler extends HandlerBase {
        protected DocumentHandler parentHandler;
        protected ProjectHelperImpl helper;

        public AbstractHandler(ProjectHelperImpl helper, DocumentHandler parentHandler) {
            this.parentHandler = parentHandler;
            this.helper=helper;

            // Start handling SAX events
            helper.parser.setDocumentHandler(this);
        }

        public void startElement(String tag, AttributeList attrs) throws SAXParseException {
            throw new SAXParseException("Unexpected element \"" + tag + "\"", helper.locator);
        }

        public void characters(char[] buf, int start, int end) throws SAXParseException {
            String s = new String(buf, start, end).trim();

            if (s.length() > 0) {
                throw new SAXParseException("Unexpected text \"" + s + "\"", helper.locator);
            }
        }

        /**
         * Called when this element and all elements nested into it have been
         * handled.
         */
        protected void finished() {}

        public void endElement(String name) throws SAXException {

            finished();
            // Let parent resume handling SAX events
            helper.parser.setDocumentHandler(parentHandler);
        }
    }

    /**
     * Handler for the root element. It's only child must be the "project" element.
     */
    private static class RootHandler extends HandlerBase {
        private ProjectHelperImpl helper;

        public RootHandler( ProjectHelperImpl helper ) {
            this.helper=helper;
        }
        
        /**
         * resolve file: URIs as relative to the build file.
         */
        public InputSource resolveEntity(String publicId,
                                         String systemId) {
        
            helper.project.log("resolving systemId: " + systemId, Project.MSG_VERBOSE);
        
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
                    file = new File(helper.buildFileParent, path);
                }
                
                try {
                    InputSource inputSource = new InputSource(new FileInputStream(file));
                    inputSource.setSystemId("file:" + entitySystemId);
                    return inputSource;
                } catch (FileNotFoundException fne) {
                    helper.project.log(file.getAbsolutePath()+" could not be found", 
                                Project.MSG_WARN);
                }
            }
            // use default if not file or file not found
            return null;
        }

        public void startElement(String tag, AttributeList attrs) throws SAXParseException {
            if (tag.equals("project")) {
                new ProjectHandler(helper, this).init(tag, attrs);
            } else {
                throw new SAXParseException("Config file is not of expected XML type", helper.locator);
            }
        }

        public void setDocumentLocator(Locator locator) {
            helper.locator = locator;
        }
    }

    /**
     * Handler for the top level "project" element.
     */
    private static class ProjectHandler extends AbstractHandler {
        public ProjectHandler(ProjectHelperImpl helper, DocumentHandler parentHandler) {
            super(helper, parentHandler);
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
                    throw new SAXParseException("Unexpected attribute \"" + attrs.getName(i) + "\"", helper.locator);
                }
            }

            if (def == null) {
                throw new SAXParseException("The default attribute of project is required", 
                                            helper.locator);
            }
            

            helper.project.setDefaultTarget(def);

            if (name != null) {
                helper.project.setName(name);
                helper.project.addReference(name, helper.project);
            }

            if (id != null) {
              helper.project.addReference(id, helper.project);
            }

            if (helper.project.getProperty("basedir") != null) {
                helper.project.setBasedir(helper.project.getProperty("basedir"));
            } else {
                if (baseDir == null) {
                    helper.project.setBasedir(helper.buildFileParent.getAbsolutePath());
                } else {
                    // check whether the user has specified an absolute path
                    if ((new File(baseDir)).isAbsolute()) {
                        helper.project.setBasedir(baseDir);
                    } else {
                        helper.project.setBaseDir(helper.project.resolveFile(baseDir, helper.buildFileParent));
                    }
                }
            }

        }

        public void startElement(String name, AttributeList attrs) throws SAXParseException {
            if (name.equals("taskdef")) {
                handleTaskdef(name, attrs);
            } else if (name.equals("typedef")) {
                handleTypedef(name, attrs);
            } else if (name.equals("property")) {
                handleProperty(name, attrs);
            } else if (name.equals("target")) {
                handleTarget(name, attrs);
            } else if (helper.project.getDataTypeDefinitions().get(name) != null) {
                handleDataType(name, attrs);
            } else {
                throw new SAXParseException("Unexpected element \"" + name + "\"", helper.locator);
            }
        }

        private void handleTaskdef(String name, AttributeList attrs) throws SAXParseException {
            (new TaskHandler(helper, this, null, null, null)).init(name, attrs);
        }

        private void handleTypedef(String name, AttributeList attrs) throws SAXParseException {
            (new TaskHandler(helper, this, null, null, null)).init(name, attrs);
        }

        private void handleProperty(String name, AttributeList attrs) throws SAXParseException {
            (new TaskHandler(helper, this, null, null, null)).init(name, attrs);
        }

        private void handleTarget(String tag, AttributeList attrs) throws SAXParseException {
            new TargetHandler(helper, this).init(tag, attrs);
        }

        private void handleDataType(String name, AttributeList attrs) throws SAXParseException {
            new DataTypeHandler(helper, this).init(name, attrs);
        }

    }

    /**
     * Handler for "target" elements.
     */
    private static class TargetHandler extends AbstractHandler {
        private Target target;

        public TargetHandler(ProjectHelperImpl helper, DocumentHandler parentHandler) {
            super(helper, parentHandler);
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
                    throw new SAXParseException("Unexpected attribute \"" + key + "\"", helper.locator);
                }
            }

            if (name == null) {
                throw new SAXParseException("target element appears without a name attribute", helper.locator);
            }

            target = new Target();
            target.setName(name);
            target.setIf(ifCond);
            target.setUnless(unlessCond);
            target.setDescription(description);
            helper.project.addTarget(name, target);

            if (id != null && !id.equals("")) {
                helper.project.addReference(id, target);
            }

            // take care of dependencies

            if (depends.length() > 0) {
                target.setDepends(depends);
            }
        }

        public void startElement(String name, AttributeList attrs) throws SAXParseException {
            if (helper.project.getDataTypeDefinitions().get(name) != null) {
                new DataTypeHandler(helper, this, target).init(name, attrs);
            } else {
                new TaskHandler(helper, this, target, null, target).init(name, attrs);
            }
        }
    }

    /**
     * Handler for all task elements.
     */
    private static class TaskHandler extends AbstractHandler {
        private Target target;
        private TaskContainer container;
        private Task task;
        private RuntimeConfigurable parentWrapper;
        private RuntimeConfigurable wrapper = null;

        public TaskHandler(ProjectHelperImpl helper,
                           DocumentHandler parentHandler,
                           TaskContainer container,
                           RuntimeConfigurable parentWrapper,
                           Target target)
        {
            super(helper, parentHandler);
            this.container = container;
            this.parentWrapper = parentWrapper;
            this.target = target;
        }

        public void init(String tag, AttributeList attrs) throws SAXParseException {
            try {
                task = helper.project.createTask(tag);
            } catch (BuildException e) {
                // swallow here, will be thrown again in 
                // UnknownElement.maybeConfigure if the problem persists.
            }

            if (task == null) {
                task = new UnknownElement(tag);
                task.setProject(helper.project);
                task.setTaskType(tag);
                task.setTaskName(tag);
            }

            task.setLocation(new Location(helper.buildFile.toString(),
                                          helper.locator.getLineNumber(),
                                          helper.locator.getColumnNumber()));
            helper.configureId(task, attrs);

            // Top level tasks don't have associated targets
            if (target != null) {
                task.setOwningTarget(target);
                container.addTask(task);
                task.init();
                wrapper = task.getRuntimeConfigurableWrapper();
                wrapper.setAttributes(attrs);
                if (parentWrapper != null) {
                    parentWrapper.addChild(wrapper);
                }
            } else {
                task.init();
                configure(task, attrs, helper.project);
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
                    addText(helper.project, task, buf, start, end);
                } catch (BuildException exc) {
                    throw new SAXParseException(exc.getMessage(), helper.locator, exc);
                }
            } else {
                wrapper.addText(buf, start, end);
            }
        }

        public void startElement(String name, AttributeList attrs) throws SAXParseException {
            if (task instanceof TaskContainer) {
                // task can contain other tasks - no other nested elements possible
                new TaskHandler(helper, this, (TaskContainer)task, wrapper, target).init(name, attrs);
            }
            else {
                new NestedElementHandler(helper, this, task, wrapper, target).init(name, attrs);
            }
        }
    }

    /**
     * Handler for all nested properties.
     */
    private static class NestedElementHandler extends AbstractHandler {
        private Object parent;
        private Object child;
        private RuntimeConfigurable parentWrapper;
        private RuntimeConfigurable childWrapper = null;
        private TaskAdapter adapter=null;
        private Target target;

        public NestedElementHandler(ProjectHelperImpl helper,
                                    DocumentHandler parentHandler, 
                                    Object parent,
                                    RuntimeConfigurable parentWrapper,
                                    Target target) {
            super(helper, parentHandler);

            if (parent instanceof TaskAdapter) {
                this.adapter= (TaskAdapter)parent;
                this.parent = adapter.getProxy();
            } else {
                this.parent = parent;
            }
            this.parentWrapper = parentWrapper;
            this.target = target;
        }

        public void init(String propType, AttributeList attrs) throws SAXParseException {
            Class parentClass = parent.getClass();
            IntrospectionHelper ih = 
                IntrospectionHelper.getHelper(parentClass);
            if( adapter!=null ) {
                adapter.setIntrospectionHelper( ih );
            }
            
            try {
                String elementName = propType.toLowerCase(Locale.US);
                if (parent instanceof UnknownElement) {
                    UnknownElement uc = new UnknownElement(elementName);
                    uc.setProject(helper.project);
                    ((UnknownElement) parent).addChild(uc);
                    child = uc;
                } else {
                    child = ih.createElement(helper.project, parent, elementName);
                }

                helper.configureId(child, attrs);

                if (parentWrapper != null) {
                    childWrapper = new RuntimeConfigurable(child, propType);
                    childWrapper.setAttributes(attrs);
                    parentWrapper.addChild(childWrapper);
                } else {
                    configure(child, attrs, helper.project);
                    ih.storeElement(helper.project, parent, child, elementName);
                }
            } catch (BuildException exc) {
                throw new SAXParseException(exc.getMessage(), helper.locator, exc);
            }
        }

        public void characters(char[] buf, int start, int end) throws SAXParseException {
            if (parentWrapper == null) {
                try {
                    addText(helper.project, child, buf, start, end);
                } catch (BuildException exc) {
                    throw new SAXParseException(exc.getMessage(), helper.locator, exc);
                }
            } else {
                childWrapper.addText(buf, start, end);
            }
        }

        public void startElement(String name, AttributeList attrs) throws SAXParseException {
            if (child instanceof TaskContainer) {
                // taskcontainer nested element can contain other tasks - no other 
                // nested elements possible
                new TaskHandler(helper, this, (TaskContainer)child, childWrapper, target).init(name, attrs);
            }
            else {
                new NestedElementHandler(helper, this, child, childWrapper, target).init(name, attrs);
            }
        }
    }

    /**
     * Handler for all data types at global level.
     */
    private static class DataTypeHandler extends AbstractHandler {
        private Target target;
        private Object element;
        private RuntimeConfigurable wrapper = null;

        public DataTypeHandler(ProjectHelperImpl helper, DocumentHandler parentHandler) {
            this(helper, parentHandler, null);
        }

        public DataTypeHandler(ProjectHelperImpl helper,
                               DocumentHandler parentHandler,
                               Target target)
        {
            super(helper, parentHandler);
            this.target = target;
        }

        public void init(String propType, AttributeList attrs) throws SAXParseException {
            try {
                element = helper.project.createDataType(propType);
                if (element == null) {
                    throw new BuildException("Unknown data type "+propType);
                }
                
                if (target != null) {
                    wrapper = new RuntimeConfigurable(element, propType);
                    wrapper.setAttributes(attrs);
                    target.addDataType(wrapper);
                } else {
                    configure(element, attrs, helper.project);
                    helper.configureId(element, attrs);
                }
            } catch (BuildException exc) {
                throw new SAXParseException(exc.getMessage(), helper.locator, exc);
            }
        }

        public void characters(char[] buf, int start, int end) throws SAXParseException {
            try {
                addText(helper.project, element, buf, start, end);
            } catch (BuildException exc) {
                throw new SAXParseException(exc.getMessage(), helper.locator, exc);
            }
        }

        public void startElement(String name, AttributeList attrs) throws SAXParseException {
            new NestedElementHandler(helper, this, element, wrapper, target).init(name, attrs);
        }
    }

    public ProjectHelperImpl() {
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
