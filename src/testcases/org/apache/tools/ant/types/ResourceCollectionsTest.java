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

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildFileTest;

public class ResourceCollectionsTest extends BuildFileTest {

    public ResourceCollectionsTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/types/resources/build.xml");
    }

    public void tearDown() {
        executeTarget("tearDown");
    }

    public void testdifference() {
        executeTarget("testdifference");
    }

    public void testdirset() {
        executeTarget("testdirset");
    }

    public void testfile() {
        executeTarget("testfile");
    }

    public void testfilelist() {
        executeTarget("testfilelist");
    }

    public void testfiles1() {
        executeTarget("testfiles1");
    }

    public void testfiles2() {
        executeTarget("testfiles2");
    }

    public void testfiles3() {
        executeTarget("testfiles3");
    }

    public void testfileset() {
        executeTarget("testfileset");
    }

    public void testfileurl() {
        executeTarget("testfileurl");
    }

    public void testfileurlref() {
        executeTarget("testfileurlref");
    }

    public void testfirst1() {
        executeTarget("testfirst1");
    }

    public void testfirst2() {
        executeTarget("testfirst2");
    }

    public void testhttpurl1() {
        executeTarget("testhttpurl1");
    }

    public void testhttpurl2() {
        executeTarget("testhttpurl2");
    }

    public void testintersect() {
        executeTarget("testintersect");
    }

    public void testjarurl() {
        executeTarget("testjarurl");
    }

    public void testnestedresources() {
        executeTarget("testnestedresources");
    }

    public void testpath() {
        executeTarget("testpath");
    }

    public void testpropertyset() {
        executeTarget("testpropertyset");
    }

    public void testresource() {
        executeTarget("testresource");
    }

    public void testresourcesref() {
        executeTarget("testresourcesref");
    }

    public void teststring1() {
        executeTarget("teststring1");
    }

    public void teststring2() {
        executeTarget("teststring2");
    }

    public void testunion() {
        executeTarget("testunion");
    }

    public void testzipentry() {
        executeTarget("testzipentry");
    }

    public void testzipfileset() {
        executeTarget("testzipfileset");
    }

}
