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

import junit.framework.TestCase;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Echo;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceFactory;

/**
 * Tests for org.apache.tools.ant.util.ResourceUtils.
 */
public class ResourceUtilsTest extends TestCase
    implements ResourceFactory, FileNameMapper {

    private Echo taskINeedForLogging = new Echo();

    public ResourceUtilsTest(String name) {
        super(name);
        taskINeedForLogging.setProject(new Project());
    }

    public void testNoDuplicates() {
        Resource r = new Resource("samual vimes", true, 1, false);
        Resource[] toNew =
            ResourceUtils.selectOutOfDateSources(taskINeedForLogging,
                                                 new Resource[] {r},
                                                 this, this);
        assertEquals(1, toNew.length);
    }

    /* ============ ResourceFactory interface ====================== */
    public Resource getResource(String name) {
        return new Resource(name); // implies lastModified == 0
    }

    /* ============ FileNameMapper interface ======================= */
    public void setFrom(String s) {}
    public void setTo(String s) {}
    public String[] mapFileName(String s) {
        return new String[] {"fred colon", "carrot ironfoundersson"};
    }
}
