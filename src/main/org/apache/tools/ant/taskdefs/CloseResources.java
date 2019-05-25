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

package org.apache.tools.ant.taskdefs;

import java.io.IOException;
import java.net.URL;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.URLProvider;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.util.FileUtils;

/**
 * Not a real task but used during tests.
 *
 * Closes the resources associated with an URL.  In particular this is
 * going to close the jars associated with a jar:file: URL - and it
 * does so in a way that the Java VM still thinks it is open, so use
 * it at your own risk.
 */
public class CloseResources extends Task {
    private Union resources = new Union();

    public void add(ResourceCollection rc) {
        resources.add(rc);
    }

    public void execute() {
        for (Resource r : resources) {
            URLProvider up = r.as(URLProvider.class);
            if (up != null) {
                URL u = up.getURL();
                try {
                    FileUtils.close(u.openConnection());
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
    }
}