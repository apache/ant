/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.java;

import org.apache.myrmidon.framework.AbstractFacadeTask;
import org.apache.myrmidon.api.TaskException;

/**
 * A task that compiles Java source files.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Robin Green <a href="mailto:greenrd@hotmail.com">greenrd@hotmail.com</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant.task name="javac"
 */
public class JavacTask
    extends AbstractFacadeTask
{
    public JavacTask()
    {
        super( "compiler", JavaCompilerAdaptor.class, "javac" );
    }

    /**
     * Execute this task.
     */
    public void execute()
        throws TaskException
    {
        getContext().verbose( "Using " + getImplementation() + " compiler." );
        final JavaCompilerAdaptor adaptor = (JavaCompilerAdaptor)prepareFacade();
        adaptor.execute();
    }

    /**
     * Create the instance of the facade.
     */
    protected Object createFacade()
        throws TaskException
    {
        JavaCompilerAdaptor adaptor = (JavaCompilerAdaptor)super.createFacade();
        adaptor.setContext( getContext() );
        return adaptor;
    }
}
