/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.sitraka;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.net.URL;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Ensure that reference classpath feature is working fine...
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public class XMLReportTest extends TestCase {
    public XMLReportTest(String s) {
        super(s);
    }

    protected File getFile(String name) throws FileNotFoundException {
        URL url = getClass().getResource(name);
        if (url == null) {
            throw new FileNotFoundException("Unable to load '" + name + "' from classpath");
        }
        return new File(url.getFile());
    }

    public void testCreateDocument() throws Exception {
        // this is a sample from running Ant include data for java.* only
        File reportFile = getFile("/taskdefs/optional/sitraka/covreport-test.xml");
        XMLReport report = new XMLReport(reportFile);
        ReportFilters filters = new ReportFilters();
        ReportFilters.Include incl = new ReportFilters.Include();
        incl.setClass("java.util.Vector");
        incl.setMethod("set*");
        filters.addInclude(incl);
        report.setReportFilters(filters);
        Document doc = report.createDocument(new String[]{ System.getProperty("java.home") + "/lib/rt.jar"});

        Node snapshot = doc.getDocumentElement();
        assertEquals("snapshot", snapshot.getNodeName());

        // there is only java.util
        NodeList packages = doc.getElementsByTagName("package");
        assertEquals(1, packages.getLength());
        assertEquals("java.util", packages.item(0).getAttributes().getNamedItem("name").getNodeValue());

        // there is only Vector
        NodeList classes = doc.getElementsByTagName("class");
        assertEquals(1, classes.getLength());
        assertEquals("Vector", classes.item(0).getAttributes().getNamedItem("name").getNodeValue());

        // there are 3 set* methods
        // set(int, Object)
        // setSize(int)
        // setElementAt(Object, int)
        NodeList methods = doc.getElementsByTagName("method");
        assertEquals(3, methods.getLength());

        //dump(doc, System.out);
    }

    /**
     *  might be useful to spit out the document
     * it's a nightmare to navigate in a DOM structure in a debugger.
     */
    protected void dump(Document doc, OutputStream out) throws Exception {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        Transformer transformer = tfactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.transform(new DOMSource(doc), new StreamResult(out));
    }
}
