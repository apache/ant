/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.JAXPUtils;

import junit.framework.TestCase;

import java.io.File;

import java.net.MalformedURLException;
import java.net.URL;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.TransformerException;

/**
 * JUnit testcases for org.apache.tools.ant.types.XMLCatalog
 *
 * @author <a href="mailto:cstrong@arielpartners.com">Craeg Strong</a> 
 * @version $Id$
 */
public class XMLCatalogTest extends TestCase {

    private Project project;
    private XMLCatalog catalog;

    private XMLCatalog newCatalog() {
        XMLCatalog cat = new XMLCatalog();
        cat.setProject(project);
        return cat;
    }

    private String toURLString(File file) throws MalformedURLException {
        return JAXPUtils.getSystemId(file);
    }

    public XMLCatalogTest(String name) {
        super(name);
    }

    public void setUp() {
        project = new Project();
        project.setBasedir(".");

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

   public void tearDown() {
      project = null;
      catalog = null;
   }
   
   public void testEmptyCatalog() {
       try {
           InputSource result = catalog.resolveEntity("PUBLIC ID ONE", 
                                                      "i/dont/exist.dtd");
           assertNull("Empty catalog should return null", result);
       } catch (Exception e) {
           fail("resolveEntity() failed!" + e.toString());
       }

       try {
           Source result = catalog.resolve("i/dont/exist.dtd", null);
           String expected = toURLString(new File(project.getBaseDir() +
                                                  "/i/dont/exist.dtd"));
           //
           // These shenanigans are necessary b/c Norm Walsh's resolver
           // has a different idea of how file URLs are created on windoze
           // ie file://c:/foo instead of file:///c:/foo
           // 
           String resultStr = new URL(((SAXSource)result).getInputSource().getSystemId()).getFile();
           assertTrue("Empty catalog should return input", 
                      expected.endsWith(resultStr));
       } catch (Exception e) {
           fail("resolve() failed!" + e.toString());
       }
   }

    public void testNonExistentEntry() {

        ResourceLocation dtd = new ResourceLocation();
        dtd.setPublicId("PUBLIC ID ONE");
        dtd.setLocation("i/dont/exist.dtd");

        try {
            InputSource result = catalog.resolveEntity("PUBLIC ID ONE", 
                                                       "i/dont/exist.dtd");
            assertNull("Nonexistent Catalog entry should not be returned", result);
        } catch (Exception e) {
            fail("resolveEntity() failed!" + e.toString());
        }

        try {
            Source result = catalog.resolve("i/dont/exist.dtd", null);
            String expected = toURLString(new File(project.getBaseDir().toURL() +
                                                   "/i/dont/exist.dtd"));
            String resultStr = new URL(((SAXSource)result).getInputSource().getSystemId()).getFile();
            assertTrue("Nonexistent Catalog entry return input",
                       expected.endsWith(resultStr));
        } catch (Exception e) {
            fail("resolve() failed!" + e.toString());
        }
    }

    public void testEmptyElementIfIsReference() {
        ResourceLocation dtd = new ResourceLocation();
        dtd.setPublicId("PUBLIC ID ONE");
        dtd.setLocation("i/dont/exist.dtd");
        catalog.addDTD(dtd);
        project.addReference("catalog", catalog);
        
        try {
            catalog.setRefid(new Reference("dummyref"));
            fail("Can add reference to nonexistent XMLCatalog");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one " 
                         + "attribute when using refid", be.getMessage());
        }
        
        XMLCatalog catalog2 = newCatalog();
        catalog2.setRefid(new Reference("catalog"));

        try {
            catalog2.addConfiguredXMLCatalog(catalog);
            fail("Can add nested XMLCatalog to XMLCatalog that is a reference");
        } catch (BuildException be) {
            assertEquals("You must not specify nested elements when using refid",
                         be.getMessage());
        }
    }

    public void testCircularReferenceCheck() {

        // catalog <--> catalog
        project.addReference("catalog", catalog);
        catalog.setRefid(new Reference("catalog"));

        try {
            InputSource result = catalog.resolveEntity("PUBLIC ID ONE", 
                                                       "i/dont/exist.dtd");
            fail("Can make XMLCatalog a Reference to itself.");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        } catch (Exception e) {
            fail("resolveEntity() failed!" + e.toString());
        }

        // catalog1 --> catalog2 --> catalog3 --> catalog1
        XMLCatalog catalog1 = newCatalog();
        project.addReference("catalog1", catalog1);
        XMLCatalog catalog2 = newCatalog();
        project.addReference("catalog2", catalog2);
        XMLCatalog catalog3 = newCatalog();
        project.addReference("catalog3", catalog3);

        catalog3.setRefid(new Reference("catalog1"));
        catalog2.setRefid(new Reference("catalog3"));
        catalog1.setRefid(new Reference("catalog2"));

        try {
            InputSource result = catalog1.resolveEntity("PUBLIC ID ONE", 
                                                        "i/dont/exist.dtd");
            fail("Can make circular reference");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        } catch (Exception e) {
            fail("resolveEntity() failed!" + e.toString());
        }
    }
    // inspired by Bugzilla Report 23913
    // a problem used to happen under Windows when the location of the DTD was given as an absolute path
    // possibly with a mixture of file separators
    public void testAbsolutePath() {
        ResourceLocation dtd = new ResourceLocation();
        dtd.setPublicId("-//stevo//DTD doc 1.0//EN");

        String sysid = System.getProperty("user.dir") + File.separator + "src/etc/testcases/taskdefs/optional/xml/doc.dtd";
        dtd.setLocation(sysid);
        catalog.addDTD(dtd);
        File dtdFile = project.resolveFile(sysid);

        try {
            InputSource result = catalog.resolveEntity("-//stevo//DTD doc 1.0//EN",
                                                       "nap:chemical+brothers");
            assertNotNull(result);
            assertEquals(toURLString(dtdFile),
                         result.getSystemId());
        } catch (Exception e) {
            fail("resolveEntity() failed!" + e.toString());
        }

    }

    public void testSimpleEntry() {

        ResourceLocation dtd = new ResourceLocation();
        dtd.setPublicId("-//stevo//DTD doc 1.0//EN");
        String sysid = "src/etc/testcases/taskdefs/optional/xml/doc.dtd";
        dtd.setLocation(sysid);
        catalog.addDTD(dtd);
        File dtdFile = project.resolveFile(sysid);
        
        try {
            InputSource result = catalog.resolveEntity("-//stevo//DTD doc 1.0//EN",
                                                       "nap:chemical+brothers");
            assertNotNull(result);
            assertEquals(toURLString(dtdFile),
                         result.getSystemId());
        } catch (Exception e) {
            fail("resolveEntity() failed!" + e.toString());
        }
    }

    public void testEntryReference() {

        String publicId = "-//stevo//DTD doc 1.0//EN";
        String sysid = "src/etc/testcases/taskdefs/optional/xml/doc.dtd";

        // catalog2 --> catalog1 --> catalog
        ResourceLocation dtd = new ResourceLocation();
        dtd.setPublicId(publicId);
        dtd.setLocation(sysid);
        catalog.addDTD(dtd);
        File dtdFile = project.resolveFile(sysid);

        String uri = "http://foo.com/bar/blah.xml";
        String uriLoc = "src/etc/testcases/taskdefs/optional/xml/about.xml";

        ResourceLocation entity = new ResourceLocation();
        entity.setPublicId(uri);
        entity.setLocation(uriLoc);
        catalog.addEntity(entity);
        File xmlFile = project.resolveFile(uriLoc);

        project.addReference("catalog", catalog);

        XMLCatalog catalog1 = newCatalog();
        project.addReference("catalog1", catalog1);
        XMLCatalog catalog2 = newCatalog();
        project.addReference("catalog2", catalog1);

        catalog1.setRefid(new Reference("catalog"));
        catalog2.setRefid(new Reference("catalog1"));

        try {
            InputSource result = catalog2.resolveEntity(publicId,
                                                        "nap:chemical+brothers");

            assertNotNull(result);
            assertEquals(toURLString(dtdFile),
                         result.getSystemId());
        } catch (Exception e) {
            fail("resolveEntity() failed!" + e.toString());
        }

        try {
            Source result = catalog.resolve(uri, null);
            assertNotNull(result);
            assertEquals(toURLString(xmlFile),
                         result.getSystemId());
        } catch (Exception e) {
            fail("resolve() failed!" + e.toString());
        }
    }

    public void testNestedCatalog() {

        String publicId = "-//stevo//DTD doc 1.0//EN";
        String dtdLoc = "src/etc/testcases/taskdefs/optional/xml/doc.dtd";

        ResourceLocation dtd = new ResourceLocation();
        dtd.setPublicId(publicId);
        dtd.setLocation(dtdLoc);
        catalog.addDTD(dtd);
        File dtdFile = project.resolveFile(dtdLoc);

        String uri = "http://foo.com/bar/blah.xml";
        String uriLoc = "src/etc/testcases/taskdefs/optional/xml/about.xml";

        ResourceLocation entity = new ResourceLocation();
        entity.setPublicId(uri);
        entity.setLocation(uriLoc);
        catalog.addEntity(entity);
        File xmlFile = project.resolveFile(uriLoc);

        XMLCatalog catalog1 = newCatalog();
        catalog1.addConfiguredXMLCatalog(catalog);

        try {
            InputSource result = catalog1.resolveEntity(publicId,
                                                        "nap:chemical+brothers");
            assertNotNull(result);
            assertEquals(toURLString(dtdFile),
                         result.getSystemId());
        } catch (Exception e) {
            fail("resolveEntity() failed!" + e.toString());
        }

        try {
            Source result = catalog.resolve(uri, null);
            assertNotNull(result);
            assertEquals(toURLString(xmlFile),
                         result.getSystemId());
        } catch (Exception e) {
            fail("resolve() failed!" + e.toString());
        }

    }

    public void testResolverBase() {

        String uri = "http://foo.com/bar/blah.xml";
        String uriLoc = "etc/testcases/taskdefs/optional/xml/about.xml";
        String base = null;
        try {
            base = toURLString(project.getBaseDir()) + "/src/";
        } catch (MalformedURLException ex) {
            fail (ex.toString());
        }

        ResourceLocation entity = new ResourceLocation();
        entity.setPublicId(uri);
        entity.setLocation(uriLoc);
        catalog.addEntity(entity);
        File xmlFile = project.resolveFile("src/" + uriLoc);

        try {
            Source result = catalog.resolve(uri, base);
            assertNotNull(result);
            assertEquals(toURLString(xmlFile),
                         result.getSystemId());
        } catch (Exception e) {
            fail("resolve() failed!" + e.toString());
        }
    }

    public void testClasspath() {


        String publicId = "-//stevo//DTD doc 1.0//EN";
        String dtdLoc = "testcases/taskdefs/optional/xml/doc.dtd";
        String path1 = project.getBaseDir().toString() + "/src/etc";

        ResourceLocation dtd = new ResourceLocation();
        dtd.setPublicId(publicId);
        dtd.setLocation(dtdLoc);
        catalog.addDTD(dtd);
        File dtdFile = project.resolveFile("src/etc/" + dtdLoc);

        String uri = "http://foo.com/bar/blah.xml";
        String uriLoc = "etc/testcases/taskdefs/optional/xml/about.xml";
        String path2 = project.getBaseDir().toString() + "/src";

        ResourceLocation entity = new ResourceLocation();
        entity.setPublicId(uri);
        entity.setLocation(uriLoc);
        catalog.addEntity(entity);
        File xmlFile = project.resolveFile("src/" + uriLoc);

        Path aPath = new Path(project, path1);
        aPath.append(new Path(project, path2));
        catalog.setClasspath(aPath);

        try {
            InputSource result = catalog.resolveEntity(publicId,
                                                       "nap:chemical+brothers");
            assertNotNull(result);
            String resultStr = new URL(result.getSystemId()).getFile();
            assertTrue(toURLString(dtdFile).endsWith(resultStr));
        } catch (Exception e) {
            fail("resolveEntity() failed!" + e.toString());
        }

        try {
            Source result = catalog.resolve(uri, null);
            assertNotNull(result);
            String resultStr = new URL(result.getSystemId()).getFile();
            assertTrue(toURLString(xmlFile).endsWith(resultStr));
        } catch (Exception e) {
            fail("resolve() failed!" + e.toString());
        }
    }
}
