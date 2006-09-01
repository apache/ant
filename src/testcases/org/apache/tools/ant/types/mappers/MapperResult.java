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

package org.apache.tools.ant.types.mappers;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.util.FileNameMapper;

/**
 * This is a test task to show the result of a mapper
 * on a specific input.
 * (Test is not in the name of the class, to make sure that
 * it is not treated as a unit test.
 */

public class MapperResult extends Task {

    private String failMessage = "";
    private String input;
    private String output;
    private FileNameMapper fileNameMapper;

    /**
     * The output on an empty string array
     */
    private static final String NULL_MAPPER_RESULT = "<NULL>";

    public void setFailMessage(String failMessage) {
        this.failMessage = failMessage;
    }
    
    public void setInput(String input) {
        this.input = input;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public void addConfiguredMapper(Mapper mapper) {
        add(mapper.getImplementation());
    }

    public void add(FileNameMapper fileNameMapper) {
        if (this.fileNameMapper != null) {
            throw new BuildException("Only one mapper type nested element allowed");
        }
        this.fileNameMapper = fileNameMapper;
    }
        
    public void execute() {
        if (input == null) {
            throw new BuildException("Missing attribute 'input'");
        }
        if (output == null) {
            throw new BuildException("Missing attribute 'output'");
        }
        if (fileNameMapper == null) {
            throw new BuildException("Missing a nested file name mapper type element");
        }
        String[] result = fileNameMapper.mapFileName(input);
        String flattened;
        if (result == null) {
            flattened = NULL_MAPPER_RESULT;
        } else {
            StringBuffer b = new StringBuffer();
            for (int i = 0; i < result.length; ++i) {
                if (i != 0) {
                    b.append("|");
                }
                b.append(result[i]);
            }
            flattened = b.toString();
        }
        if (!flattened.equals(output)) {
            throw new BuildException(
                failMessage
                + " "
                + "got "
                + flattened
                + " "
                + "expected "
                + output);
        }
    }
}
