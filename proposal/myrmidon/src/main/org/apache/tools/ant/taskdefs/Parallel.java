/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;

import java.util.Enumeration;
import java.util.Vector;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;

/**
 * Implements a multi threaded task execution. <p>
 *
 *
 *
 * @author Thomas Christen <a href="mailto:chr@active.ch">chr@active.ch</a>
 * @author <a href="mailto:conor@apache.org">Conor MacNeill </a>
 */
public class Parallel extends Task
    implements TaskContainer
{

    /**
     * Collection holding the nested tasks
     */
    private Vector nestedTasks = new Vector();

    /**
     * Add a nested task to execute parallel (asynchron). <p>
     *
     *
     *
     * @param nestedTask Nested task to be executed in parallel
     * @exception TaskException Description of Exception
     */
    public void addTask( Task nestedTask )
        throws TaskException
    {
        nestedTasks.addElement( nestedTask );
    }

    /**
     * Block execution until the specified time or for a specified amount of
     * milliseconds and if defined, execute the wait status.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        TaskThread[] threads = new TaskThread[ nestedTasks.size() ];
        int threadNumber = 0;
        for( Enumeration e = nestedTasks.elements(); e.hasMoreElements(); threadNumber++ )
        {
            Task nestedTask = (Task)e.nextElement();
            threads[ threadNumber ] = new TaskThread( threadNumber, nestedTask );
        }

        // now start all threads
        for( int i = 0; i < threads.length; ++i )
        {
            threads[ i ].start();
        }

        // now join to all the threads
        for( int i = 0; i < threads.length; ++i )
        {
            try
            {
                threads[ i ].join();
            }
            catch( InterruptedException ie )
            {
                // who would interrupt me at a time like this?
            }
        }

        // now did any of the threads throw an exception
        StringBuffer exceptionMessage = new StringBuffer();
        String lSep = System.getProperty( "line.separator" );
        int numExceptions = 0;
        Throwable firstException = null;
        Location firstLocation = Location.UNKNOWN_LOCATION;
        ;
        for( int i = 0; i < threads.length; ++i )
        {
            Throwable t = threads[ i ].getException();
            if( t != null )
            {
                numExceptions++;
                if( firstException == null )
                {
                    firstException = t;
                }
                /*
                if( t instanceof TaskException &&
                    firstLocation == Location.UNKNOWN_LOCATION )
                {
                    firstLocation = ( (TaskException)t ).getLocation();
                }
                */
                exceptionMessage.append( lSep );
                exceptionMessage.append( t.getMessage() );
            }
        }

        if( numExceptions == 1 )
        {
            if( firstException instanceof TaskException )
            {
                throw (TaskException)firstException;
            }
            else
            {
                throw new TaskException( "Error", firstException );
            }
        }
        else if( numExceptions > 1 )
        {
            throw new TaskException( exceptionMessage.toString() );
        }
    }

    class TaskThread extends Thread
    {
        private Throwable exception;
        private Task task;
        private int taskNumber;

        /**
         * Construct a new TaskThread<p>
         *
         *
         *
         * @param task the Task to be executed in a seperate thread
         * @param taskNumber Description of Parameter
         */
        TaskThread( int taskNumber, Task task )
        {
            this.task = task;
            this.taskNumber = taskNumber;
        }

        public Throwable getException()
        {
            return exception;
        }

        /**
         * Executes the task within a thread and takes care about Exceptions
         * raised within the task.
         */
        public void run()
        {
            try
            {
                task.perform();
            }
            catch( Throwable t )
            {
                exception = t;
            }
        }
    }
}
