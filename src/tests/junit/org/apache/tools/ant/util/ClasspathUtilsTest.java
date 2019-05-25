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

package org.apache.tools.ant.util;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * Test case for ClasspathUtils
 *
 */
public class ClasspathUtilsTest {

    private Project p;

    @Before
    public void setUp() {
        p = new Project();
        p.init();
    }


    @Test
    public void testOnlyOneInstance() {
        Enumeration<URL> enumeration;
        ClassLoader c = ClasspathUtils.getUniqueClassLoaderForPath(p, null, false);
        try {
            enumeration = c.getResources(
                "org/apache/tools/ant/taskdefs/defaults.properties");
        } catch (IOException e) {
            throw new BuildException(
                "Could not get the defaults.properties resource", e);
        }
        int count = 0;
        StringBuilder list = new StringBuilder();
        while (enumeration.hasMoreElements()) {
            list.append(" ").append(enumeration.nextElement());
            count++;
        }
        assertEquals("Should be only one and not " + count + " " + list, 1, count);
    }
}
