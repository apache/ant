/* 
 * Copyright  2001,2003-2004 Apache Software Foundation
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
package org.apache.tools.ant.taskdefs.optional.sitraka;

import java.util.Hashtable;
import java.io.File;

import junit.framework.TestCase;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.taskdefs.optional.sitraka.bytecode.ClassPathLoader;

/**
 * Minimal testing for the classpath loader..
 *
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public class ClassPathLoaderTest extends TestCase {
    public ClassPathLoaderTest(String s) {
        super(s);
    }

    public void testgetClasses() throws Exception {
        // good performance test...load all classes in rt.jar
        Path p = new Path(null);
        p.addJavaRuntime();
        ClassPathLoader cl = new ClassPathLoader(p.toString());
        Hashtable map = cl.getClasses();
        assertTrue(map.size() > 0);
    }

}
