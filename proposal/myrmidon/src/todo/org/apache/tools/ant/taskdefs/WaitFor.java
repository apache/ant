/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import java.util.Hashtable;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.taskdefs.condition.ConditionBase;
import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * Wait for an external event to occur. Wait for an external process to start or
 * to complete some task. This is useful with the <code>parallel</code> task to
 * syncronize the execution of tests with server startup. The following
 * attributes can be specified on a waitfor task:
 * <ul>
 *   <li> maxwait - maximum length of time to wait before giving up</li>
 *   <li> maxwaitunit - The unit to be used to interpret maxwait attribute</li>
 *
 *   <li> checkevery - amount of time to sleep between each check</li>
 *   <li> checkeveryunit - The unit to be used to interpret checkevery attribute
 *   </li>
 *   <li> timeoutproperty - name of a property to set if maxwait has been
 *   exceeded.</li>
 * </ul>
 * The maxwaitunit and checkeveryunit are allowed to have the following values:
 * millesond, second, minute, hour, day and week. The default is millisecond.
 *
 * @author <a href="mailto:denis@network365.com">Denis Hennessy</a>
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 */

public class WaitFor extends ConditionBase
{
    private long maxWaitMillis = 1000l * 60l * 3l;// default max wait time
    private long maxWaitMultiplier = 1l;
    private long checkEveryMillis = 500l;
    private long checkEveryMultiplier = 1l;
    private String timeoutProperty;

    /**
     * Set the time between each check
     *
     * @param time The new CheckEvery value
     */
    public void setCheckEvery( long time )
    {
        checkEveryMillis = time;
    }

    /**
     * Set the check every time unit
     *
     * @param unit The new CheckEveryUnit value
     */
    public void setCheckEveryUnit( Unit unit )
    {
        checkEveryMultiplier = unit.getMultiplier();
    }

    /**
     * Set the maximum length of time to wait
     *
     * @param time The new MaxWait value
     */
    public void setMaxWait( long time )
    {
        maxWaitMillis = time;
    }

    /**
     * Set the max wait time unit
     *
     * @param unit The new MaxWaitUnit value
     */
    public void setMaxWaitUnit( Unit unit )
    {
        maxWaitMultiplier = unit.getMultiplier();
    }

    /**
     * Set the timeout property.
     *
     * @param p The new TimeoutProperty value
     */
    public void setTimeoutProperty( String p )
    {
        timeoutProperty = p;
    }

    /**
     * Check repeatedly for the specified conditions until they become true or
     * the timeout expires.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        if( countConditions() > 1 )
        {
            throw new TaskException( "You must not nest more than one condition into <waitfor>" );
        }
        if( countConditions() < 1 )
        {
            throw new TaskException( "You must nest a condition into <waitfor>" );
        }
        Condition c = (Condition)getConditions().nextElement();

        maxWaitMillis *= maxWaitMultiplier;
        checkEveryMillis *= checkEveryMultiplier;
        long start = System.currentTimeMillis();
        long end = start + maxWaitMillis;

        while( System.currentTimeMillis() < end )
        {
            if( c.eval() )
            {
                return;
            }
            try
            {
                Thread.sleep( checkEveryMillis );
            }
            catch( InterruptedException e )
            {
            }
        }

        if( timeoutProperty != null )
        {
            final String name = timeoutProperty;
            getContext().setProperty( name, "true" );
        }
    }

    public static class Unit extends EnumeratedAttribute
    {

        private final static String MILLISECOND = "millisecond";
        private final static String SECOND = "second";
        private final static String MINUTE = "minute";
        private final static String HOUR = "hour";
        private final static String DAY = "day";
        private final static String WEEK = "week";

        private final static String[] units = {
            MILLISECOND, SECOND, MINUTE, HOUR, DAY, WEEK
        };

        private Hashtable timeTable = new Hashtable();

        public Unit()
        {
            timeTable.put( MILLISECOND, new Long( 1l ) );
            timeTable.put( SECOND, new Long( 1000l ) );
            timeTable.put( MINUTE, new Long( 1000l * 60l ) );
            timeTable.put( HOUR, new Long( 1000l * 60l * 60l ) );
            timeTable.put( DAY, new Long( 1000l * 60l * 60l * 24l ) );
            timeTable.put( WEEK, new Long( 1000l * 60l * 60l * 24l * 7l ) );
        }

        public long getMultiplier()
        {
            String key = getValue().toLowerCase();
            Long l = (Long)timeTable.get( key );
            return l.longValue();
        }

        public String[] getValues()
        {
            return units;
        }
    }
}
