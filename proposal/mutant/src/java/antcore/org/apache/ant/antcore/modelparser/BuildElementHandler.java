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
import java.util.Iterator;

import org.apache.ant.common.model.BuildElement;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;

/**
 * A BuildElementHandler parses the task elements of a build. Task elements
 * include tasks themselves plus all their nested elements to any depth.
 *
 * @author Conor MacNeill
 * @created 9 January 2002
 */
public class BuildElementHandler extends ModelElementHandler {
    /** The task element being parsed by this handler. */
    private BuildElement buildElement;

    /**
     * Get the task element being parsed by this handler.
     *
     * @return the BuildElement being parsed.
     */
    public BuildElement getBuildElement() {
        return buildElement;
    }

    /**
     * Create a task element handler to parse a task element
     *
     * @param elementName the name of the element - always target
     */
    public void processElement(String elementName) {
        buildElement
             = new BuildElement(getLocation(), elementName);
        setModelElement(buildElement);

        for (Iterator i = getAttributes(); i.hasNext();) {
            String attributeName = (String) i.next();
            buildElement.addAttribute(attributeName,
                getAttribute(attributeName));
        }
        addNamespaceAttributes();
    }


    /**
     * Process a nested element of this task element. All nested elements of
     * a buildElement are themselves buildElements.
     *
     * @param uri The Namespace URI.
     * @param localName The local name (without prefix).
     * @param qualifiedName The qualified name (with prefix)
     * @param attributes The attributes attached to the element.
     * @throws SAXParseException if there is a parsing problem.
     */
    protected void addNestedElement(String uri, String localName,
                                    String qualifiedName, Attributes attributes)
         throws SAXParseException {

        // everything within a task element is also a task element
        BuildElementHandler nestedHandler
             = new BuildElementHandler();
        nestedHandler.start(getParseContext(), getXMLReader(),
            this, getLocator(), attributes, getElementSource(), qualifiedName);
        buildElement.addNestedElement(nestedHandler.getBuildElement());
    }


    /**
     * This method is called when this element is finished being processed.
     * This is a template method allowing subclasses to complete any
     * necessary processing.
     */
    protected void finish() {
        String content = getContent();
        if (content != null && content.trim().length() != 0) {
            buildElement.addText(getContent());
        }
    }

    /**
     * Validate that the given attribute and value are valid.
     *
     * @param attributeName The name of the attributes
     * @param attributeValue The value of the attributes
     */
    protected void validateAttribute(String attributeName,
                                     String attributeValue) {
        // do nothing - all attributes are OK by default.
    }
}

