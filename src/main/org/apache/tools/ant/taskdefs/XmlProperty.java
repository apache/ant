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
 * 4. The names "Ant" and "Apache Software
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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * Loads property values from a valid XML file,
 * generating the property names from the file's element and attribute names.
 *
 * Example:
 * <pre>
 *   &lt;root-tag myattr="true"&gt;
 *     &lt;inner-tag someattr="val"&gt;Text&lt;/inner-tag&gt;
 *     &lt;a2&gt;&lt;a3&gt;&lt;a4&gt;false&lt;/a4&gt;&lt;/a3&gt;&lt;/a2&gt;
 *   &lt;/root-tag&gt;
 *</pre>
 * this generates
 * <pre>
 *  root-tag(myattr)=true
 *  root-tag.inner-tag=Text
 *  root-tag.inner-tag(someattr)=val
 *  root-tag.a2.a3.a4=false
 * </pre>
 * @author <a href="mailto:nicolaken@apache.org">Nicola Ken Barozzi</a>
 * @author Erik Hatcher
 * @created 14 January 2002
 * @ant.task name="xmlproperty" category="xml"
 */

public class XmlProperty extends org.apache.tools.ant.Task {

    private File src;
    private String prefix = "";
    private boolean keepRoot = true;
    private boolean validate = false;
    private boolean collapseAttributes = false;
    private org.w3c.dom.Document document;

    /**
     * Constructor.
     */
    public XmlProperty() {
        super();
    }

    /**
     * Initializes the task.
     */

    public void init() {
        super.init();
    }

    /**
     * Run the task.
     * @throws BuildException The exception raised during task execution.
     * @todo validate the source file is valid before opening, print a better error message
     * @todo add a verbose level log message listing the name of the file being loaded
     */
    public void execute()
            throws BuildException {
            
        BufferedInputStream configurationStream = null;

        try {
            configurationStream =
                    new BufferedInputStream(new FileInputStream(src));

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            factory.setValidating(validate);
            factory.setNamespaceAware(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(configurationStream);

            Element topElement = document.getDocumentElement();
            NodeList topChildren = topElement.getChildNodes();
            int numChildren = topChildren.getLength();

            log("Using prefix: \"" + prefix + "\"", Project.MSG_DEBUG);

            if (keepRoot) {
                addNodeRecursively(topElement, prefix);
            }
            else {
                for (int i = 0; i < numChildren; i++) {
                    addNodeRecursively(topChildren.item(i), prefix);
                }
            }

        } catch (SAXException sxe) {
            // Error generated during parsing
            Exception x = sxe;
            if (sxe.getException() != null)
                x = sxe.getException();
            throw new BuildException(x);

        } catch (ParserConfigurationException pce) {
            // Parser with specified options can't be built
            throw new BuildException(pce);
        } catch (IOException ioe) {
            // I/O error
            throw new BuildException(ioe);
        } finally {
            if (configurationStream != null) {
                try {
                    configurationStream.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * add all attributes of a node, and its inner text, and then recursively add all nested elements
     */

    void addNodeRecursively(org.w3c.dom.Node node, String prefix) {

        if (node.hasAttributes()) {
            org.w3c.dom.NamedNodeMap nodeAttributes = node.getAttributes();
            for (int i = 0; i < nodeAttributes.getLength(); i++) {
                Node attributeNode = nodeAttributes.item(i);
                String attributeName;
                
                if(collapseAttributes){
                  attributeName = prefix + (prefix.trim().equals("")?"":".") + node.getNodeName() + "." + attributeNode.getNodeName();
                }
                else{
                  attributeName = prefix + (prefix.trim().equals("")?"":".") + node.getNodeName() + "(" + attributeNode.getNodeName() + ")";
                }              
          
                String attributeValue = attributeNode.getNodeValue();
                log(attributeName + ":" + attributeValue, Project.MSG_DEBUG);
                project.setNewProperty(attributeName, attributeValue);
            }
        }

        if (node.getNodeType() == Node.TEXT_NODE) {
            String nodeText = node.getNodeValue();
            if (nodeText.trim().length() != 0) {
                log(prefix + ":" + nodeText, Project.MSG_DEBUG);
                 project.setNewProperty(prefix, nodeText);
            }
        }

        if (node.hasChildNodes()) {
            prefix += ((prefix.trim().equals("")?"":".") + node.getNodeName());

            org.w3c.dom.NodeList nodeChildren = node.getChildNodes();
            int numChildren = nodeChildren.getLength();

            for (int i = 0; i < numChildren; i++) {
                addNodeRecursively(nodeChildren.item(i), prefix);
            }
        }
    }

    /**
     * The XML file to parse; required.
     */
    public void setFile(File src) {
        this.src = src;
    }

    /**
     * the prefix to prepend to each property
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix.trim();
    }

    /**
     * flag to include the xml root tag as a 
     * first value in the property name; optional, 
     * default is true
     */
    public void setKeeproot(boolean keepRoot) {
        this.keepRoot = keepRoot;
    }

    /**
     * flag to validate the XML file; optional, default false
     */
    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    /**
     * flag to treat attributes as nested elements;
     * optional, default false
     */
    public void setCollapseAttributes(boolean collapseAttributes) {
        this.collapseAttributes = collapseAttributes;
    }
        
}
