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

/**
 * Task get property values from a valid xml file.
 * Example:
 *   <root-tag myattr="true">
 *     <inner-tag someattr="val">Text</inner-tag>
 *     <a2><a3><a4>false</a4></a3></a2>
 *   </root-tag>
 *
 *  root-tag(myattr)=true
 *  root-tag.inner-tag=Text
 *  root-tag.inner-tag(someattr)=val
 *  root-tag.a2.a3.a4=false
 *
 * @author <a href="mailto:barozzi@nicolaken.com">Nicola Ken Barozzi</a>
 * @author Erik Hatcher
 * @created 14 January 2002
 */

public class XmlProperty extends org.apache.tools.ant.Task {
    private File src;
    private String prefix = "";
    private boolean keepRoot = true;
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
     * @exception org.apache.tools.ant.BuildException The exception raised during task execution.
     */
    public void execute()
            throws org.apache.tools.ant.BuildException {
        BufferedInputStream configurationStream = null;

        try {
            configurationStream =
                    new BufferedInputStream(new FileInputStream(src));

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            factory.setValidating(false);
            factory.setNamespaceAware(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(configurationStream);

            Element topElement = document.getDocumentElement();
            NodeList topChildren = topElement.getChildNodes();
            int numChildren = topChildren.getLength();

            String prefixToUse = "";

            if (!(prefix.equals(""))) {
                prefixToUse = prefix;
            }

            log("Prefix to use 1: \"" + prefixToUse + "\"", Project.MSG_DEBUG);

/*
            if ((!(prefix.equals(""))) && keepRoot) {
                prefixToUse += ".";
            }

            log("Prefix to use 2: \"" + prefixToUse + "\"", Project.MSG_DEBUG);

            if (keepRoot) {
                prefixToUse += (topElement.getNodeName());
            }

            log("Prefix to use 3: \"" + prefixToUse + "\"", Project.MSG_VERBOSE);
*/
            if (keepRoot) {
                addNodeRecursively(topElement, prefixToUse, 0);
            }
            else {
                for (int i = 0; i < numChildren; i++) {
                    addNodeRecursively(topChildren.item(i), prefixToUse, 0);
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


    void addNodeRecursively(org.w3c.dom.Node node, String prefix, int index) {

        if (node.hasAttributes()) {
            org.w3c.dom.NamedNodeMap nodeAttributes = node.getAttributes();
            for (int i = 0; i < nodeAttributes.getLength(); i++) {
                Node attributeNode = nodeAttributes.item(i);
                String attributeName = prefix + (prefix.trim().equals("")?"":".") + node.getNodeName() + "(" + attributeNode.getNodeName() + ")";
                String attributeValue = attributeNode.getNodeValue();
                log(attributeName + ":" + attributeValue, Project.MSG_DEBUG);
                project.setNewProperty(attributeName, attributeValue);
            }
        }

        if (node.getNodeType() == Node.TEXT_NODE) {
            String nodeText = node.getNodeValue();
            if (nodeText.trim().length() != 0) {
                log(prefix + ":" + nodeText, Project.MSG_DEBUG);
                if (index == 0) {
                    project.setNewProperty(prefix, nodeText);
                }

                project.setNewProperty(prefix + "[" + String.valueOf(index) + "]", nodeText);
            }
        }

        if (node.hasChildNodes()) {
            prefix += ((prefix.trim().equals("")?"":".") + node.getNodeName());
            org.w3c.dom.NodeList nodeChildren = node.getChildNodes();

            int numChildren = nodeChildren.getLength();

            StringBuffer childList = new StringBuffer();

            for (int i = 0; i < numChildren; i++) {
                if (i != 0) {
                    childList.append(",");
                }
                childList.append(node.getNodeName() + "[" + String.valueOf(index) + "]");
                addNodeRecursively(nodeChildren.item(i), prefix, i);
            }

            project.setNewProperty(prefix + "[]", childList.toString());

        }
    }

    public void setFile(File src) {
        this.src = src;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix.trim();
    }

    public void setKeeproot(boolean keepRoot) {
        this.keepRoot = keepRoot;
    }
}
