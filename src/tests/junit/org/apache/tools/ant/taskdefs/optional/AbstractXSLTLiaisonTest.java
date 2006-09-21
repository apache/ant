/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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

package org.apache.tools.ant.taskdefs.optional;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import junit.framework.TestCase;
import org.apache.tools.ant.taskdefs.XSLTLiaison;
import org.apache.tools.ant.util.FileUtils;
import org.w3c.dom.Document;

/**
 * Abtract testcase for XSLTLiaison.
 * Override createLiaison for each XSLTLiaison.
 *
 * <a href="sbailliez@apache.org">Stephane Bailliez</a>
 */
public abstract class AbstractXSLTLiaisonTest extends TestCase {

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    
    protected XSLTLiaison liaison;

    protected  AbstractXSLTLiaisonTest(String name){
        super(name);
    }

    protected void setUp() throws Exception {
        liaison = createLiaison();
    }

    // to override
    protected abstract XSLTLiaison createLiaison() throws Exception ;

    /** load the file from the caller classloader that loaded this class */
    protected File getFile(String name) throws FileNotFoundException {
        URL url = getClass().getResource(name);
        if (url == null){
          throw new FileNotFoundException("Unable to load '" + name + "' from classpath");
        }
        return new File(FILE_UTILS.fromURI(url.toExternalForm()));
    }

    /** keep it simple stupid */
    public void testTransform() throws Exception {
        File xsl = getFile("/taskdefs/optional/xsltliaison-in.xsl");
        liaison.setStylesheet(xsl);
        liaison.addParam("param", "value");
        File in = getFile("/taskdefs/optional/xsltliaison-in.xml");
        File out = new File("xsltliaison.tmp");
        out.deleteOnExit(); // just to be sure
        try {
            liaison.transform(in, out);
        } finally {
            out.delete();
        }
    }

    public void testEncoding() throws Exception {
        File xsl = getFile("/taskdefs/optional/xsltliaison-encoding-in.xsl");
        liaison.setStylesheet(xsl);
        File in = getFile("/taskdefs/optional/xsltliaison-encoding-in.xml");
        File out = new File("xsltliaison-encoding.tmp");
        out.deleteOnExit(); // just to be sure
        try {
            liaison.transform(in, out);
            Document doc = parseXML(out);
            assertEquals("root",doc.getDocumentElement().getNodeName());
            assertEquals("message",doc.getDocumentElement().getFirstChild().getNodeName());
            assertEquals("\u00E9\u00E0\u00E8\u00EF\u00F9",doc.getDocumentElement().getFirstChild().getFirstChild().getNodeValue());
        } finally {
            out.delete();
        }
    }

    public Document parseXML(File file) throws Exception {
        DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dbuilder = dbfactory.newDocumentBuilder();
        return dbuilder.parse(file);
    }
}
