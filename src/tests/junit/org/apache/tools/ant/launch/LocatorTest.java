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

import java.net.URISyntaxException;
import java.io.File;

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
        assertEquals("Different fromURI conversion.\nJava1.4="+j14+"\nJava1.3="+j13+"\n",
                j14, j13);
        return j14;
    }

    private void resolveTo(String uri,String expectedResult) {
        String result = resolve(uri);
        assertResolved(uri, expectedResult, result);
    }

    private void assertResolved(String uri, String expectedResult, String result) {
        assertEquals("Expected "+uri+" to resolve to \n"+expectedResult+"\n but got\n"+result+"\n",
                expectedResult,result);
    }

    /**
     * This asserts that we can round trip the path to a URI and back again
     * @param path
     */
    private void assertResolves(String path) throws Exception {
        String asuri = new File(path).toURI().toASCIIString();
        logURI(path +" => "+asuri);
        resolveTo(asuri,path);
    }

    private void resolveTo13(String uri, String expectedResult) {
        String result = Locator.fromURIJava13(uri);
        assertResolved(uri, expectedResult, result);
    }

    private void logURI(String path) throws URISyntaxException{

        String s = new File(path).toURI().toASCIIString();
        System.out.println(path+" => "+s);

    }

    /**
     * this isnt really a valid URI, except maybe in IE
     * @throws Exception
     */
    public void testNetworkURI() throws Exception {
        resolveTo("file:\\\\PC03\\jclasses\\lib\\ant-1.7.0.jar","\\\\PC03\\jclasses\\lib\\ant-1.7.0.jar");
    }

    public void testTripleForwardSlashNetworkURI() throws Exception {
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


    public void testInternationalURI() throws Exception {
        assertResolves("/L\\u00f6wenbrau/aus/M\\u00fcnchen");
    }

    public void testOddLowAsciiURI() throws Exception {
        assertResolves("/hash#/ and /percent%");
    }


}
