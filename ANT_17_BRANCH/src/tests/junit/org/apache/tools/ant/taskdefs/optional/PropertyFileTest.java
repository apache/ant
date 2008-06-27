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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import org.apache.tools.ant.BuildFileTest;

/**
 *  JUnit testcase that excercises the optional PropertyFile task in ant.
 *  (this is really more of a functional test so far.., but it's enough to let
 *   me start refactoring...)
 *
 *@created    October 2, 2001
 */

public class PropertyFileTest extends BuildFileTest {

    public PropertyFileTest(String name) {
        super(name);
    }


    /**
     *  The JUnit setup method
     */
    public void setUp() throws Exception {
        destroyTempFiles();
        initTestPropFile();
        initBuildPropFile();
        configureProject(projectFilePath);
        project.setProperty(valueDoesNotGetOverwrittenPropertyFileKey,valueDoesNotGetOverwrittenPropertyFile);
    }


    /**
     *  The JUnit tearDown method
     */
    public void tearDown() {
        destroyTempFiles();
    }

    public void testNonExistingFile() {
        PropertyFile props = new PropertyFile();
        props.setProject( getProject() );
        File file = new File("this-file-does-not-exist.properties");
        props.setFile(file);
        assertFalse("Properties file exists before test.", file.exists());
        props.execute();
        assertTrue("Properties file does not exist after test.", file.exists());
        file.delete();
    }

    /**
     *  A unit test for JUnit- Excercises the propertyfile tasks ability to
     *  update properties that are already defined-
     */
    public void testUpdatesExistingProperties() throws Exception {
        Properties beforeUpdate = getTestProperties();
        assertEquals(FNAME, beforeUpdate.getProperty(FNAME_KEY));
        assertEquals(LNAME, beforeUpdate.getProperty(LNAME_KEY));
        assertEquals(EMAIL, beforeUpdate.getProperty(EMAIL_KEY));
        assertEquals(null, beforeUpdate.getProperty(PHONE_KEY));
        assertEquals(null, beforeUpdate.getProperty(AGE_KEY));
        assertEquals(null, beforeUpdate.getProperty(DATE_KEY));

        // ask ant to update the properties...
        executeTarget("update-existing-properties");

        Properties afterUpdate = getTestProperties();
        assertEquals(NEW_FNAME, afterUpdate.getProperty(FNAME_KEY));
        assertEquals(NEW_LNAME, afterUpdate.getProperty(LNAME_KEY));
        assertEquals(NEW_EMAIL, afterUpdate.getProperty(EMAIL_KEY));
        assertEquals(NEW_PHONE, afterUpdate.getProperty(PHONE_KEY));
        assertEquals(NEW_AGE, afterUpdate.getProperty(AGE_KEY));
        assertEquals(NEW_DATE, afterUpdate.getProperty(DATE_KEY));
    }

    public void testExerciseDefaultAndIncrement() throws Exception {
        executeTarget("exercise");
        assertEquals("3",project.getProperty("int.with.default"));
        assertEquals("1",project.getProperty("int.without.default"));
        assertEquals("-->",project.getProperty("string.with.default"));
        assertEquals(".",project.getProperty("string.without.default"));
        assertEquals("2002/01/21 12:18", project.getProperty("ethans.birth"));
        assertEquals("2003/01/21", project.getProperty("first.birthday"));
        assertEquals("0124", project.getProperty("olderThanAWeek"));
        assertEquals("37", project.getProperty("existing.prop"));
        assertEquals("6",project.getProperty("int.without.value"));
    }

    public void testValueDoesNotGetOverwritten() {
        // this test shows that the bug report 21505 is fixed
        executeTarget("bugDemo1");
        executeTarget("bugDemo2");
        assertEquals("5", project.getProperty("foo"));
    }
/*
    public void testDirect() throws Exception {
        PropertyFile pf = new PropertyFile();
        pf.setProject(project);
        pf.setFile(new File(System.getProperty("root"), testPropsFilePath));
        PropertyFile.Entry entry = pf.createEntry();

        entry.setKey("date");
        entry.setValue("123");
        PropertyFile.Entry.Type type = new PropertyFile.Entry.Type();
        type.setValue("date");
        entry.setType(type);

        entry.setPattern("yyyy/MM/dd");

        PropertyFile.Entry.Operation operation = new PropertyFile.Entry.Operation();
        operation.setValue("+");
        pf.execute();

        Properties props = getTestProperties();
        assertEquals("yeehaw", props.getProperty("date"));
    }
*/

    private Properties getTestProperties() throws Exception {
        Properties testProps = new Properties();
        FileInputStream propsFile = new FileInputStream(new File(System.getProperty("root"), testPropsFilePath));
        testProps.load(propsFile);
        propsFile.close();
        return testProps;
    }


    private void initTestPropFile() throws Exception {
        Properties testProps = new Properties();
        testProps.put(FNAME_KEY, FNAME);
        testProps.put(LNAME_KEY, LNAME);
        testProps.put(EMAIL_KEY, EMAIL);
        testProps.put("existing.prop", "37");

        FileOutputStream fos = new FileOutputStream(new File(System.getProperty("root"), testPropsFilePath));
        testProps.store(fos, "defaults");
        fos.close();
    }


    private void initBuildPropFile() throws Exception {
        Properties buildProps = new Properties();
        buildProps.put(testPropertyFileKey, testPropertyFile);
        buildProps.put(FNAME_KEY, NEW_FNAME);
        buildProps.put(LNAME_KEY, NEW_LNAME);
        buildProps.put(EMAIL_KEY, NEW_EMAIL);
        buildProps.put(PHONE_KEY, NEW_PHONE);
        buildProps.put(AGE_KEY, NEW_AGE);
        buildProps.put(DATE_KEY, NEW_DATE);

        FileOutputStream fos = new FileOutputStream(new File(System.getProperty("root"), buildPropsFilePath));
        buildProps.store(fos, null);
        fos.close();
    }


    private void destroyTempFiles() {
        File tempFile = new File(System.getProperty("root"), testPropsFilePath);
        tempFile.delete();
        tempFile = null;

        tempFile = new File(System.getProperty("root"), buildPropsFilePath);
        tempFile.delete();
        tempFile = null;

        tempFile = new File(System.getProperty("root"), valueDoesNotGetOverwrittenPropsFilePath);
        tempFile.delete();
        tempFile = null;
    }



    private static final String
        projectFilePath     = "src/etc/testcases/taskdefs/optional/propertyfile.xml",

        testPropertyFile    = "propertyfile.test.properties",
        testPropertyFileKey = "test.propertyfile",
        testPropsFilePath   = "src/etc/testcases/taskdefs/optional/" + testPropertyFile,

        valueDoesNotGetOverwrittenPropertyFile    = "overwrite.test.properties",
        valueDoesNotGetOverwrittenPropertyFileKey = "overwrite.test.propertyfile",
        valueDoesNotGetOverwrittenPropsFilePath   = "src/etc/testcases/taskdefs/optional/" + valueDoesNotGetOverwrittenPropertyFile,

        buildPropsFilePath  = "src/etc/testcases/taskdefs/optional/propertyfile.build.properties",

        FNAME     = "Bruce",
        NEW_FNAME = "Clark",
        FNAME_KEY = "firstname",

        LNAME     = "Banner",
        NEW_LNAME = "Kent",
        LNAME_KEY = "lastname",

        EMAIL     = "incredible@hulk.com",
        NEW_EMAIL = "kc@superman.com",
        EMAIL_KEY = "email",

        NEW_PHONE = "(520) 555-1212",
        PHONE_KEY = "phone",

        NEW_AGE = "30",
        AGE_KEY = "age",

        NEW_DATE = "2001/01/01 12:45",
        DATE_KEY = "date";
}
