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

package org.apache.tools.ant;

import java.io.File;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;
import org.junit.Test;

/**
 * Used to verify the performance effect of classloader changes.
 */
public class AntClassLoaderPerformance  {

    @Test
    public void testFindClass() throws Exception {
        String testCaseURL = getClass()
            .getClassLoader().getResource("junit/framework/TestCase.class")
            .toExternalForm();
        int pling = testCaseURL.indexOf('!');
        String jarName = testCaseURL.substring(4, pling);
        File f = new File(FileUtils.getFileUtils().fromURI(jarName));
        Path p = new Path(null);
        p.createPathElement().setLocation(f);
        AntClassLoader al = null;
        for (int i = 0; i < 1000; i++) {
            try {
                // not using factory method so the test can run on Ant
                // 1.7.1 as well
                al = new AntClassLoader(null, null, p, false);
                al.findClass("junit.framework.TestCase");
            } finally {
                if (al != null) {
                    al.cleanup();
                }
            }
        }
    }

}
