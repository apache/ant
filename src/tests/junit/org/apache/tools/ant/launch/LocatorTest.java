/** (C) Copyright 2007 Hewlett-Packard Development Company, LP

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 For more information: www.smartfrog.org

 */
package org.apache.tools.ant.launch;

import junit.framework.TestCase;

/** created 27-Apr-2007 12:26:47 */

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
