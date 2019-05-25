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

package org.apache.tools.ant;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class UnknownElementTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/core/unknownelement.xml");
    }

    @Test
    public void testMaybeConfigure() {
        // make sure we do not get a NPE
        buildRule.executeTarget("testMaybeConfigure");
    }

    /**
     * Not really a UnknownElement test but rather one of "what
     * information is available in taskFinished".
     * @see <a href="https://issues.apache.org/bugzilla/show_bug.cgi?id=26197">bug 26197</a>
     */
    @Test
    @Ignore("Previously disabled through naming convention")
    public void testTaskFinishedEvent() {
        buildRule.getProject().addBuildListener(new BuildListener() {
            public void buildStarted(BuildEvent event) {
            }

            public void buildFinished(BuildEvent event) {
            }

            public void targetStarted(BuildEvent event) {
            }

            public void targetFinished(BuildEvent event) {
            }

            public void taskStarted(BuildEvent event) {
                assertTaskProperties(event.getTask());
            }

            public void taskFinished(BuildEvent event) {
                assertTaskProperties(event.getTask());
            }

            public void messageLogged(BuildEvent event) {
            }

            private void assertTaskProperties(Task ue) {
                assertNotNull(ue);
                assertThat(ue, instanceOf(UnknownElement.class));
                Task t = ((UnknownElement) ue).getTask();
                assertNotNull(t);
                assertEquals("org.apache.tools.ant.taskdefs.Echo",
                        t.getClass().getName());
            }
        });
        buildRule.executeTarget("echo");
    }

    public static class Child extends Task {
        Parent parent;

        public void injectParent(Parent parent) {
            this.parent = parent;
        }

        public void execute() {
            parent.fromChild();
        }
    }

    public static class Parent extends Task implements TaskContainer {
        List<Task> children = new ArrayList<>();

        public void addTask(Task t) {
            children.add(t);
        }

        public void fromChild() {
            log("fromchild");
        }

        public void execute() {
            for (Task task : children) {
                UnknownElement el = (UnknownElement) task;
                el.maybeConfigure();
                Child child = (Child) el.getRealThing();
                child.injectParent(this);
                child.perform();
            }
        }
    }
}
