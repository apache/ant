/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.java;

import java.io.File;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.java.ExecuteJava;
import org.apache.tools.todo.types.Argument;
import org.apache.tools.todo.types.EnvironmentVariable;
import org.apache.myrmidon.framework.file.Path;

/**
 * This task acts as a loader for java applications but allows to use the same
 * JVM for the called application thus resulting in much faster operation.
 *
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">
 *      stefano@apache.org</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 *
 * @ant.task name="java"
 */
public class JavaTask
    extends AbstractTask
{
    private final ExecuteJava m_exec = new ExecuteJava();

    /**
     * Set the class name.
     */
    public void setClassname( final String className )
    {
        m_exec.setClassName( className );
    }

    /**
     * Add a classpath element.
     */
    public void addClasspath( final Path classpath )
        throws TaskException
    {
        m_exec.getClassPath().add( classpath );
    }

    /**
     * The working directory of the process
     *
     * @param dir The new Dir value
     */
    public void setDir( final File dir )
    {
        m_exec.setWorkingDirectory( dir );
    }

    /**
     * Set the forking flag.
     */
    public void setFork( final boolean fork )
    {
        m_exec.setFork( fork );
    }

    /**
     * Set the jar name.
     */
    public void setJar( final File jar )
    {
        m_exec.setJar( jar );
    }

    /**
     * Set the command used to start the VM (only if fork==true).
     */
    public void setJvm( final String jvm )
    {
        m_exec.setJvm( jvm );
    }

    /**
     * -mx or -Xmx depending on VM version
     */
    public void setMaxmemory( final String max )
    {
        m_exec.setMaxMemory( max );
    }

    /**
     * Add a nested sysproperty element.
     */
    public void addSysproperty( final EnvironmentVariable sysp )
    {
        m_exec.getSysProperties().addVariable( sysp );
    }

    /**
     * Creates a nested arg element.
     */
    public void addArg( final Argument argument )
    {
        m_exec.getArguments().addArgument( argument );
    }

    /**
     * Creates a nested jvmarg element.
     */
    public void addJvmarg( final Argument argument )
    {
        m_exec.getVmArguments().addArgument( argument );
    }

    public void execute()
        throws TaskException
    {
        m_exec.execute( getContext() );
    }
}
