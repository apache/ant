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
package org.apache.tools.ant.util;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

/**
 * JAXPUtils test case
 */
public class JAXPUtilsTest {

    @Test
    public void testGetSystemId(){
        File file = null;
        if ( File.separatorChar == '\\' ){
            file = new File("d:\\jdk");
        } else {
            file = new File("/user/local/bin");
        }
        String systemid = JAXPUtils.getSystemId(file);
        assertTrue("SystemIDs should start by file:/", systemid.startsWith("file:/"));
        assertTrue("SystemIDs should not start with file:////", !systemid.startsWith("file:////"));
    }
}
