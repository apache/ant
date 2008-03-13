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

package org.apache.tools.ant.taskdefs.optional.splash;

import org.apache.tools.ant.Project;

/**
 * This is an "interactive" test, it passes if the splash screen
 * disappears after the "finished" but before the "exiting" message.
 *
 * This even isn't a JUnit test case.
 *
 * @since Ant 1.5.2
 */
public class SplashScreenTest {

    public static void main(String[] args) {
        Project p = new Project();
        SplashTask t = new SplashTask();
        t.setProject(p);
        t.execute();

        // give it some time to display
        try {
            Thread.currentThread().sleep(2000);
        } catch (InterruptedException e) {
        } // end of try-catch

        p.fireBuildFinished(null);
        System.err.println("finished");

        try {
            Thread.currentThread().sleep(2000);
        } catch (InterruptedException e) {
        } // end of try-catch
        System.err.println("exiting");
        System.exit(0);
    }
}

