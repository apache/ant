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

package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.IOException;
import java.io.OutputStream;

import junit.framework.AssertionFailedError;
import junit.framework.Test;

import org.apache.tools.ant.BuildException;

public class TestFormatter implements JUnitResultFormatter {

    private static final byte[] grafitto = new byte[] {
        (byte) 'T', (byte) 'e', (byte) 's', (byte) 't', (byte) 'F', (byte) 'o',
        (byte) 'r', (byte) 'm', (byte) 'a', (byte) 't', (byte) 't', (byte) 'e',
        (byte) 'r', (byte) ' ', (byte) 'w', (byte) 'a', (byte) 's', (byte) ' ',
        (byte) 'h', (byte) 'e', (byte) 'r', (byte) 'e', 10
    };

    /**
     * Where to write the log to.
     */
    private OutputStream out;

    /**
     * Empty
     */
    public TestFormatter() {
    }

    /**
     * Empty
     */
    public void startTestSuite(JUnitTest suite) {
    }
    /**
     * Empty
     */
    public void startTest(Test t) {
    }
    /**
     * Empty
     */
    public void endTest(Test test) {
    }
    /**
     * Empty
     */
    public void addFailure(Test test, Throwable t) {
    }
    /**
     * Empty
     */
    public void addFailure(Test test, AssertionFailedError t) {
    }
    /**
     * Empty
     */
    public void addError(Test test, Throwable t) {
    }
    /**
     * Empty
     */
    public void setSystemOutput(String out) {
    }
    /**
     * Empty
     */
    public void setSystemError(String err) {
    }

    public void setOutput(OutputStream out) {
        this.out = out;
    }

    public void endTestSuite(JUnitTest suite) throws BuildException {
        if (out != null) {
            try {
                out.write(grafitto);
                out.flush();
            } catch (IOException ioex) {
                throw new BuildException("Unable to write output", ioex);
            } finally {
                if (out != System.out && out != System.err) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
    }
}
