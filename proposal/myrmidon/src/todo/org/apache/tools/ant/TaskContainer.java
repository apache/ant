/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant;

/**
 * Interface for objects which can contain tasks <p>
 *
 * It is recommended that implementations call {@link Task#perform perform}
 * instead of {@link Task#execute execute} for the tasks they contain, as this
 * method ensures that {@link BuildEvent BuildEvents} will be generated.</p>
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 */
public interface TaskContainer
{
    /**
     * Add a task to this task container
     *
     * @param task the task to be added to this container
     */
    void addTask( Task task );
}

