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
import java.util.StringTokenizer;

import org.apache.ant.common.model.Target;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;

/**
 * Element handler for the target element
 *
 * @author Conor MacNeill
 * @created 9 January 2002
 */
public class TargetHandler extends ModelElementHandler {
    /** The name attribute */
    public static final String NAME_ATTR = "name";

    /** The depends attribute name */
    public static final String DEPENDS_ATTR = "depends";

    /** The depends attribute name */
    public static final String DESC_ATTR = "description";

    /** The if attribute name */
    public static final String IF_ATTR = "if";

    /** The unless attribute name */
    public static final String UNLESS_ATTR = "unless";

    /** The target being configured. */
    private Target target;

    /**
     * Get the target parsed by this handler.
     *
     * @return the Target model object parsed by this handler.
     */
    public Target getTarget() {
        return target;
    }


    /**
     * Process the target element.
     *
     * @param elementName the name of the element
     * @exception SAXParseException if there is a problem parsing the
     *      element
     */
    public void processElement(String elementName)
         throws SAXParseException {
        target = new Target(getLocation(), getAttribute(NAME_ATTR));
        setModelElement(target);
        target.setDescription(getAttribute(DESC_ATTR));
        addNamespaceAttributes();

        String depends = getAttribute(DEPENDS_ATTR);
        if (depends != null) {
            StringTokenizer tokenizer = new StringTokenizer(depends, ",");
            while (tokenizer.hasMoreTokens()) {
                String dependency = tokenizer.nextToken().trim();
                target.addDependency(dependency);
            }
        }
        target.setIfCondition(getAttribute(IF_ATTR));
        target.setUnlessCondition(getAttribute(UNLESS_ATTR));
    }


    /**
     * Process an element within this target. All elements within the target
     * are treated as tasks.
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
        // everything is a task
        BuildElementHandler taskHandler = new BuildElementHandler();
        taskHandler.start(getParseContext(), getXMLReader(), this, getLocator(),
            attributes, getElementSource(), qualifiedName);
        target.addTask(taskHandler.getBuildElement());
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
        if (!attributeName.equals(NAME_ATTR)
            && !attributeName.equals(DEPENDS_ATTR)
            && !attributeName.equals(DESC_ATTR)
            && !attributeName.equals(IF_ATTR)
            && !attributeName.equals(UNLESS_ATTR)) {
            throwInvalidAttribute(attributeName);
        }
    }
}


