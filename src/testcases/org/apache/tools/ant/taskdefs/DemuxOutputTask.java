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
package org.apache.tools.ant.taskdefs;

import java.util.Random;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * A simple task that prints to System.out and System.err and then catches
 * the output which it then checks. If the output does not match, an
 * exception is thrown
 *
 * @since 1.5
 * @created 21 February 2002
 */
public class DemuxOutputTask extends Task {
    private String randomOutValue;
    private String randomErrValue;
    private boolean outputReceived = false;
    private boolean errorReceived = false;

    public void execute() {
        Random generator = new Random();
        randomOutValue = "Output Value is " + generator.nextInt();
        randomErrValue = "Error Value is " + generator.nextInt();

        System.out.println(randomOutValue);
        System.err.println(randomErrValue);
        if (!outputReceived) {
            throw new BuildException("Did not receive output");
        }

        if (!errorReceived) {
            throw new BuildException("Did not receive error");
        }
    }

    protected void handleOutput(String line) {
        line = line.trim();
        if (line.length() != 0 && !line.equals(randomOutValue)) {
            String message = "Received = [" + line + "], expected = ["
                + randomOutValue + "]";
            throw new BuildException(message);
        }
        outputReceived = true;
    }

    protected void handleErrorOutput(String line) {
        line = line.trim();
        if (line.length() != 0 && !line.equals(randomErrValue)) {
            String message = "Received = [" + line + "], expected = ["
                + randomErrValue + "]";
            throw new BuildException(message);
        }
        errorReceived = true;
    }
}

