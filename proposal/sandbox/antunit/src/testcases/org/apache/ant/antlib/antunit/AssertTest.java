/*
 * Copyright  2005 The Apache Software Foundation
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
package org.apache.ant.antlib.antunit;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileTest;

public class AssertTest extends BuildFileTest {

    public AssertTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/assert.xml");
    }

    public void testTruePass() {
        testPass("assertTruePass");
    }
    public void testFalsePass() {
        testPass("assertFalsePass");
    }
    public void testEqualsPass() {
        testPass("assertEqualsPass");
    }
    public void testEqualsCasePass() {
        testPass("assertEqualsCasePass");
    }
    public void testPropertySetPass() {
        testPass("assertPropertySetPass");
    }
    public void testPropertyEqualsPass() {
        testPass("assertPropertyEqualsPass");
    }
    public void testPropertyEqualsCasePass() {
        testPass("assertPropertyEqualsCasePass");
    }
    public void testFileExistsPass() {
        testPass("assertFileExistsPass");
    }
    public void testFileDoesntExistPass() {
        testPass("assertFileDoesntExistPass");
    }
    public void testDestIsUptodatePass() {
        testPass("assertDestIsUptodatePass");
    }
    public void testDestIsOutofdatePass() {
        testPass("assertDestIsOutofdatePass");
    }

    public void testTrueFail() {
        testFail("assertTrueFail");
    }
    public void testFalseFail() {
        testFail("assertFalseFail");
    }
    public void testEqualsFail1() {
        testFail("assertEqualsFail1", "Expected 'bar' but was 'baz'");
    }
    public void testEqualsFail2() {
        testFail("assertEqualsFail2", "Expected 'bar' but was 'BAR'");
    }
    public void testPropertySetFail() {
        testFail("assertPropertySetFail", "Expected property 'foo'");
    }
    public void testPropertyEqualsFail1() {
        testFail("assertPropertyEqualsFail1", "Expected property 'foo' to have value 'bar' but was '${foo}'");
    }
    public void testPropertyEqualsFail2() {
        testFail("assertPropertyEqualsFail2", "Expected property 'foo' to have value 'baz' but was 'bar'");
    }
    public void testPropertyEqualsFail3() {
        testFail("assertPropertyEqualsFail3", "Expected property 'foo' to have value 'BAR' but was 'bar'");
    }
    public void testFileExistsFail() {
        testFail("assertFileExistsFail",
                 "Expected file 'assert.txt' to exist");
    }
    public void testFileDoesntExistFail() {
        testFail("assertFileDoesntExistFail",
                 "Didn't expect file 'assert.xml' to exist");
    }
    public void testDestIsUptodateFail() {
        testFail("assertDestIsUptodateFail",
                 "Expected '../../main/org/apache/ant/antlib/antunit/AssertTask.java' to be more recent than '../../../build/classes/org/apache/ant/antlib/antunit/AssertTask.class'");
    }
    public void testDestIsOutofdateFail() {
        testFail("assertDestIsOutofdateFail",
                 "Expected '../../main/org/apache/ant/antlib/antunit/AssertTask.java' to be more recent than '../../../build/classes/org/apache/ant/antlib/antunit/AssertTask.class'");
    }


    private void testPass(String target) {
        executeTarget(target);
    }

    private void testFail(String target) {
        testFail(target, "Assertion failed");
    }

    private void testFail(String target, String message) {
        try {
            executeTarget(target);
            fail("Expected failed assetion");
        } catch (AssertionFailedException e) {
            assertEquals(message, e.getMessage());
        } catch (BuildException e) {
            // depending on the number of macrodef indirections, this
            // can become arbitrarily deep
            while (true) {
                Throwable t = e.getCause();
                assertNotNull(t);
                assertTrue("nested is a BuildException",
                           t instanceof BuildException);
                if (t instanceof AssertionFailedException) {
                    assertEquals(message, e.getMessage());
                    break;
                }
                e = (BuildException) t;
            }
        } // end of try-catch
    }
}