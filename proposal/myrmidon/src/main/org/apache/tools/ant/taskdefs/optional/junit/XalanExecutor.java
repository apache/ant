/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import org.apache.myrmidon.api.TaskException;

/**
 * Command class that encapsulate specific behavior for each Xalan version. The
 * right executor will be instantiated at runtime via class lookup. For
 * instance, it will check first for Xalan2, then for Xalan1.
 *
 * @author RT
 */
abstract class XalanExecutor
{
    /**
     * the transformer caller
     */
    protected AggregateTransformer caller;

    /**
     * Create a valid Xalan executor. It checks first if Xalan2 is present, if
     * not it checks for xalan1. If none is available, it fails.
     *
     * @param caller object containing the transformation information.
     * @return Description of the Returned Value
     * @throws TaskException thrown if it could not find a valid xalan
     *      executor.
     */
    static XalanExecutor newInstance( AggregateTransformer caller )
        throws TaskException
    {
        Class procVersion = null;
        XalanExecutor executor = null;
        try
        {
            procVersion = Class.forName( "org.apache.xalan.processor.XSLProcessorVersion" );
            executor = new Xalan2Executor();
        }
        catch( Exception xalan2missing )
        {
            try
            {
                procVersion = Class.forName( "org.apache.xalan.xslt.XSLProcessorVersion" );
                executor = (XalanExecutor)Class.forName(
                    "org.apache.tools.ant.taskdefs.optional.junit.Xalan1Executor" ).newInstance();
            }
            catch( Exception xalan1missing )
            {
                throw new TaskException( "Could not find xalan2 nor xalan1 in the classpath. Check http://xml.apache.org/xalan-j" );
            }
        }
        String version = getXalanVersion( procVersion );
        //caller.task.getLogger().info( "Using Xalan version: " + version );
        executor.setCaller( caller );
        return executor;
    }

    /**
     * pretty useful data (Xalan version information) to display.
     *
     * @param procVersion Description of Parameter
     * @return The XalanVersion value
     */
    private static String getXalanVersion( Class procVersion )
    {
        try
        {
            Field f = procVersion.getField( "S_VERSION" );
            return f.get( null ).toString();
        }
        catch( Exception e )
        {
            return "?";
        }
    }

    /**
     * get the appropriate stream based on the format (frames/noframes)
     *
     * @return The OutputStream value
     * @exception IOException Description of Exception
     */
    protected OutputStream getOutputStream()
        throws IOException
    {
        if( caller.FRAMES.equals( caller.format ) )
        {
            // dummy output for the framed report
            // it's all done by extension...
            return new ByteArrayOutputStream();
        }
        else
        {
            return new FileOutputStream( new File( caller.toDir, "junit-noframes.html" ) );
        }
    }

    /**
     * override to perform transformation
     *
     * @exception Exception Description of Exception
     */
    abstract void execute()
        throws Exception;

    /**
     * set the caller for this object.
     *
     * @param caller The new Caller value
     */
    private final void setCaller( AggregateTransformer caller )
    {
        this.caller = caller;
    }
}
