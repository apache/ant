/* Copyright (c) 2000 The Apache Software Foundation */

package org.apache.tools.ant.xml;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.apache.tools.ant.*;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;

/**
 *  This class populates a Project object via SAX events.
 */
public class ProjectHandler extends DefaultHandler /* implements LexicalHandler */ {
    private Workspace workspace;
    private Project project;
    private Locator locator;

    /**
     *  The top of this stack represents the "current" event handler.
     */
    private Stack handlers;

    /**
     * Constructs a SAX handler for the specified project.
     */
    public ProjectHandler(Project project) {
        this.project = project;
        this.workspace = project.getWorkspace();
        this.handlers = new Stack();
        this.handlers.push(new RootHandler());
    }

    public Project getProject() {
        return project;
    }

    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    protected String getLocation() {
        return locator.getPublicId() + ":" + locator.getLineNumber();
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        // Delegate to the current handler
        ((ContentHandler)handlers.peek()).startElement(namespaceURI, localName, qName, atts);
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        // Delegate to the current handler
        ((ContentHandler)handlers.peek()).endElement(namespaceURI, localName, qName);
    }

    public void characters(char[] ch, int start, int length) {
        //XXX need to implement text content
    }

    public void processingInstruction(String target, String data) {
        System.out.println("@" + target + "@" + data + "@");
    }

    /*
    public void comment(char[] ch, int start, int length) {)
    public void endCDATA() {}
    public void endDTD() {}
    public void endEntity(java.lang.String name) {}
    public void startCDATA() {}
    public void startDTD(String name, String publicId, String systemId) {}
    public void startEntity(java.lang.String name)  {}
    */

    /**
     * This class handles any top level SAX events.
     */
    private class RootHandler extends DefaultHandler {
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if (isAntNamespace(namespaceURI) && localName.equals("project")) {
                handlers.push(new ProjectElemHandler(qName, atts));
            }
            else {
                throw new SAXParseException("Unexpected element \"" + qName + "\"", locator);
            }
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            handlers.pop();
        }
    }

    /**
     *  This class handles events that occur with a "project" element.
     */
    private class ProjectElemHandler extends DefaultHandler {
        public ProjectElemHandler(String qName, Attributes atts) throws SAXException {
            String projectName = null;

            for (int i = 0; i < atts.getLength(); i++) {
                if (!isAntNamespace(atts.getURI(i))) {
                    continue;
                }

                String name = atts.getQName(i);
                String value = atts.getValue(i);
                if (name.equals("name")) {
                    projectName = value;
                }
                else {
                    throw new SAXParseException("Unexpected attribute \"" + name + "\"", locator);
                }
            }

            if (projectName == null) {
                throw new SAXParseException("Missing attribute \"name\"", locator);
            }

            if (!projectName.equals(project.getName())) {
                throw new SAXParseException("A project named \"" + projectName + "\" must be located in a file called \"" + projectName + ".ant\"", locator);
            }
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if (isAntNamespace(namespaceURI) && localName.equals("target")) {
                handlers.push(new TargetElemHandler(project, qName, atts));
            }
            else if (isAntNamespace(namespaceURI) && localName.equals("import")) {
                handlers.push(new ImportElemHandler(project, qName, atts));
            }
            else {
                throw new SAXParseException("Unexpected element \"" + qName + "\"", locator);
            }
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            handlers.pop();
        }
    }

    /**
     *  This class handles events that occur with a "target" element.
     */
    private class TargetElemHandler extends DefaultHandler {
        private Target target;

        public TargetElemHandler(Project project, String qName, Attributes atts) throws SAXException {
            String targetName = null;
            String dependencies = "";

            for (int i = 0; i < atts.getLength(); i++) {
                if (!isAntNamespace(atts.getURI(i))) {
                    continue;
                }

                String name = atts.getQName(i);
                String value = atts.getValue(i);
                if (name.equals("name")) {
                    targetName = value;
                }
                else if (name.equals("depends")) {
                    dependencies = value;
                }
                else {
                    throw new SAXParseException("Unexpected attribute \"" + name + "\"", locator);
                }
            }

            if (targetName == null) {
                throw new SAXParseException("Missing attribute \"name\"", locator);
            }

            try {
                target = project.createTarget(targetName);
                target.setLocation(getLocation());
                parseDepends(dependencies);
            }
            catch(BuildException exc) {
                throw new SAXException(exc);
            }
        }

        /**
         *  Parses the list of space-separated project names.
         */
        private void parseDepends(String depends) {
            StringTokenizer tokenizer = new StringTokenizer(depends);
            while (tokenizer.hasMoreTokens()) {
                String targetName = tokenizer.nextToken();
                target.addDepend(targetName);
            }
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if (!isAntNamespace(namespaceURI)) {
                throw new SAXParseException("Unexpected attribute \"" + qName + "\"", locator);
            }

            TaskProxy proxy = target.createTaskProxy(qName);
            proxy.setLocation(getLocation());
            handlers.push(new TaskElemHandler(proxy.getData(), qName, atts));
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            handlers.pop();
        }
    }

    /**
     *  This class handles events that occur with a "import" element.
     */
    private class ImportElemHandler extends DefaultHandler {
        public ImportElemHandler(Project project, String qName, Attributes atts) throws SAXException {
            String importName = null;

            for (int i = 0; i < atts.getLength(); i++) {
                if (!isAntNamespace(atts.getURI(i))) {
                    continue;
                }

                String name = atts.getQName(i);
                String value = atts.getValue(i);
                if (name.equals("name")) {
                    importName = value;
                }
                else {
                    throw new SAXParseException("Unexpected attribute \"" + name + "\"", locator);
                }
            }

            if (importName == null) {
                throw new SAXParseException("Missing attribute \"name\"",  locator);
            }

            Import imp = project.createImport(importName);
            imp.setLocation(getLocation());
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            throw new SAXParseException("Unexpected element \"" + qName + "\"", locator);
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            handlers.pop();
        }
    }

    /**
     *  This class handles events that occur with a task element.
     */
    private class TaskElemHandler extends DefaultHandler {
        private TaskData data;

        public TaskElemHandler(TaskData data, String qName, Attributes atts) throws SAXException {
            this.data = data;

            for (int i = 0; i < atts.getLength(); i++) {
                if (!isAntNamespace(atts.getURI(i))) {
                    continue;
                }

                String name = atts.getQName(i);
                String value = atts.getValue(i);
                TaskData child = data.addProperty(name);
                child.setLocation(getLocation());
                child.setText(value);
            }
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if (!isAntNamespace(namespaceURI)) {
                throw new SAXParseException("Unexpected element \"" + qName + "\"", locator);
            }

            TaskData child = data.addProperty(qName);
            child.setLocation(getLocation());
            handlers.push(new TaskElemHandler(child, qName, atts));
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            handlers.pop();
        }
    }

    private static boolean isAntNamespace(String uri) {
        return uri == null ? false : uri.equals("");
    }
}