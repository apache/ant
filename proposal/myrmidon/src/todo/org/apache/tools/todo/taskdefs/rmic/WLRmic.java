/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.rmic;

import java.lang.reflect.Method;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.framework.java.ExecuteJava;
import org.apache.myrmidon.framework.nativelib.Commandline;
import org.apache.myrmidon.framework.nativelib.ArgumentList;
import org.apache.tools.todo.taskdefs.rmic.DefaultRmicAdapter;

/**
 * The implementation of the rmic for WebLogic
 *
 * @author <a href="mailto:tokamoto@rd.nttdata.co.jp">Takashi Okamoto</a>
 */
public class WLRmic extends DefaultRmicAdapter
{

    /**
     * Get the suffix for the rmic skeleton classes
     *
     * @return The SkelClassSuffix value
     */
    public String getSkelClassSuffix()
    {
        return "_WLSkel";
    }

    /**
     * Get the suffix for the rmic stub classes
     *
     * @return The StubClassSuffix value
     */
    public String getStubClassSuffix()
    {
        return "_WLStub";
    }

    public boolean execute()
        throws TaskException
    {
        getTaskContext().debug( "Using WebLogic rmic" );

        final ExecuteJava exe = new ExecuteJava();
        exe.setClassName( "weblogic.rmic" );
        final ArgumentList cmd = setupRmicCommand( new String[]{"-noexit"} );
        exe.getArguments().addArguments( cmd );

        exe.execute( getTaskContext() );
        return true;
    }
}
