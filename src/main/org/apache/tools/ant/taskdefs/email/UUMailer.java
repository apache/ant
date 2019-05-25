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
package org.apache.tools.ant.taskdefs.email;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.UUEncoder;

/**
 * An emailer that uuencodes attachments.
 *
 * @since Ant 1.5
 */
class UUMailer extends PlainMailer {
    @Override
    protected void attach(File file, PrintStream out)
         throws IOException {
        if (!file.exists() || !file.canRead()) {
            throw new BuildException(
                "File \"%s" + "\" does not exist or is not " + "readable.",
                file.getAbsolutePath());
        }

        try (InputStream in =
            new BufferedInputStream(Files.newInputStream(file.toPath()))) {
            new UUEncoder(file.getName()).encode(in, out);
        }
    }
}

