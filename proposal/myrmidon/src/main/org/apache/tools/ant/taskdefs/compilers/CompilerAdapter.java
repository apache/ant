/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.compilers;

import org.apache.myrmidon.api.TaskException;

/**
 * The interface that all compiler adapters must adher to. <p>
 *
 * A compiler adapter is an adapter that interprets the javac's parameters in
 * preperation to be passed off to the compier this adapter represents. As all
 * the necessary values are stored in the Javac task itself, the only thing all
 * adapters need is the javac task, the execute command and a parameterless
 * constructor (for reflection).</p>
 *
 * @author Jay Dickon Glanville <a href="mailto:jayglanville@home.com">
 *      jayglanville@home.com</a>
 */

public interface CompilerAdapter
{

    /**
     * Sets the compiler attributes, which are stored in the Javac task.
     *
     * @param attributes The new Javac value
     */
    void setJavac( Javac attributes );

    /**
     * Executes the task.
     *
     * @return has the compilation been successful
     * @exception TaskException Description of Exception
     */
    boolean execute()
        throws TaskException;
}
