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
package org.apache.tools.ant.taskdefs;

/**
 * Helper class for ExecuteWatchdogTest and ExecuteJavaTest.
 *
 * <p>Used to be an inner class of ExecuteWatchdogTest.
 *
 */
public class TimeProcess {
    public static void main(String[] args) throws Exception {
        int time = Integer.parseInt(args[0]);
        if (time < 1) {
            throw new IllegalArgumentException("Invalid time: " + time);
        }
        Thread.sleep(time);
    }
}
