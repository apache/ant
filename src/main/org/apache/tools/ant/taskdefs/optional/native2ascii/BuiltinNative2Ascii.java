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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.util.function.UnaryOperator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.Native2Ascii;
import org.apache.tools.ant.types.CharSet;
import org.apache.tools.ant.util.Native2AsciiUtils;

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
        CharSet charSet = args.getCharSet();
        try (BufferedReader input = getReader(srcFile, charSet, reverse);
             Writer output = getWriter(destFile, charSet, reverse)) {
            translate(input, output, reverse ? Native2AsciiUtils::ascii2native
                : Native2AsciiUtils::native2ascii);
            return true;
        } catch (IOException ex) {
            throw new BuildException("Exception trying to translate data", ex);
        }
    }

    private BufferedReader getReader(File srcFile, CharSet charSet,
                                     boolean reverse) throws IOException {
        return reverse ? new BufferedReader(new FileReader(srcFile))
                : new BufferedReader(new InputStreamReader(Files.newInputStream(srcFile.toPath()),
                charSet.getCharset()));
    }

    private Writer getWriter(File destFile, CharSet charSet,
                             boolean reverse) throws IOException {
        if (!reverse) {
            charSet = CharSet.getAscii();
        }
        return new BufferedWriter(
            new OutputStreamWriter(Files.newOutputStream(destFile.toPath()),
                                   charSet.getCharset()));
    }

    private void translate(BufferedReader input, Writer output,
        UnaryOperator<String> translation) throws IOException {
        for (String line : (Iterable<String>) () -> input.lines()
            .map(translation).iterator()) {
            output.write(String.format("%s%n", line));
        }
    }
}
