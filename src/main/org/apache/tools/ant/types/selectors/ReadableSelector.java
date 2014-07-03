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

import java.io.File;

import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.selectors.ResourceSelector;

/**
 * A selector that selects readable files.
 *
 * <p>Readable is defined in terms of java.io.File#canRead, this
 * means the selector will accept any file that exists and is readable
 * by the application.</p>
 *
 * @since Ant 1.8.0
 */
public class ReadableSelector implements FileSelector, ResourceSelector {

    public boolean isSelected(File basedir, String filename, File file) {
        return file != null && file.canRead();
    }

    public boolean isSelected(Resource r) {
        FileProvider fp = r.as(FileProvider.class);
        if (fp != null) {
            return isSelected(null, null, fp.getFile());
        }
        return false;
    }
}