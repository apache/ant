/*
 * Copyright  2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

/**
 * writes a single 0 byte to the given output stream in endTestSuite.
 */
public class ForkedVMWatcher implements JUnitResultFormatter {

    /**
     * OutputStream to write to.
     */
    private OutputStream out;

    /**
     * Empty
     */
    public ForkedVMWatcher() {
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
     * Interface TestListener for JUnit &gt; 3.4.
     *
     * <p>A Test failed.
     */
    public void addFailure(Test test, AssertionFailedError t) {
        addFailure(test, (Throwable) t);
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

    /**
     * The whole testsuite ended.
     */
    public void endTestSuite(JUnitTest suite) throws BuildException {
        try {
            out.write(0);
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
