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

package org.apache.tools.ant.launch;

import junit.framework.TestCase;

/** Test the locator in the ant-launch JAR */

public class LocatorTest extends TestCase {


    /**
     * No-arg constructor to enable serialization. This method is not intended to be used by mere mortals without calling
     * setName().
     */
    public LocatorTest() {
    }

    /** Constructs a test case with the given name. */
    public LocatorTest(String name) {
        super(name);
    }

    private String resolve(String uri) {
        String j14= Locator.fromURI(uri);
        String j13 = Locator.fromURIJava13(uri);
        assertEquals(uri,j14,j13);
        return j14;
    }

    private void resolveTo(String uri,String expectedResult) {
        String result = resolve(uri);
        assertEquals(uri,expectedResult,result);
    }

    private void resolveTo13(String uri, String expectedResult) {
        String result = Locator.fromURIJava13(uri);
        assertEquals(uri, expectedResult, result);
    }
    /**
     * this isnt really a valid URI, except maybe in IE
     * @throws Exception
     */
    public void testNetworkURI() throws Exception {
        resolveTo("file:\\\\PC03\\jclasses\\lib\\ant-1.7.0.jar","\\\\PC03\\jclasses\\lib\\ant-1.7.0.jar");
    }

    public void testTripleForwardSlashNetworkURI_BugID_42275() throws Exception {
        resolveTo("file:///PC03/jclasses/lib/ant-1.7.0.jar", "///PC03/jclasses/lib/ant-1.7.0.jar");
    }

    public void testUnixNetworkPath() throws Exception {
        resolveTo("file://cluster/home/ant/lib", "//cluster/home/ant/lib");
    }

    public void testUnixNetworkPath13() throws Exception {
        resolveTo13("file://cluster/home/ant/lib", "//cluster/home/ant/lib");
    }

    public void testUnixPath() throws Exception {
        resolveTo("file:/home/ant/lib", "/home/ant/lib");
    }

    public void testSpacedURI() throws Exception {
        resolveTo("file:C:\\Program Files\\Ant\\lib","C:\\Program Files\\Ant\\lib");
    }

    public void testHttpURI() throws Exception {
        String url = "http://ant.apache.org";
        try {
            Locator.fromURI(url);
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            assertTrue(message,message.indexOf(Locator.ERROR_NOT_FILE_URI)>=0);
            assertTrue(message, message.indexOf(url) >= 0);
        }
    }





}
