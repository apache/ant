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

import java.net.MalformedURLException;
import java.net.URL;
import org.apache.ant.common.model.Project;
import org.apache.ant.antcore.xml.ElementHandler;
import org.apache.ant.antcore.xml.XMLParseException;
import org.xml.sax.SAXParseException;

/**
 * The Ref handler handles the reference of one project to another. The
 * project to be references is parsed with a new parser and then added to
 * the current project under the given alias
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @created 10 January 2002
 */
public class RefHandler extends ElementHandler {
    /** The attribute used to name the ref. */
    public final static String NAME_ATTR = "name";

    /** The attribute name used to locate the project to be referenced. */
    public final static String SYSTEMID_ATTR = "project";

    /** The project that has been referenced. */
    private Project referencedProject;

    /**
     * Get the project referenced.
     *
     * @return an referenced Project.
     */
    public Project getReferencedProject() {
        return referencedProject;
    }


    /**
     * Get the name under which the project is referenced.
     *
     * @return the ref name of the project
     */
    public String getRefName() {
        return getAttribute(NAME_ATTR);
    }


    /**
     * Create an ref handler to reference a project.
     *
     * @param elementName the name of the ref element
     * @exception SAXParseException if the ref element could not be parsed
     */
    public void processElement(String elementName)
         throws SAXParseException {
        String refName = getAttribute(NAME_ATTR);
        if (refName == null) {
            throw new SAXParseException("Attribute " + NAME_ATTR +
                " is required in a <ref> element", getLocator());
        }

        String projectSystemId = getAttribute(SYSTEMID_ATTR);
        if (projectSystemId == null) {
            throw new SAXParseException("Attribute " + SYSTEMID_ATTR +
                " is required in a <ref> element", getLocator());
        }

        // create a new parser to read this project relative to the
        // project's URI
        try {
            URL refURL = new URL(getElementSource(), projectSystemId);
            ProjectHandler referencedProjectHandler = new ProjectHandler();
            getParseContext().parse(refURL, "project",
                referencedProjectHandler);

            referencedProject = referencedProjectHandler.getProject();
        } catch (XMLParseException e) {
            throw new SAXParseException("Error parsing referenced project "
                 + projectSystemId + ": " + e.getMessage(), getLocator(), e);
        } catch (NoProjectReadException e) {
            throw new SAXParseException("No project found in the reference: "
                 + projectSystemId, getLocator(), e);
        } catch (MalformedURLException e) {
            throw new SAXParseException("Unable to reference project "
                 + projectSystemId + ": " + e.getMessage(),
                getLocator(), e);
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
        if (!attributeName.equals(SYSTEMID_ATTR) &&
            !attributeName.equals(NAME_ATTR)) {
            throwInvalidAttribute(attributeName);
        }
    }
}


