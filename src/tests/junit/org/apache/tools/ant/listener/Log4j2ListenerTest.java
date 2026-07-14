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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Proxy;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskAdapter;
import org.junit.Test;

public class Log4j2ListenerTest {

    private static LoggerContext createMockContext() {
        final ClassLoader cl = Log4j2Listener.class.getClassLoader();
        return (LoggerContext) Proxy.newProxyInstance(cl, new Class<?>[] { LoggerContext.class },
                (proxy, method, args) -> {
                    if ("getLogger".equals(method.getName())) {
                        return Proxy.newProxyInstance(cl, new Class<?>[] { ExtendedLogger.class }, (p, m, a) -> null);
                    }
                    return null;
                });
    }

    @Test
    public void testMessageOutputLevel() {
        final Log4j2Listener listener = new Log4j2Listener();
        // Sets level using Log4j2 Core
        listener.setMessageOutputLevel(Project.MSG_DEBUG);
        assertEquals(Level.DEBUG, listener.context.getLogger("").getLevel());
        // Does not fail if Log4j2 Core is not available
        final Log4j2Listener mockListener = new Log4j2Listener(createMockContext());
        mockListener.setMessageOutputLevel(Project.MSG_DEBUG);
    }

    @Test
    public void smokeTest() {
        final Project project = new Project();
        project.setName("My.Project");
        project.setUserProperty(MagicNames.ANT_FILE, "My.File");
        final Target target = new Target();
        target.setName("My.Target");
        target.setLocation(new Location("My.File", 3, 0));
        target.setProject(project);
        final Task task = new TaskAdapter();
        task.setTaskName("My.Task");
        task.setLocation(new Location("My.File", 7, 0));
        task.setProject(project);
        task.setOwningTarget(target);
        final Log4j2Listener listener = new Log4j2Listener();

        listener.setEmacsMode(false);
        final PrintStream stream = new PrintStream(new ByteArrayOutputStream());
        listener.setOutputPrintStream(stream);
        listener.setErrorPrintStream(stream);
        listener.setMessageOutputLevel(Project.MSG_VERBOSE);

        final BuildEvent projectEvent = new BuildEvent(project);
        listener.buildStarted(projectEvent);
        listener.buildFinished(projectEvent);
        listener.subBuildStarted(projectEvent);
        listener.subBuildFinished(projectEvent);

        final BuildEvent targetEvent = new BuildEvent(target);
        listener.targetStarted(targetEvent);
        listener.targetFinished(targetEvent);

        final BuildEvent taskEvent = new BuildEvent(task);
        listener.taskStarted(taskEvent);
        listener.taskFinished(taskEvent);

        final BuildEvent logEvent = new BuildEvent(task);
        logEvent.setException(new RuntimeException());
        logEvent.setMessage("Hello world!", Project.MSG_ERR);
        listener.messageLogged(logEvent);
    }
}
