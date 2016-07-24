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
package org.apache.tools.ant.taskdefs.optional.native2ascii;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.Native2Ascii;
import org.apache.tools.ant.util.Native2AsciiUtils;
import org.apache.tools.ant.util.StringUtils;

/**
 * Encapsulates the built-in Native2Ascii implementation.
 *
 * @since Ant 1.9.8
 */
public class BuiltinNative2Ascii implements Native2AsciiAdapter {

    static final String IMPLEMENTATION_NAME = "builtin";

    @Override
    public final boolean convert(Native2Ascii args, File srcFile,
                                 File destFile) throws BuildException {
        boolean reverse = args.getReverse();
        String encoding = args.getEncoding();
        try (BufferedReader input = getReader(srcFile, encoding, reverse);
             Writer output = getWriter(destFile, encoding, reverse)) {
            translate(input, output, reverse);
            return true;
        } catch (IOException ex) {
            throw new BuildException("Exception trying to translate data", ex);
        }
    }

    private BufferedReader getReader(File srcFile, String encoding,
                                     boolean reverse) throws IOException {
        if (!reverse && encoding != null) {
            return new BufferedReader(new InputStreamReader(
                new FileInputStream(srcFile), encoding));
        }
        return new BufferedReader(new FileReader(srcFile));
    }

    private Writer getWriter(File destFile, String encoding,
                             boolean reverse) throws IOException {
        if (!reverse) {
            encoding = "ASCII";
        }
        if (encoding != null) {
            return new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(destFile),
                                       encoding));
        }
        return new BufferedWriter(new FileWriter(destFile));
    }

    private void translate(BufferedReader input, Writer output,
                           boolean reverse) throws IOException {
        String line = null;
        while ((line = input.readLine()) != null) {
            if (!reverse) {
                output.write(Native2AsciiUtils.native2ascii(line));
            } else {
                output.write(Native2AsciiUtils.ascii2native(line));
            }
            output.write(StringUtils.LINE_SEP);
        }
    }
}
