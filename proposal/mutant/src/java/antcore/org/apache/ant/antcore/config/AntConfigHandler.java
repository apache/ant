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
package org.apache.ant.antcore.config;
import org.apache.ant.antcore.xml.ElementHandler;
import org.apache.ant.antcore.modelparser.BuildElementHandler;
import org.apache.ant.common.model.BuildElement;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;

/**
 * XML Element Handler for Ant config files
 *
 * @author Conor MacNeill
 * @created 20 January 2002
 */
public class AntConfigHandler extends ElementHandler {
    /** The allowRemoteProject attribute name */
    public static final String REMOTE_PROJECT_ATTR = "allow-remote-project";

    /** The allowRemoteLibrary attribute name */
    public static final String REMOTE_LIBRARY_ATTR = "allow-remote-library";

    /** The allowReportProject attribute name */
    public static final String UNSET_PROPS_ATTR = "allow-unset-properties";

    /** The global tasks element */
    public static final String GLOBAL_TASKS_ELEMENT = "global-tasks";

    /** The per-frame tasks element */
    public static final String PERFRAME_TASKS_ELEMENT = "project-tasks";

    /** The list of allowed Attributes */
    public static final String[] ALLOWED_ATTRIBUTES
         = {REMOTE_PROJECT_ATTR, REMOTE_LIBRARY_ATTR, UNSET_PROPS_ATTR};
    /**
     * The config object which is contructed from the XML representation of
     * the config
     */
    private AntConfig config;

    /**
     * Get the Ant Config read in by this handler
     *
     * @return the AntConfig instance
     */
    public AntConfig getAntConfig() {
        return config;
    }

    /**
     * Process the antlib element
     *
     * @param elementName the name of the element
     * @exception SAXParseException if there is a problem parsing the
     *      element
     */
    public void processElement(String elementName)
         throws SAXParseException {
        config = new AntConfig();
        config.allowRemoteLibs(getBooleanAttribute(REMOTE_LIBRARY_ATTR));
        config.allowRemoteProjects(getBooleanAttribute(REMOTE_PROJECT_ATTR));
        boolean allowUnsetProperties = true;
        if (getAttribute(UNSET_PROPS_ATTR) != null) {
            allowUnsetProperties = getBooleanAttribute(UNSET_PROPS_ATTR);
        }
        config.allowUnsetProperties(allowUnsetProperties);
    }

    /**
     * Start a new element in the ant config.
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

        // configs support two task collections as elements
        BuildElementHandler buildElementHandler = new BuildElementHandler();
        buildElementHandler.start(getParseContext(), getXMLReader(),
            this, getLocator(), attributes, getElementSource(),
            qualifiedName);
        BuildElement element = buildElementHandler.getBuildElement();
        if (element.getType().equals(GLOBAL_TASKS_ELEMENT)) {
            config.addGlobalTasks(element);
        } else if (element.getType().equals(PERFRAME_TASKS_ELEMENT)) {
            config.addFrameTasks(element);
        } else {
            throw new SAXParseException("<antconfig> does not support the <"
                + element.getType() + "> element", getLocator());
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
        for (int i = 0; i < ALLOWED_ATTRIBUTES.length; ++i) {
            if (attributeName.equals(ALLOWED_ATTRIBUTES[i])) {
                return;
            }
        }
        throwInvalidAttribute(attributeName);
    }
}


