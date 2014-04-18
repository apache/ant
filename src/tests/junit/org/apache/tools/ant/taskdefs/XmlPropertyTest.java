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

package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 */
public class XmlPropertyTest {
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/xmlproperty.xml");
    }

    @Test
    public void testFile() {
        testProperties("test");
    }

    @Test
    public void testResource() {
        testProperties("testResource");
    }

    private void testProperties(String target) {
        buildRule.executeTarget(target);
        assertEquals("true", buildRule.getProject().getProperty("root-tag(myattr)"));
        assertEquals("Text", buildRule.getProject().getProperty("root-tag.inner-tag"));
        assertEquals("val",
                buildRule.getProject().getProperty("root-tag.inner-tag(someattr)"));
        assertEquals("false", buildRule.getProject().getProperty("root-tag.a2.a3.a4"));
        assertEquals("CDATA failed",
            "<test>", buildRule.getProject().getProperty("root-tag.cdatatag"));
    }

    @Test
    public void testDTD() {
        buildRule.executeTarget("testdtd");
        assertEquals("Text", buildRule.getProject().getProperty("root-tag.inner-tag"));
    }

    @Test
    public void testNone () throws IOException {
        doTest("testNone", false, false, false, false, false);
    }

    @Test
    public void testKeeproot() throws IOException {
        doTest("testKeeproot", true, false, false, false, false);
    }

    @Test
    public void testCollapse () throws IOException {
        doTest("testCollapse", false, true, false, false, false);
    }

    @Test
    public void testSemantic () throws IOException {
        doTest("testSemantic", false, false, true, false, false);
    }

    @Test
    public void testKeeprootCollapse () throws IOException {
        doTest("testKeeprootCollapse", true, true, false, false, false);
    }

    @Test
    public void testKeeprootSemantic () throws IOException {
        doTest("testKeeprootSemantic", true, false, true, false, false);
    }

    @Test
    public void testCollapseSemantic () throws IOException {
        doTest("testCollapseSemantic", false, true, true, false, false);
    }

    @Test
    public void testKeeprootCollapseSemantic () throws IOException {
        doTest("testKeeprootCollapseSemantic", true, true, true, false, false);
    }

    @Test
    public void testInclude () throws IOException {
        doTest("testInclude", false, false, false, true, false);
    }

    @Test
    public void testSemanticInclude () throws IOException {
        doTest("testSemanticInclude", false, false, true, true, false);
    }

    @Test
    public void testSemanticLocal () throws IOException {
        doTest("testSemanticInclude", false, false, true, false, true);
    }

    @Test
    public void testNeedsCatalog() {
        buildRule.executeTarget("testneedscat");
        assertEquals("true", buildRule.getProject().getProperty("skinconfig.foo"));
    }

    /**
     * Actually run a test, finding all input files (and corresponding
     * goldfile)
     */
    private void doTest(String msg, boolean keepRoot, boolean collapse,
                        boolean semantic, boolean include, boolean localRoot) throws IOException {
        Enumeration iter =
            getFiles(new File(System.getProperty("root"), "src/etc/testcases/taskdefs/xmlproperty/inputs"));
        while (iter.hasMoreElements()) {
            File inputFile = (File) iter.nextElement();
            // What's the working directory?  If local, then its the
            // folder of the input file.  Otherwise, its the "current" dir..
            File workingDir;
            if ( localRoot ) {
                workingDir = inputFile.getParentFile();
            } else {
                workingDir = FILE_UTILS.resolveFile(new File("."), ".");
            }


            File propertyFile = getGoldfile(inputFile, keepRoot, collapse,
                                            semantic, include, localRoot);
            if (!propertyFile.exists()) {
//                    System.out.println("Skipping as "
//                                       + propertyFile.getAbsolutePath()
//                                       + ") doesn't exist.");
                continue;
            }

            //                System.out.println(msg + " (" + propertyFile.getName() + ") in (" + workingDir + ")");

            Project p = new Project();

            XmlProperty xmlproperty = new XmlProperty();
            xmlproperty.setProject(p);
            xmlproperty.setFile(inputFile);

            xmlproperty.setKeeproot(keepRoot);
            xmlproperty.setCollapseAttributes(collapse);
            xmlproperty.setSemanticAttributes(semantic);
            xmlproperty.setIncludeSemanticAttribute(include);
            xmlproperty.setRootDirectory(workingDir);

            // Set a property on the project to make sure that loading
            // a property with the same name from an xml file will
            // *not* change it.
            p.setNewProperty("override.property.test", "foo");

            xmlproperty.execute();

            Properties props = new Properties();
            props.load(new FileInputStream(propertyFile));

            //printProperties(p.getProperties());

            ensureProperties(msg, inputFile, workingDir, p, props);
            ensureReferences(msg, inputFile, p.getReferences());

        }
    }

    /**
     * Make sure every property loaded from the goldfile was also
     * read from the XmlProperty.  We could try and test the other way,
     * but some other properties may get set in the XmlProperty due
     * to generic Project/Task configuration.
     */
    private static void ensureProperties (String msg, File inputFile,
                                          File workingDir, Project p,
                                          Properties properties) {
        Hashtable xmlproperties = p.getProperties();
        // Every key identified by the Properties must have been loaded.
        Enumeration propertyKeyEnum = properties.propertyNames();
        while(propertyKeyEnum.hasMoreElements()){
            String currentKey = propertyKeyEnum.nextElement().toString();
            String assertMsg = msg + "-" + inputFile.getName()
                + " Key=" + currentKey;

            String propertyValue = properties.getProperty(currentKey);

            String xmlValue = (String)xmlproperties.get(currentKey);

            if (propertyValue.startsWith("ID.")) {
                // The property is an id's thing -- either a property
                // or a path.  We need to make sure
                // that the object was created with the given id.
                // We don't have an adequate way of testing the actual
                // *value* of the Path object, though...
                String id = currentKey;
                Object obj = p.getReferences().get(id);

                if ( obj == null ) {
                    fail(assertMsg + " Object ID does not exist.");
                }

                // What is the property supposed to be?
                propertyValue =
                    propertyValue.substring(3, propertyValue.length());
                if (propertyValue.equals("path")) {
                    if (!(obj instanceof Path)) {
                        fail(assertMsg + " Path ID is a "
                             + obj.getClass().getName());
                    }
                } else {
                    assertEquals(assertMsg, propertyValue, obj.toString());
                }

            } else {

                if (propertyValue.startsWith("FILE.")) {
                    // The property is the name of a file.  We are testing
                    // a location attribute, so we need to resolve the given
                    // file name in the provided folder.
                    String fileName =
                        propertyValue.substring(5, propertyValue.length());
                    File f = new File(workingDir, fileName);
                    propertyValue = f.getAbsolutePath();
                }

                assertEquals(assertMsg, propertyValue, xmlValue);
            }

        }
    }

    /**
     * Debugging method to print the properties in the given hashtable
     */
    private static void printProperties(Hashtable xmlproperties) {
        Enumeration keyEnum = xmlproperties.keys();
        while (keyEnum.hasMoreElements()) {
            String currentKey = keyEnum.nextElement().toString();
            System.out.println(currentKey + " = "
                               + xmlproperties.get(currentKey));
        }
    }

    /**
     * Ensure all references loaded by the project are valid.
     */
    private static void ensureReferences (String msg, File inputFile,
                                          Hashtable references) {
        Enumeration referenceKeyEnum = references.keys();
        while(referenceKeyEnum.hasMoreElements()){
            String currentKey = referenceKeyEnum.nextElement().toString();
            Object currentValue = references.get(currentKey);

            if (currentValue instanceof Path) {
            } else if (currentValue instanceof String) {
            } else {
                if( ! currentKey.startsWith("ant.") ) {
                    fail(msg + "-" + inputFile.getName() + " Key="
                         + currentKey + " is not a recognized type.");
                }
            }
        }
    }

    /**
     * Munge the name of the input file to find an appropriate goldfile,
     * based on hardwired naming conventions.
     */
    private static File getGoldfile (File input, boolean keepRoot,
                                     boolean collapse, boolean semantic,
                                     boolean include, boolean localRoot) {
        // Substitute .xml with .properties
        String baseName = input.getName().toLowerCase();
        if (baseName.endsWith(".xml")) {
            baseName = baseName.substring(0, baseName.length() - 4)
                + ".properties";
        }

        File dir = input.getParentFile().getParentFile();

        String goldFileFolder = "goldfiles/";

        if (keepRoot) {
            goldFileFolder += "keeproot-";
        } else {
            goldFileFolder += "nokeeproot-";
        }

        if (semantic) {
            goldFileFolder += "semantic-";
            if (include) {
                goldFileFolder += "include-";
            }
        } else {
            if (collapse) {
                goldFileFolder += "collapse-";
            } else {
                goldFileFolder += "nocollapse-";
            }
        }

        return new File(dir, goldFileFolder + baseName);
    }

    /**
     * Retrieve a list of xml files in the specified folder
     * and below.
     */
    private static Enumeration getFiles (final File startingDir) {
        Vector result = new Vector();
        getFiles(startingDir, result);
        return result.elements();
    }

    /**
     * Collect a list of xml files in the specified folder
     * and below.
     */
    private static void getFiles (final File startingDir, Vector collect) {
        FileFilter filter = new FileFilter() {
            public boolean accept (File file) {
                if (file.isDirectory()) {
                    return true;
                } else {
                    return (file.getPath().indexOf("taskdefs") > 0 &&
                            file.getPath().toLowerCase().endsWith(".xml") );
                }
            }
        };

        File[] files = startingDir.listFiles(filter);
        for (int i=0;i<files.length;i++) {
            File f = files[i];
            if (!f.isDirectory()) {
                collect.addElement(f);
            } else {
                getFiles(f, collect);
            }
        }
    }
}
