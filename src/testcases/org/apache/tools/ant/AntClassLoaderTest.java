/*
 * Copyright  2000-2002,2004 The Apache Software Foundation
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

package org.apache.tools.ant;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import junit.framework.TestCase;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;

/**
 * Test case for ant class loader
 *
 */
public class AntClassLoaderTest extends TestCase {

    private Project p;

    public AntClassLoaderTest(String name) {
        super(name);
    }

    public void setUp() {
        p = new Project();
        p.init();
    }

    public void testCleanup() throws BuildException {
        Path path = new Path(p, ".");
        AntClassLoader loader = new AntClassLoader(p, path);
        try {
            // we don't expect to find this
            loader.findClass("fubar");
            fail("Did not expect to find fubar class");
        } catch (ClassNotFoundException e) {
            // ignore expected
        }

        loader.cleanup();
        try {
            // we don't expect to find this
            loader.findClass("fubar");
            fail("Did not expect to find fubar class");
        } catch (ClassNotFoundException e) {
            // ignore expected
        } catch (NullPointerException e) {
            fail("loader should not fail even if cleaned up");
        }

        // tell the build it is finished
        p.fireBuildFinished(null);
        try {
            // we don't expect to find this
            loader.findClass("fubar");
            fail("Did not expect to find fubar class");
        } catch (ClassNotFoundException e) {
            // ignore expected
        } catch (NullPointerException e) {
            fail("loader should not fail even if project finished");
        }
    }
}
