/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.rmic;

import java.lang.reflect.Method;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.TaskContext;
import org.apache.tools.ant.types.Commandline;

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
        Commandline cmd = setupRmicCommand( new String[]{"-noexit"} );

        try
        {
            // Create an instance of the rmic
            Class c = Class.forName( "weblogic.rmic" );
            Method doRmic = c.getMethod( "main",
                                         new Class[]{String[].class} );
            doRmic.invoke( null, new Object[]{cmd.getArguments()} );
            return true;
        }
        catch( ClassNotFoundException ex )
        {
            throw new TaskException( "Cannot use WebLogic rmic, as it is not available" +
                                     " A common solution is to set the environment variable" +
                                     " CLASSPATH." );
        }
        catch( Exception ex )
        {
            if( ex instanceof TaskException )
            {
                throw (TaskException)ex;
            }
            else
            {
                throw new TaskException( "Error starting WebLogic rmic: ", ex );
            }
        }
    }
}
