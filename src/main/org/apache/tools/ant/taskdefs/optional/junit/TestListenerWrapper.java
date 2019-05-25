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

package org.apache.tools.ant.taskdefs.optional.junit;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;


public class TestListenerWrapper implements TestListener, IgnoredTestListener {

    private TestListener wrapped;

    public TestListenerWrapper(TestListener listener) {
        super();
        wrapped = listener;
    }

    @Override
    public void addError(Test test, Throwable throwable) {
        wrapped.addError(test, throwable);
    }

    @Override
    public void addFailure(Test test, AssertionFailedError assertionFailedError) {
        wrapped.addFailure(test, assertionFailedError);
    }

    @Override
    public void endTest(Test test) {
        wrapped.endTest(test);
    }

    @Override
    public void startTest(Test test) {
        wrapped.startTest(test);
    }

    @Override
    public void testIgnored(Test test) {
        if (wrapped instanceof IgnoredTestListener) {
            ((IgnoredTestListener) wrapped).testIgnored(test);
        }
    }

    @Override
    public void testAssumptionFailure(Test test, Throwable throwable) {
        if (wrapped instanceof IgnoredTestListener) {
            ((IgnoredTestListener) wrapped).testAssumptionFailure(test,
                throwable);
        }
    }

}
