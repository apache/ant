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

    /**
     * log category we log into
     */
    public static final String LOG_ANT = "org.apache.tools.ant";

    /** Indicates if the listener was initialized. */
    private final boolean initialized;

    /**
     * Construct the listener and make sure there is a valid appender.
     */
    public Log4jListener() {
        final Logger log = Logger.getLogger(LOG_ANT);
        final Logger rootLog = Logger.getRootLogger();
        initialized = !(rootLog.getAllAppenders() instanceof NullEnumeration);
        if (!initialized) {
            log.error("No log4j.properties in build area");
        }
    }

    /**
     * @see BuildListener#buildStarted
     */
    /** {@inheritDoc}. */
    @Override
    public void buildStarted(final BuildEvent event) {
        if (initialized) {
            final Logger log = Logger.getLogger(Project.class.getName());
            log.info("Build started.");
        }
    }

    /**
     * @see BuildListener#buildFinished
     */
    /** {@inheritDoc}. */
    @Override
    public void buildFinished(final BuildEvent event) {
        if (initialized) {
            final Logger log = Logger.getLogger(Project.class.getName());
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
    /** {@inheritDoc}. */
    @Override
    public void targetStarted(final BuildEvent event) {
        if (initialized) {
            final Logger log = Logger.getLogger(Target.class.getName());
            log.info("Target \"" + event.getTarget().getName() + "\" started.");
        }
    }

    /**
     * @see BuildListener#targetFinished
     */
    /** {@inheritDoc}. */
    @Override
    public void targetFinished(final BuildEvent event) {
        if (initialized) {
            final String targetName = event.getTarget().getName();
            final Logger cat = Logger.getLogger(Target.class.getName());
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
    /** {@inheritDoc}. */
    @Override
    public void taskStarted(final BuildEvent event) {
        if (initialized) {
            final Task task = event.getTask();
            final Logger log = Logger.getLogger(task.getClass().getName());
            log.info("Task \"" + task.getTaskName() + "\" started.");
        }
    }

    /**
     * @see BuildListener#taskFinished
     */
    /** {@inheritDoc}. */
    @Override
    public void taskFinished(final BuildEvent event) {
        if (initialized) {
            final Task task = event.getTask();
            final Logger log = Logger.getLogger(task.getClass().getName());
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
    /** {@inheritDoc}. */
    @Override
    public void messageLogged(final BuildEvent event) {
        if (initialized) {
            Object categoryObject = event.getTask();
            if (categoryObject == null) {
                categoryObject = event.getTarget();
                if (categoryObject == null) {
                    categoryObject = event.getProject();
                }
            }

            final Logger log
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
