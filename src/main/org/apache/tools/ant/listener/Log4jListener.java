/*
 * Copyright  2001-2002,2004 The Apache Software Foundation
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

package org.apache.tools.ant.listener;

import org.apache.log4j.Logger;
import org.apache.log4j.helpers.NullEnumeration;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;


/**
 *  Listener which sends events to Log4j logging system
 *
 */
public class Log4jListener implements BuildListener {

    /** Indicates if the listener was initialized. */
    private boolean initialized = false;

    /**
     * Construct the listener and make sure there is a valid appender.
     */
    public Log4jListener() {
        initialized = false;
        Logger log = Logger.getLogger("org.apache.tools.ant");
        Logger rootLog = Logger.getRootLogger();
        if (!(rootLog.getAllAppenders() instanceof NullEnumeration)) {
            initialized = true;
        } else {
            log.error("No log4j.properties in build area");
        }
    }

    /**
     * @see BuildListener#buildStarted
     */
    public void buildStarted(BuildEvent event) {
        if (initialized) {
            Logger log = Logger.getLogger(Project.class.getName());
            log.info("Build started.");
        }
    }

    /**
     * @see BuildListener#buildFinished
     */
    public void buildFinished(BuildEvent event) {
        if (initialized) {
            Logger log = Logger.getLogger(Project.class.getName());
            if (event.getException() == null) {
                log.info("Build finished.");
            } else {
                log.error("Build finished with error.", event.getException());
            }
        }
    }

    /**
     * @see BuildListener#targetStarted
     */
    public void targetStarted(BuildEvent event) {
        if (initialized) {
            Logger log = Logger.getLogger(Target.class.getName());
            log.info("Target \"" + event.getTarget().getName() + "\" started.");
        }
    }

    /**
     * @see BuildListener#targetFinished
     */
    public void targetFinished(BuildEvent event) {
        if (initialized) {
            String targetName = event.getTarget().getName();
            Logger cat = Logger.getLogger(Target.class.getName());
            if (event.getException() == null) {
                cat.info("Target \"" + targetName + "\" finished.");
            } else {
                cat.error("Target \"" + targetName
                    + "\" finished with error.", event.getException());
            }
        }
    }

    /**
     * @see BuildListener#taskStarted
     */
    public void taskStarted(BuildEvent event) {
        if (initialized) {
            Task task = event.getTask();
            Logger log = Logger.getLogger(task.getClass().getName());
            log.info("Task \"" + task.getTaskName() + "\" started.");
        }
    }

    /**
     * @see BuildListener#taskFinished
     */
    public void taskFinished(BuildEvent event) {
        if (initialized) {
            Task task = event.getTask();
            Logger log = Logger.getLogger(task.getClass().getName());
            if (event.getException() == null) {
                log.info("Task \"" + task.getTaskName() + "\" finished.");
            } else {
                log.error("Task \"" + task.getTaskName()
                    + "\" finished with error.", event.getException());
            }
        }
    }

    /**
     * @see BuildListener#messageLogged
     */
    public void messageLogged(BuildEvent event) {
        if (initialized) {
            Object categoryObject = event.getTask();
            if (categoryObject == null) {
                categoryObject = event.getTarget();
                if (categoryObject == null) {
                    categoryObject = event.getProject();
                }
            }

            Logger log
                = Logger.getLogger(categoryObject.getClass().getName());
            switch (event.getPriority()) {
                case Project.MSG_ERR:
                    log.error(event.getMessage());
                    break;
                case Project.MSG_WARN:
                    log.warn(event.getMessage());
                    break;
                case Project.MSG_INFO:
                    log.info(event.getMessage());
                    break;
                case Project.MSG_VERBOSE:
                    log.debug(event.getMessage());
                    break;
                case Project.MSG_DEBUG:
                    log.debug(event.getMessage());
                    break;
                default:
                    log.error(event.getMessage());
                    break;
            }
        }
    }
}
