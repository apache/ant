/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
package org.apache.ant.antcore.modelparser;
import org.apache.ant.common.model.ModelException;
import org.apache.ant.common.model.Project;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;

/**
 * Element to parse the project element. The project handler creates a
 * number of different handlers to which it delegates processing of child
 * elements.
 *
 * @author Conor MacNeill
 * @created 9 January 2002
 */
public class ProjectHandler extends ModelElementHandler {
    /** The basedir attribute tag */
    public static final String BASEDIR_ATTR = "basedir";

    /** The name attribute */
    public static final String NAME_ATTR = "name";

    /** The default attribute name */
    public static final String DEFAULT_ATTR = "default";

    /** The name of the element used to define references */
    public static final String REF_ELEMENT = "ant:ref";
    
    /** The name of the element used to define references */
    public static final String INCLUDE_ELEMENT = "ant:include";
    
    /** The name of the element used to define references */
    public static final String TARGET_ELEMENT = "target";

    /** The project being parsed. */
    private Project project;

    /** Constructor parsing a new project */
    public ProjectHandler() {
        project = null;
    }

    /**
     * Constructor for including a project or fragment into an existing
     * project
     *
     * @param project The project to be configured by the handler
     */
    public ProjectHandler(Project project) {
        this.project = project;
    }

    /**
     * Get the project that has been parsed from the element
     *
     * @return the project that has been parsed from the build source
     * @throws NoProjectReadException thrown if no project was read in.
     */
    public Project getProject()
         throws NoProjectReadException {
        if (project == null) {
            throw new NoProjectReadException();
        }
        return project;
    }


    /**
     * Process the project element
     *
     * @param elementName the name of the element
     * @exception SAXParseException if there is a problem parsing the
     *      element
     */
    public void processElement(String elementName)
         throws SAXParseException {
        if (project == null) {
            project = new Project(getElementSource(), getLocation());
            setModelElement(project);
            
            project.setDefaultTarget(getAttribute(DEFAULT_ATTR));
            project.setBase(getAttribute(BASEDIR_ATTR));
            project.setName(getAttribute(NAME_ATTR));
            project.setAspects(getAspects());
        }
    }


    /**
     * Start a new element in the project. Project currently handles the
     * following elements
     * <ul>
     *   <li> ref</li>
     *   <li> include</li>
     *   <li> target</li>
     * </ul>
     * Everything else is treated as a task.
     *
     * @param uri The Namespace URI.
     * @param localName The local name (without prefix).
     * @param qualifiedName The qualified name (with prefix)
     * @param attributes The attributes attached to the element.
     * @throws SAXParseException if there is a parsing problem.
     */
    public void startElement(String uri, String localName, String qualifiedName,
                             Attributes attributes)
         throws SAXParseException {

        if (qualifiedName.equals(INCLUDE_ELEMENT)) {
            IncludeHandler includeHandler = new IncludeHandler(project);
            includeHandler.start(getParseContext(), getXMLReader(),
                this, getLocator(), attributes, getElementSource(),
                qualifiedName);
        } else if (qualifiedName.equals(TARGET_ELEMENT)) {
            TargetHandler targetHandler = new TargetHandler();
            targetHandler.start(getParseContext(), getXMLReader(),
                this, getLocator(), attributes,
                getElementSource(), qualifiedName);
            try {
                project.addTarget(targetHandler.getTarget());
            } catch (ModelException e) {
                throw new SAXParseException(e.getMessage(), getLocator(), e);
            }
        } else {
            // everything else is a task
            BuildElementHandler buildElementHandler = new BuildElementHandler();
            buildElementHandler.start(getParseContext(), getXMLReader(),
                this, getLocator(), attributes, getElementSource(),
                qualifiedName);
            project.addTask(buildElementHandler.getBuildElement());
        }
    }

    /**
     * Validate that the given attribute and value are valid.
     *
     * @param attributeName The name of the attributes
     * @param attributeValue The value of the attributes
     * @exception SAXParseException if the attribute is not allowed on the
     *      element.
     */
    protected void validateAttribute(String attributeName,
                                     String attributeValue)
         throws SAXParseException {
        if (!attributeName.equals(BASEDIR_ATTR) &&
            !attributeName.equals(NAME_ATTR) &&
            !attributeName.equals(DEFAULT_ATTR)) {
            throwInvalidAttribute(attributeName);
        }
    }
}


