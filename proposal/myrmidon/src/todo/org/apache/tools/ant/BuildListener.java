/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant;

import java.util.EventListener;

/**
 * Classes that implement this interface will be notified when things happend
 * during a build.
 *
 * @author RT
 * @see BuildEvent
 * @see Project#addBuildListener(BuildListener)
 */
public interface BuildListener extends EventListener
{

    /**
     * Fired before any targets are started.
     *
     * @param event Description of Parameter
     */
    void buildStarted( BuildEvent event );

    /**
     * Fired after the last target has finished. This event will still be thrown
     * if an error occured during the build.
     *
     * @param event Description of Parameter
     * @see BuildEvent#getException()
     */
    void buildFinished( BuildEvent event );

    /**
     * Fired when a target is started.
     *
     * @param event Description of Parameter
     * @see BuildEvent#getTarget()
     */
    void targetStarted( BuildEvent event );

    /**
     * Fired when a target has finished. This event will still be thrown if an
     * error occured during the build.
     *
     * @param event Description of Parameter
     * @see BuildEvent#getException()
     */
    void targetFinished( BuildEvent event );

    /**
     * Fired when a task is started.
     *
     * @param event Description of Parameter
     * @see BuildEvent#getTask()
     */
    void taskStarted( BuildEvent event );

    /**
     * Fired when a task has finished. This event will still be throw if an
     * error occured during the build.
     *
     * @param event Description of Parameter
     * @see BuildEvent#getException()
     */
    void taskFinished( BuildEvent event );

    /**
     * Fired whenever a message is logged.
     *
     * @param event Description of Parameter
     * @see BuildEvent#getMessage()
     * @see BuildEvent#getPriority()
     */
    void messageLogged( BuildEvent event );
}
