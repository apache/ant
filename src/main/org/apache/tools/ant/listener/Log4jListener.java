/*
 * Copyright  2001-2002,2004 Apache Software Foundation
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

import org.apache.log4j.Category;
import org.apache.log4j.helpers.NullEnumeration;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;


/**
 *  Listener which sends events to Log4j logging system
 *
 * @author Conor MacNeill
 */
public class Log4jListener implements BuildListener {

    /** Indicates if the listener was initialized. */
    private boolean initialized = false;

    /**
     * Construct the listener and make sure there is a valid appender.
     */
    public Log4jListener() {
        initialized = false;
        Category cat = Category.getInstance("org.apache.tools.ant");
        Category rootCat = Category.getRoot();
        if (!(rootCat.getAllAppenders() instanceof NullEnumeration)) {
            initialized = true;
        } else {
            cat.error("No log4j.properties in build area");
        }
    }

    /**
     * @see BuildListener#buildStarted
     */
    public void buildStarted(BuildEvent event) {
        if (initialized) {
            Category cat = Category.getInstance(Project.class.getName());
            cat.info("Build started.");
        }
    }

    /**
     * @see BuildListener#buildFinished
     */
    public void buildFinished(BuildEvent event) {
        if (initialized) {
            Category cat = Category.getInstance(Project.class.getName());
            if (event.getException() == null) {
                cat.info("Build finished.");
            } else {
                cat.error("Build finished with error.", event.getException());
            }
        }
    }

    /**
     * @see BuildListener#targetStarted
     */
    public void targetStarted(BuildEvent event) {
        if (initialized) {
            Category cat = Category.getInstance(Target.class.getName());
            cat.info("Target \"" + event.getTarget().getName() + "\" started.");
        }
    }

    /**
     * @see BuildListener#targetFinished
     */
    public void targetFinished(BuildEvent event) {
        if (initialized) {
            String targetName = event.getTarget().getName();
            Category cat = Category.getInstance(Target.class.getName());
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
            Category cat = Category.getInstance(task.getClass().getName());
            cat.info("Task \"" + task.getTaskName() + "\" started.");
        }
    }

    /**
     * @see BuildListener#taskFinished
     */
    public void taskFinished(BuildEvent event) {
        if (initialized) {
            Task task = event.getTask();
            Category cat = Category.getInstance(task.getClass().getName());
            if (event.getException() == null) {
                cat.info("Task \"" + task.getTaskName() + "\" finished.");
            } else {
                cat.error("Task \"" + task.getTaskName()
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

            Category cat
                = Category.getInstance(categoryObject.getClass().getName());
            switch (event.getPriority()) {
                case Project.MSG_ERR:
                    cat.error(event.getMessage());
                    break;
                case Project.MSG_WARN:
                    cat.warn(event.getMessage());
                    break;
                case Project.MSG_INFO:
                    cat.info(event.getMessage());
                    break;
                case Project.MSG_VERBOSE:
                    cat.debug(event.getMessage());
                    break;
                case Project.MSG_DEBUG:
                    cat.debug(event.getMessage());
                    break;
                default:
                    cat.error(event.getMessage());
                    break;
            }
        }
    }
}
