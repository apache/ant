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

package org.apache.tools.ant.loader;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.CollectionUtils;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AntClassLoader5Test {

    /**
     * Asserts that getResources won't return resources that cannot be
     * seen by AntClassLoader but by ClassLoader.this.parent.
     *
     * @see <a href="https://issues.apache.org/bugzilla/show_bug.cgi?id=46752">
     *     https://issues.apache.org/bugzilla/show_bug.cgi?id=46752</a>
     */
    @Test
    public void testGetResources() throws IOException {
        AntClassLoader acl = new AntClassLoader5(new EmptyLoader(), null,
                                                 new Path(null), true);
        assertNull(acl.getResource("META-INF/MANIFEST.MF"));
        assertFalse(acl.getResources("META-INF/MANIFEST.MF").hasMoreElements());

        // double check using system classloader as parent
        acl = new AntClassLoader5(null, null, new Path(null), true);
        assertNotNull(acl.getResource("META-INF/MANIFEST.MF"));
        assertTrue(acl.getResources("META-INF/MANIFEST.MF").hasMoreElements());
    }

    @Test
    public void testGetResourcesUsingFactory() throws IOException {
        AntClassLoader acl =
            AntClassLoader.newAntClassLoader(new EmptyLoader(), null,
                                             new Path(null), true);
        assertNull(acl.getResource("META-INF/MANIFEST.MF"));
        assertFalse(acl.getResources("META-INF/MANIFEST.MF").hasMoreElements());
    }

    private static class EmptyLoader extends ClassLoader {
        public URL getResource(String n) {
            return null;
        }
        public Enumeration getResources(String n) {
            return new CollectionUtils.EmptyEnumeration();
        }
    }
}