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

package org.example.junitlauncher.vintage;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class ForkedTest {

    public static String SYS_PROP_ONE = "junitlauncher.test.sysprop.one";


    @Test
    public void testSysProp() {
        Assert.assertEquals("Unexpected value for system property",
                "forked", System.getProperty(SYS_PROP_ONE));
    }
}
