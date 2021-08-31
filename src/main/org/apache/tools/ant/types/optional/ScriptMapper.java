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
package org.apache.tools.ant.types.optional;

import java.util.ArrayList;

import org.apache.tools.ant.util.FileNameMapper;

/**
 * Script support at map time.
 * @since Ant1.7
 */
public class ScriptMapper extends AbstractScriptComponent implements FileNameMapper {

    private ArrayList<String> files;


    /**
     * Sets the from part of the transformation rule.
     *
     * @param from a string.
     */
    @Override
    public void setFrom(String from) {
    }

    /**
     * Sets the to part of the transformation rule.
     *
     * @param to a string.
     */
    @Override
    public void setTo(String to) {
    }

    /**
     * Reset the list of files
     */
    public void clear() {
        files = new ArrayList<>(1);
    }

    /**
     * Add a mapped name
     * @param mapping the value to use.
     */
    public void addMappedName(String mapping) {
        files.add(mapping);
    }

    /**
     * Returns an array containing the target filename(s) for the given source
     * file.
     *
     * <p>if the given rule doesn't apply to the source file, implementation
     * must return null. SourceFileScanner will then omit the source file in
     * question.</p>
     *
     * @param sourceFileName the name of the source file relative to some given
     *                       basedirectory.
     * @return an array of strings if the rule applies to the source file, or
     *         null if it does not.
     */

    @Override
    public String[] mapFileName(String sourceFileName) {
        initScriptRunner();
        getRunner().addBean("source", sourceFileName);
        clear();
        executeScript("ant_mapper");
        if (files.isEmpty()) {
            return null;
        }
        return files.toArray(new String[0]);
    }
}
