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

package org.apache.tools.ant;

import java.io.PrintWriter;
import junit.framework.TestCase;
import org.apache.tools.ant.util.StringUtils;

public class DefaultLoggerTest extends TestCase {

    public DefaultLoggerTest(String n) {
        super(n);
    }

    private static String msg(Throwable error, boolean verbose) {
        StringBuffer m = new StringBuffer();
        DefaultLogger.throwableMessage(m, error, verbose);
        return m.toString();
    }

    public void testThrowableMessage() throws Exception { // #43398
        BuildException be = new BuildException("oops", new Location("build.xml", 1, 0));
        assertEquals(
                "build.xml:1: oops" + StringUtils.LINE_SEP,
                msg(be, false));
        be = ProjectHelper.addLocationToBuildException(be, new Location("build.xml", 2, 0));
        assertEquals(
                "build.xml:2: The following error occurred while executing this line:" + StringUtils.LINE_SEP +
                "build.xml:1: oops" + StringUtils.LINE_SEP,
                msg(be, false));
        be = ProjectHelper.addLocationToBuildException(be, new Location("build.xml", 3, 0));
        assertEquals(
                "build.xml:3: The following error occurred while executing this line:" + StringUtils.LINE_SEP +
                "build.xml:2: The following error occurred while executing this line:" + StringUtils.LINE_SEP +
                "build.xml:1: oops" + StringUtils.LINE_SEP,
                msg(be, false));
        Exception x = new Exception("problem") {
            public void printStackTrace(PrintWriter w) {
                w.println("problem");
                w.println("  at p.C.m");
            }
        };
        assertEquals(
                "problem" + StringUtils.LINE_SEP +
                "  at p.C.m" + StringUtils.LINE_SEP,
                msg(x, false));
        be = new BuildException(x, new Location("build.xml", 1, 0));
        assertEquals(
                "build.xml:1: problem" + StringUtils.LINE_SEP +
                "  at p.C.m" + StringUtils.LINE_SEP,
                msg(be, false));
        be = ProjectHelper.addLocationToBuildException(be, new Location("build.xml", 2, 0));
        assertEquals(
                "build.xml:2: The following error occurred while executing this line:" + StringUtils.LINE_SEP +
                "build.xml:1: problem" + StringUtils.LINE_SEP +
                "  at p.C.m" + StringUtils.LINE_SEP,
                msg(be, false));
    }

}
