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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Properties;

import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 *  JUnit testcase that exercises the optional PropertyFile task in ant.
 *  (this is really more of a functional test so far.., but it's enough to let
 *   me start refactoring...)
 *
 *created    October 2, 2001
 */

public class PropertyFileTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() throws Exception {
        buildRule.configureProject(projectFilePath);
        buildRule.executeTarget("setUp");
        initTestPropFile();
        initBuildPropFile();
        buildRule.configureProject(projectFilePath);
        buildRule.getProject().setProperty(valueDoesNotGetOverwrittenPropertyFileKey,
                valueDoesNotGetOverwrittenPropertyFile);
    }

    @Test
    public void testNonExistingFile() {
        PropertyFile pf = new PropertyFile();
        pf.setProject(buildRule.getProject());
        File file = new File("this-file-does-not-exist.properties");
        pf.setFile(file);
        assertFalse("Properties file exists before test.", file.exists());
        pf.execute();
        assertTrue("Properties file does not exist after test.", file.exists());
        file.delete();
    }

    /**
     *  A unit test for JUnit- Exercises the propertyfile tasks ability to
     *  update properties that are already defined-
     */
    @Test
    public void testUpdatesExistingProperties() throws Exception {
        Properties beforeUpdate = getTestProperties();
        assertEquals(FNAME, beforeUpdate.getProperty(FNAME_KEY));
        assertEquals(LNAME, beforeUpdate.getProperty(LNAME_KEY));
        assertEquals(EMAIL, beforeUpdate.getProperty(EMAIL_KEY));
        assertNull(beforeUpdate.getProperty(PHONE_KEY));
        assertNull(beforeUpdate.getProperty(AGE_KEY));
        assertNull(beforeUpdate.getProperty(DATE_KEY));

        // ask ant to update the properties...
        buildRule.executeTarget("update-existing-properties");

        Properties afterUpdate = getTestProperties();
        assertEquals(NEW_FNAME, afterUpdate.getProperty(FNAME_KEY));
        assertEquals(NEW_LNAME, afterUpdate.getProperty(LNAME_KEY));
        assertEquals(NEW_EMAIL, afterUpdate.getProperty(EMAIL_KEY));
        assertEquals(NEW_PHONE, afterUpdate.getProperty(PHONE_KEY));
        assertEquals(NEW_AGE, afterUpdate.getProperty(AGE_KEY));
        assertEquals(NEW_DATE, afterUpdate.getProperty(DATE_KEY));
    }

    @Test
    public void testDeleteProperties() throws Exception {
        Properties beforeUpdate = getTestProperties();
        assertEquals("Property '" + FNAME_KEY + "' should exist before deleting",
            FNAME, beforeUpdate.getProperty(FNAME_KEY));
        assertEquals("Property '" + LNAME_KEY + "' should exist before deleting",
            LNAME, beforeUpdate.getProperty(LNAME_KEY));

        buildRule.executeTarget("delete-properties");
        Properties afterUpdate = getTestProperties();

        assertEquals("Property '" + LNAME_KEY + "' should exist after deleting",
            LNAME, afterUpdate.getProperty(LNAME_KEY));
        assertNull("Property '" + FNAME_KEY + "' should be deleted",
            afterUpdate.getProperty(FNAME_KEY));
    }

    @Test
    public void testExerciseDefaultAndIncrement() {
        buildRule.executeTarget("exercise");
        assertEquals("3", buildRule.getProject().getProperty("int.with.default"));
        assertEquals("1", buildRule.getProject().getProperty("int.without.default"));
        assertEquals("-->", buildRule.getProject().getProperty("string.with.default"));
        assertEquals(".", buildRule.getProject().getProperty("string.without.default"));
        assertEquals("2002/01/21 12:18", buildRule.getProject().getProperty("ethans.birth"));
        assertEquals("2003/01/21", buildRule.getProject().getProperty("first.birthday"));
        assertEquals("0124", buildRule.getProject().getProperty("olderThanAWeek"));
        assertEquals("37", buildRule.getProject().getProperty("existing.prop"));
        assertEquals("6", buildRule.getProject().getProperty("int.without.value"));
    }

    @Test
    public void testValueDoesNotGetOverwritten() {
        // this test shows that the bug report 21505 is fixed
        buildRule.executeTarget("bugDemo1");
        buildRule.executeTarget("bugDemo2");
        assertEquals("5", buildRule.getProject().getProperty("foo"));
    }

    @Test
    public void testDirect() throws Exception {
        PropertyFile pf = new PropertyFile();
        pf.setProject(buildRule.getProject());
        pf.setFile(new File(buildRule.getOutputDir(), testPropsFilePath));

        long delta = 123L;
        PropertyFile.Entry entry = pf.createEntry();
        entry.setKey("date");
        entry.setValue(String.valueOf(delta));

        PropertyFile.Entry.Type type = new PropertyFile.Entry.Type();
        type.setValue("date");
        entry.setType(type);
        entry.setPattern("yyyy/MM/dd");

        PropertyFile.Entry.Operation operation = new PropertyFile.Entry.Operation();
        operation.setValue("+");
        entry.setOperation(operation);
        pf.execute();

        Properties props = getTestProperties();
        LocalDate currentDate = LocalDate.now().plusDays(delta);
        assertEquals(String.format("%d/%02d/%02d", currentDate.getYear(), currentDate.getMonthValue(),
                currentDate.getDayOfMonth()), props.getProperty("date"));
    }


    private Properties getTestProperties() throws Exception {
        Properties testProps = new Properties();
        FileInputStream propsFile = new FileInputStream(new File(buildRule.getOutputDir(), testPropsFilePath));
        testProps.load(propsFile);
        propsFile.close();
        return testProps;
    }


    private void initTestPropFile() throws IOException {
        Properties testProps = new Properties();
        testProps.put(FNAME_KEY, FNAME);
        testProps.put(LNAME_KEY, LNAME);
        testProps.put(EMAIL_KEY, EMAIL);
        testProps.put("existing.prop", "37");

        FileOutputStream fos = new FileOutputStream(new File(buildRule.getOutputDir(), testPropsFilePath));
        testProps.store(fos, "defaults");
        fos.close();
    }


    private void initBuildPropFile() throws IOException {
        Properties buildProps = new Properties();
        buildProps.put(testPropertyFileKey, testPropertyFile);
        buildProps.put(FNAME_KEY, NEW_FNAME);
        buildProps.put(LNAME_KEY, NEW_LNAME);
        buildProps.put(EMAIL_KEY, NEW_EMAIL);
        buildProps.put(PHONE_KEY, NEW_PHONE);
        buildProps.put(AGE_KEY, NEW_AGE);
        buildProps.put(DATE_KEY, NEW_DATE);

        FileOutputStream fos = new FileOutputStream(new File(buildRule.getOutputDir(), buildPropsFilePath));
        buildProps.store(fos, null);
        fos.close();
    }

    @SuppressWarnings("unused")
    private static final String
        projectFilePath     = "src/etc/testcases/taskdefs/optional/propertyfile.xml",

        testPropertyFile    = "propertyfile.test.properties",
        testPropertyFileKey = "test.propertyfile",
        testPropsFilePath   = testPropertyFile,

        valueDoesNotGetOverwrittenPropertyFile    = "overwrite.test.properties",
        valueDoesNotGetOverwrittenPropertyFileKey = "overwrite.test.propertyfile",
        valueDoesNotGetOverwrittenPropsFilePath   = valueDoesNotGetOverwrittenPropertyFile,

        buildPropsFilePath  = "propertyfile.build.properties",

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
