/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs.optional;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.tools.ant.taskdefs.XSLTLiaison;
import org.apache.tools.ant.util.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;

/**
 * Abstract testcase for XSLTLiaison.
 * Override createLiaison for each XSLTLiaison.
 *
 * <a href="sbailliez@apache.org">Stephane Bailliez</a>
 */
public abstract class AbstractXSLTLiaisonTest {

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    protected XSLTLiaison liaison;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        liaison = createLiaison();
    }

    // to override
    protected abstract XSLTLiaison createLiaison() throws Exception;

    /**
     * Load the file from the caller classloader that loaded this class
     *
     * @param name String
     * @return File
     * @throws FileNotFoundException if file is not found
     */
    protected File getFile(String name) throws FileNotFoundException {
        URL url = getClass().getResource(name);
        if (url == null) {
          throw new FileNotFoundException("Unable to load '" + name + "' from classpath");
        }
        return new File(FILE_UTILS.fromURI(url.toExternalForm()));
    }

    /**
     * Keep it simple stupid
     *
     * @throws Exception if something goes wrong
     */
    @Test
    public void testTransform() throws Exception {
        File xsl = getFile("/taskdefs/optional/xsltliaison-in.xsl");
        liaison.setStylesheet(xsl);
        liaison.addParam("param", "value");
        File in = getFile("/taskdefs/optional/xsltliaison-in.xml");
        File out = testFolder.newFile("xsltliaison.tmp");
        liaison.transform(in, out);
    }

    @Test
    public void testEncoding() throws Exception {
        File xsl = getFile("/taskdefs/optional/xsltliaison-encoding-in.xsl");
        liaison.setStylesheet(xsl);
        File in = getFile("/taskdefs/optional/xsltliaison-encoding-in.xml");
        File out = testFolder.newFile("xsltliaison-encoding.tmp");
        liaison.transform(in, out);
        Document doc = parseXML(out);
        assertEquals("root", doc.getDocumentElement().getNodeName());
        assertEquals("message", doc.getDocumentElement().getFirstChild().getNodeName());
        assertEquals("\u00E9\u00E0\u00E8\u00EF\u00F9",
                doc.getDocumentElement().getFirstChild().getFirstChild().getNodeValue());
    }

    public Document parseXML(File file) throws Exception {
        DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dbuilder = dbfactory.newDocumentBuilder();
        return dbuilder.parse(file);
    }
}
