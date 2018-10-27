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

package org.apache.tools.ant.types.selectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.UserPrincipal;

import org.apache.tools.ant.taskdefs.condition.Os;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class OwnedBySelectorTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void ownedByIsTrueForSelf() throws Exception {
        // at least on Jenkins the file is owned by "BUILTIN\Administrators"
        Assume.assumeFalse(Os.isFamily("windows"));
        String self = System.getProperty("user.name");
        File file = folder.newFile("f.txt");
        UserPrincipal user = Files.getOwner(file.toPath());
        assertEquals(self, user.getName());

        OwnedBySelector s = new OwnedBySelector();
        s.setOwner(self);
        assertTrue(s.isSelected(null, null, file));
    }

}
