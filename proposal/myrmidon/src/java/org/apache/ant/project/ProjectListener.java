/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.project;

/**
 * The interface to implement if you want to receive 
 * notification of project status.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface ProjectListener
{
    /**
     * Notify listener of projectStarted event.
     *
     * @param projectName the projectName
     */
    void projectStarted( String projectName );

    /**
     * Notify listener of projectFinished event.
     */
    void projectFinished();

    /**
     * Notify listener of targetStarted event.
     *
     * @param targetName the name of target
     */
    void targetStarted( String targetName );

    /**
     * Notify listener of targetFinished event.
     */
    void targetFinished();

    /**
     * Notify listener of taskletStarted event.
     *
     * @param taskletName the name of tasklet
     */
    void taskletStarted( String taskletName );

    /**
     * Notify listener of taskletFinished event.
     */
    void taskletFinished();

    /**
     * Notify listener of log message event.
     *
     * @param message the message
     */
    void log( String message );

    /**
     * Notify listener of log message event.
     *
     * @param message the message
     * @param throwable the throwable
     */
    void log( String message, Throwable throwable );
}
