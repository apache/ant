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
package org.apache.tools.ant.taskdefs.optional.native2ascii;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.Native2Ascii;

/**
 * Interface for an adapter to a native2ascii implementation.
 *
 * @since Ant 1.6.3
 */
// CheckStyle:HideUtilityClassConstructorCheck OFF (bc)
public interface Native2AsciiAdapter {
    /**
     * Convert the encoding of srcFile writing to destFile.
     *
     * @param args Task that holds command line arguments and allows
     * the implementation to send messages to Ant's logging system
     * @param srcFile the source to convert
     * @param destFile where to send output to
     * @return whether the conversion has been successful.
     * @throws BuildException if there was a problem.
     */
    boolean convert(Native2Ascii args, File srcFile, File destFile)
        throws BuildException;
}
