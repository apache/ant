/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.jsp.compilers;

import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.TaskContext;
import org.apache.tools.todo.taskdefs.jsp.JspC;

/**
 * The interface that all jsp compiler adapters must adher to. <p>
 *
 * A compiler adapter is an adapter that interprets the jspc's parameters in
 * preperation to be passed off to the compier this adapter represents. As all
 * the necessary values are stored in the Jspc task itself, the only thing all
 * adapters need is the jsp task, the execute command and a parameterless
 * constructor (for reflection).</p>
 *
 * @author Jay Dickon Glanville <a href="mailto:jayglanville@home.com">
 *      jayglanville@home.com</a>
 * @author Matthew Watson <a href="mailto:mattw@i3sp.com">mattw@i3sp.com</a>
 */
public interface CompilerAdapter
{
    void setTaskContext( TaskContext context );

    /**
     * Sets the compiler attributes, which are stored in the Jspc task.
     *
     * @param attributes The new Jspc value
     */
    void setJspc( JspC attributes );

    /**
     * Executes the task.
     *
     * @return has the compilation been successful
     * @exception TaskException Description of Exception
     */
    boolean execute()
        throws TaskException;
}
