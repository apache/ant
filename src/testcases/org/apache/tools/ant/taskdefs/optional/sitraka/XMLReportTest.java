/*
 * Copyright  2001,2003-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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
import org.apache.tools.ant.types.Path;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Ensure that reference classpath feature is working fine...
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
        Path p = new Path(null);
        p.addJavaRuntime();
        Document doc = report.createDocument(p.list());

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
