/*
 *  The Apache Software License, Version 1.1
 *
 *  Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
 *  reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution, if
 *  any, must include the following acknowlegement:
 *  "This product includes software developed by the
 *  Apache Software Foundation (http://www.apache.org/)."
 *  Alternately, this acknowlegement may appear in the software itself,
 *  if and wherever such third-party acknowlegements normally appear.
 *
 *  4. The names "Ant" and "Apache Software
 *  Foundation" must not be used to endorse or promote products derived
 *  from this software without prior written permission. For written
 *  permission, please contact apache@apache.org.
 *
 *  5. Products derived from this software may not be called "Apache"
 *  nor may "Apache" appear in their names without prior written
 *  permission of the Apache Group.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of the Apache Software Foundation.  For more
 *  information on the Apache Software Foundation, please see
 *  <http://www.apache.org/>.
 */
package org.apache.tools.ant.taskdefs.optional;

import org.apache.tools.ant.BuildFileTest;

import java.util.Properties;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.File;

/**
 *  JUnit testcase that excercises the optional PropertyFile task in ant.
 *  (this is really more of a functional test so far.., but it's enough to let
 *   me start refactoring...)
 *
 *@author     Levi Cook <levicook@papajo.com>
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
    }


    /**
     *  The JUnit tearDown method
     */
    public void tearDown() {
        destroyTempFiles();
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

/*
    public void testDirect() throws Exception {
        PropertyFile pf = new PropertyFile();
        pf.setProject(project);
        pf.setFile(new File(testPropsFilePath));
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
        FileInputStream propsFile = new FileInputStream(testPropsFilePath);
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
      
        FileOutputStream fos = new FileOutputStream(testPropsFilePath);
        testProps.save(fos, "defaults");
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
      
        FileOutputStream fos = new FileOutputStream(buildPropsFilePath);
        buildProps.save(fos, null);
        fos.close();
    }


    private void destroyTempFiles() {
        File tempFile = new File(testPropsFilePath);
        tempFile.delete();
        tempFile = null;

        tempFile = new File(buildPropsFilePath);
        tempFile.delete();
        tempFile = null;
    }
   


    private static final String 
        projectFilePath     = "src/etc/testcases/taskdefs/optional/propertyfile.xml",
      
        testPropertyFile    = "propertyfile.test.properties",
        testPropertyFileKey = "test.propertyfile",
        testPropsFilePath   = "src/etc/testcases/taskdefs/optional/" + testPropertyFile,
      
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

