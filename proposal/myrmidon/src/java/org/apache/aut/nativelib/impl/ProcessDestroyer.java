/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.nativelib.impl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Destroys all registered <code>Process</code>es when
 * the VM exits (if in JDK1.3) or when requested.
 *
 * @author <a href="mailto:mnewcomb@tacintel.com">Michael Newcomb</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
class ProcessDestroyer
    extends Thread
{
    private ArrayList m_processes = new ArrayList();

    /**
     * Constructs a <code>ProcessDestroyer</code> and registers it as a shutdown
     * hook.
     */
    public ProcessDestroyer()
    {
        try
        {
            // check to see if the method exists (support pre-JDK 1.3 VMs)
            //
            final Class[] paramTypes = {Thread.class};
            final Method addShutdownHook =
                Runtime.class.getMethod( "addShutdownHook", paramTypes );

            // add the hook
            Object[] args = {this};
            addShutdownHook.invoke( Runtime.getRuntime(), args );
        }
        catch( final Exception e )
        {
            // it just won't be added as a shutdown hook... :(
        }
    }

    /**
     * Add process to list of processes to be shutdown.
     *
     * @param process the process to add
     */
    public synchronized void add( final Process process )
    {
        if( !m_processes.contains( process ) )
        {
            m_processes.add( process );
        }
    }

    /**
     * Remove process from list of processes to be shutdown.
     *
     * @param process the process to remove
     */
    public synchronized void remove( final Process process )
    {
        m_processes.remove( process );
    }

    /**
     * Invoked by the VM when it is exiting.
     */
    public void run()
    {
        destroyProcesses();
    }

    protected synchronized void destroyProcesses()
    {
        final Iterator processes = m_processes.iterator();
        while( processes.hasNext() )
        {
            ( (Process)processes.next() ).destroy();
            processes.remove();
        }
    }
}
