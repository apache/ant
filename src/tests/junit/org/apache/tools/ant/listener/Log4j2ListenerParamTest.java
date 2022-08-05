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
package org.apache.tools.ant.listener;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskAdapter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class Log4j2ListenerParamTest {
    
    private final static Marker PROJECT = MarkerManager.getMarker("PROJECT");
    private final static Marker TARGET = MarkerManager.getMarker("TARGET");
    private final static Marker TASK = MarkerManager.getMarker("TASK");

    @Parameters
    public static Collection<Object[]> data() {
        final Project namelessProject = new Project();
        final Project namedProject = new Project();
        namedProject.setName("My.Project");
        namedProject.setUserProperty(MagicNames.ANT_FILE, "My.File");
        final Target namelessTarget = new Target();
        namelessTarget.setLocation(new Location("My.File0", 1, 0));
        namelessTarget.setProject(namedProject);
        final Target namedTarget = new Target();
        namedTarget.setName("My.Target");
        namedTarget.setLocation(new Location("My.File1", 3, 0));
        namedTarget.setProject(namedProject);
        final Task globalTask = new TaskAdapter();
        globalTask.setTaskName("My.Task1");
        globalTask.setLocation(new Location("My.File2", 5, 0));
        globalTask.setProject(namedProject);
        final Task localTask = new TaskAdapter();
        localTask.setTaskName("My.Task2");
        localTask.setLocation(new Location("My.File3", 7, 0));
        localTask.setProject(namedProject);
        localTask.setOwningTarget(namedTarget);
        MarkerManager.getMarker("PROJECT");
        MarkerManager.getMarker("TARGET");
        MarkerManager.getMarker("TASK");
        return Arrays.asList(new Object[][] {
                { new BuildEvent(namelessProject), "", null, -1, PROJECT },
                { new BuildEvent(namedProject), "My_Project", "My.File", -1, PROJECT },
                { new BuildEvent(namelessTarget), "My_Project", "My.File0", 1, TARGET },
                { new BuildEvent(namedTarget), "My_Project.My_Target", "My.File1", 3, TARGET },
                { new BuildEvent(globalTask), "My_Project.My_Task1", "My.File2", 5, TASK },
                { new BuildEvent(localTask), "My_Project.My_Target.My_Task2", "My.File3", 7, TASK } });
    }

    private final BuildEvent event;
    private final String loggerName;
    private final String fileName;
    private final int lineNumber;
    private final Marker marker;

    public Log4j2ListenerParamTest(BuildEvent event, String loggerName, String fileName, int lineNumber, Marker marker) {
        this.event = event;
        this.loggerName = loggerName;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.marker = marker;
    }

    @Test
    public void testNames() {
        assertEquals(loggerName, Log4j2Listener.getLoggerName(event));
        final StackTraceElement location = Log4j2Listener.extractLocation(event);
        assertEquals(fileName, location.getFileName());
        assertEquals(lineNumber, location.getLineNumber());
        assertEquals(marker, Log4j2Listener.getMarker(event));
    }
}
