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

package org.apache.tools.ant.types;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.MagicTestNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.JAXPUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * JUnit testcases for org.apache.tools.ant.types.XMLCatalog
 *
 */
public class XMLCatalogTest {

    private Project project;
    private XMLCatalog catalog;

    private XMLCatalog newCatalog() {
        XMLCatalog cat = new XMLCatalog();
        cat.setProject(project);
        return cat;
    }

    private static String toURLString(File file) {
        return JAXPUtils.getSystemId(file);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        project = new Project();
        if (System.getProperty(MagicTestNames.TEST_ROOT_DIRECTORY) != null) {
            project.setBasedir(System.getProperty(MagicTestNames.TEST_ROOT_DIRECTORY));
        }
        // This causes XMLCatalog to print out detailed logging
        // messages for debugging
        //
        // DefaultLogger logger = new DefaultLogger();
        // logger.setMessageOutputLevel(Project.MSG_DEBUG);
        // logger.setOutputPrintStream(System.out);
        // logger.setErrorPrintStream(System.err);
        // project.addBuildListener(logger);

        catalog = newCatalog();
    }

    @Test
    public void testEmptyCatalogResolveEntity() throws IOException, SAXException {
        InputSource result = catalog.resolveEntity("PUBLIC ID ONE", "i/dont/exist.dtd");
        assertNull("Empty catalog should return null", result);
     }

    @Test
    public void testEmptyCatalogResolve() throws TransformerException, MalformedURLException {
        String expected = toURLString(new File(project.getBaseDir() +
                "/i/dont/exist.dtd"));
        Source result = catalog.resolve("i/dont/exist.dtd", null);
        String resultStr = fileURLPartWithoutLeadingSlashes((SAXSource) result);
        assertThat("Empty catalog should return input with a system ID like "
                        + expected + " but was " + resultStr, expected, endsWith(resultStr));
    }

    private static String fileURLPartWithoutLeadingSlashes(SAXSource result)
        throws MalformedURLException {
        //
        // These shenanigans are necessary b/c Norm Walsh's resolver
        // has a different idea of how file URLs are created on windoze
        // ie file://c:/foo instead of file:///c:/foo
        //
        String resultStr = new URL(result.getInputSource().getSystemId()).getFile();
        // on Sun's Java6 this returns an unexpected number of four
        // leading slashes, at least on Linux - strip all of them
        while (resultStr.startsWith("/")) {
            resultStr = resultStr.substring(1);
        }
        return resultStr;
    }

    @Test
    public void testNonExistentEntry() throws IOException, SAXException, TransformerException {
        ResourceLocation dtd = new ResourceLocation();
        dtd.setPublicId("PUBLIC ID ONE");
        dtd.setLocation("i/dont/exist.dtd");

        InputSource isResult = catalog.resolveEntity("PUBLIC ID ONE", "i/dont/exist.dtd");
        assertNull("Nonexistent Catalog entry should not be returned", isResult);

        Source result = catalog.resolve("i/dont/exist.dtd", null);
        String expected = toURLString(new File(project.getBaseDir().toURL()
                + "/i/dont/exist.dtd"));
        String resultStr = fileURLPartWithoutLeadingSlashes((SAXSource) result);
        assertThat("Nonexistent Catalog entry return input with a system ID like "
                   + expected + " but was " + resultStr,
                   expected, endsWith(resultStr));
    }

    @Test
    public void testEmptyElementIfIsReferenceAttr() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        ResourceLocation dtd = new ResourceLocation();
        dtd.setPublicId("PUBLIC ID ONE");
        dtd.setLocation("i/dont/exist.dtd");
        catalog.addDTD(dtd);
        project.addReference("catalog", catalog);
        catalog.setRefid(new Reference(project, "dummyref"));
    }

    @Test
    public void testEmptyElementIfIsReferenceElem() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify nested elements when using refid");
        XMLCatalog catalogA = newCatalog();
        catalogA.setRefid(new Reference(project, "catalog"));
        catalogA.addConfiguredXMLCatalog(catalog);
    }

    @Test
    public void testCircularReferenceCheck() throws IOException, SAXException {
        thrown.expect(BuildException.class);
        thrown.expectMessage("This data type contains a circular reference.");
        // catalog <--> catalog
        project.addReference("catalog", catalog);
        catalog.setRefid(new Reference(project, "catalog"));
        catalog.resolveEntity("PUBLIC ID ONE", "i/dont/exist.dtd");
    }

    @Test
    public void testLoopReferenceCheck() throws IOException, SAXException {
        thrown.expect(BuildException.class);
        thrown.expectMessage("This data type contains a circular reference.");
        // catalog --> catalogA --> catalogB --> catalog
        project.addReference("catalog", catalog);
        XMLCatalog catalogA = newCatalog();
        project.addReference("catalogA", catalogA);
        XMLCatalog catalogB = newCatalog();
        project.addReference("catalogB", catalogB);

        catalogB.setRefid(new Reference(project, "catalog"));
        catalogA.setRefid(new Reference(project, "catalogB"));
        catalog.setRefid(new Reference(project, "catalogA"));

        catalog.resolveEntity("PUBLIC ID ONE", "i/dont/exist.dtd");
    }

    // inspired by Bugzilla Report 23913
    // a problem used to happen under Windows when the location of the DTD was given as an absolute path
    // possibly with a mixture of file separators
    @Test
    public void testAbsolutePath() throws IOException, SAXException {
        ResourceLocation dtd = new ResourceLocation();
        dtd.setPublicId("-//stevo//DTD doc 1.0//EN");

        String sysid = project.resolveFile("src/etc/testcases/taskdefs/optional/xml/doc.dtd")
                .getAbsolutePath();
        dtd.setLocation(sysid);
        catalog.addDTD(dtd);

        InputSource result = catalog.resolveEntity("-//stevo//DTD doc 1.0//EN",
                                                   "nap:chemical+brothers");
        assertNotNull(result);

        File dtdFile = project.resolveFile(sysid);
        assertEquals(toURLString(dtdFile), result.getSystemId());
    }

    @Test
    public void testSimpleEntry() throws IOException, SAXException {
        ResourceLocation dtd = new ResourceLocation();
        dtd.setPublicId("-//stevo//DTD doc 1.0//EN");
        String sysid = "src/etc/testcases/taskdefs/optional/xml/doc.dtd";
        dtd.setLocation(sysid);
        catalog.addDTD(dtd);

        InputSource result = catalog.resolveEntity("-//stevo//DTD doc 1.0//EN",
                                                   "nap:chemical+brothers");
        assertNotNull(result);

        File dtdFile = project.resolveFile(sysid);
        assertEquals(toURLString(dtdFile), result.getSystemId());
    }

    @Test
    public void testEntryReference() throws IOException, SAXException, TransformerException {
        String publicId = "-//stevo//DTD doc 1.0//EN";
        String sysid = "src/etc/testcases/taskdefs/optional/xml/doc.dtd";

        // catalogB --> catalogA --> catalog
        ResourceLocation dtd = new ResourceLocation();
        dtd.setPublicId(publicId);
        dtd.setLocation(sysid);
        catalog.addDTD(dtd);

        String uri = "http://foo.com/bar/blah.xml";
        String uriLoc = "src/etc/testcases/taskdefs/optional/xml/about.xml";

        ResourceLocation entity = new ResourceLocation();
        entity.setPublicId(uri);
        entity.setLocation(uriLoc);
        catalog.addEntity(entity);

        project.addReference("catalog", catalog);

        XMLCatalog catalogA = newCatalog();
        project.addReference("catalogA", catalogA);
        XMLCatalog catalogB = newCatalog();
        project.addReference("catalogB", catalogB);

        catalogA.setRefid(new Reference(project, "catalog"));
        catalogB.setRefid(new Reference(project, "catalogA"));

        InputSource isResult = catalogB.resolveEntity(publicId, "nap:chemical+brothers");
        assertNotNull(isResult);

        File dtdFile = project.resolveFile(sysid);
        assertEquals(toURLString(dtdFile), isResult.getSystemId());

        Source result = catalog.resolve(uri, null);
        assertNotNull(result);

        File xmlFile = project.resolveFile(uriLoc);
        assertEquals(toURLString(xmlFile), result.getSystemId());
    }

    @Test
    public void testNestedCatalog() throws IOException, SAXException, TransformerException {
        String publicId = "-//stevo//DTD doc 1.0//EN";
        String dtdLoc = "src/etc/testcases/taskdefs/optional/xml/doc.dtd";

        ResourceLocation dtd = new ResourceLocation();
        dtd.setPublicId(publicId);
        dtd.setLocation(dtdLoc);
        catalog.addDTD(dtd);

        String uri = "http://foo.com/bar/blah.xml";
        String uriLoc = "src/etc/testcases/taskdefs/optional/xml/about.xml";

        ResourceLocation entity = new ResourceLocation();
        entity.setPublicId(uri);
        entity.setLocation(uriLoc);
        catalog.addEntity(entity);

        XMLCatalog catalogA = newCatalog();
        catalogA.addConfiguredXMLCatalog(catalog);
        InputSource isResult = catalogA.resolveEntity(publicId, "nap:chemical+brothers");
        assertNotNull(isResult);

        File dtdFile = project.resolveFile(dtdLoc);
        assertEquals(toURLString(dtdFile), isResult.getSystemId());

        Source result = catalog.resolve(uri, null);
        assertNotNull(result);

        File xmlFile = project.resolveFile(uriLoc);
        assertEquals(toURLString(xmlFile), result.getSystemId());
    }

    @Test
    public void testResolverBase() throws TransformerException {
        String uri = "http://foo.com/bar/blah.xml";
        String uriLoc = "etc/testcases/taskdefs/optional/xml/about.xml";
        String base = toURLString(project.getBaseDir()) + "/src/";

        ResourceLocation entity = new ResourceLocation();
        entity.setPublicId(uri);
        entity.setLocation(uriLoc);
        catalog.addEntity(entity);

        Source result = catalog.resolve(uri, base);
        assertNotNull(result);

        File xmlFile = project.resolveFile("src/" + uriLoc);
        assertEquals(toURLString(xmlFile), result.getSystemId());
    }

    @Test
    public void testClasspath() throws IOException, TransformerException, SAXException {
        String publicId = "-//stevo//DTD doc 1.0//EN";
        String dtdLoc = "testcases/taskdefs/optional/xml/doc.dtd";
        String path1 = project.getBaseDir().toString() + "/src/etc";

        ResourceLocation dtd = new ResourceLocation();
        dtd.setPublicId(publicId);
        dtd.setLocation(dtdLoc);
        catalog.addDTD(dtd);

        String uri = "http://foo.com/bar/blah.xml";
        String uriLoc = "etc/testcases/taskdefs/optional/xml/about.xml";
        String path2 = project.getBaseDir().toString() + "/src";

        ResourceLocation entity = new ResourceLocation();
        entity.setPublicId(uri);
        entity.setLocation(uriLoc);
        catalog.addEntity(entity);

        Path aPath = new Path(project, path1);
        aPath.append(new Path(project, path2));
        catalog.setClasspath(aPath);

        InputSource isResult = catalog.resolveEntity(publicId, "nap:chemical+brothers");
        assertNotNull(isResult);

        String resultStr1 = new URL(isResult.getSystemId()).getFile();
        File dtdFile = project.resolveFile("src/etc/" + dtdLoc);
        assertThat(toURLString(dtdFile), endsWith(resultStr1));

        Source result = catalog.resolve(uri, null);
        assertNotNull(result);

        File xmlFile = project.resolveFile("src/" + uriLoc);
        String resultStr = new URL(result.getSystemId()).getFile();
        assertThat(toURLString(xmlFile), endsWith(resultStr));
    }
}
