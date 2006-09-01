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
package org.apache.tools.ant.taskdefs.optional.perforce;

import junit.framework.TestCase;
import org.apache.oro.text.perl.Perl5Util;

/**
 * Basic testcase to ensure that backslashing is OK.
 */
public class P4ChangeTest extends TestCase {

    protected P4Change p4change;

    public P4ChangeTest(String s) {
        super(s);
    }

    protected void setUp() throws Exception {
        p4change = new P4Change();
    }

    public void testBackslash(){
        String input = "comment with a / inside";
        String output = P4Change.backslash(input);
        assertEquals("comment with a \\/ inside", output);
    }

    public void testSubstitute(){
        Perl5Util util = new Perl5Util();
        String tosubstitute = "xx<here>xx";
        String input = P4Change.backslash("/a/b/c/");
        String output = util.substitute("s/<here>/" + input + "/", tosubstitute);
        assertEquals("xx/a/b/c/xx", output);
    }

}
