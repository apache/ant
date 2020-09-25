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
package org.apache.tools.ant.taskdefs.optional.script.graal;

import org.apache.tools.ant.BuildException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DisableNashornCompatTest extends AbstractNashornCompatTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    public DisableNashornCompatTest() {
        super("true");
    }

    @Test
    @Override
    public void runSquaresTest() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("TypeError");
        super.runSquaresTest();
    }
}
