/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.condition;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import org.apache.myrmidon.framework.Os;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.taskdefs.Available;
import org.apache.tools.ant.taskdefs.Checksum;
import org.apache.tools.ant.taskdefs.UpToDate;

/**
 * Baseclass for the &lt;condition&gt; task as well as several conditions -
 * ensures that the types of conditions inside the task and the "container"
 * conditions are in sync.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version $Revision$
 */
public abstract class ConditionBase extends ProjectComponent
{
    private ArrayList conditions = new ArrayList();

    /**
     * Add an &lt;and&gt; condition "container".
     *
     * @param a The feature to be added to the And attribute
     * @since 1.1
     */
    public void addAnd( And a )
    {
        conditions.add( a );
    }

    /**
     * Add an &lt;available&gt; condition.
     *
     * @param a The feature to be added to the Available attribute
     * @since 1.1
     */
    public void addAvailable( Available a )
    {
        conditions.add( a );
    }

    /**
     * Add an &lt;checksum&gt; condition.
     *
     * @param c The feature to be added to the Checksum attribute
     * @since 1.4
     */
    public void addChecksum( Checksum c )
    {
        conditions.add( c );
    }

    /**
     * Add an &lt;equals&gt; condition.
     *
     * @param e The feature to be added to the Equals attribute
     * @since 1.1
     */
    public void addEquals( Equals e )
    {
        conditions.add( e );
    }

    /**
     * Add an &lt;http&gt; condition.
     *
     * @param h The feature to be added to the Http attribute
     * @since 1.7
     */
    public void addHttp( Http h )
    {
        conditions.add( h );
    }

    /**
     * Add an &lt;isset&gt; condition.
     *
     * @param i The feature to be added to the IsSet attribute
     * @since 1.1
     */
    public void addIsSet( IsSet i )
    {
        conditions.add( i );
    }

    /**
     * Add an &lt;not&gt; condition "container".
     *
     * @param n The feature to be added to the Not attribute
     * @since 1.1
     */
    public void addNot( Not n )
    {
        conditions.add( n );
    }

    /**
     * Add an &lt;or&gt; condition "container".
     *
     * @param o The feature to be added to the Or attribute
     * @since 1.1
     */
    public void addOr( Or o )
    {
        conditions.add( o );
    }

    /**
     * Add an &lt;os&gt; condition.
     *
     * @param o The feature to be added to the Os attribute
     * @since 1.1
     */
    public void addOs( Os o )
    {
        conditions.add( o );
    }

    /**
     * Add a &lt;socket&gt; condition.
     *
     * @param s The feature to be added to the Socket attribute
     * @since 1.7
     */
    public void addSocket( Socket s )
    {
        conditions.add( s );
    }

    /**
     * Add an &lt;uptodate&gt; condition.
     *
     * @param u The feature to be added to the Uptodate attribute
     * @since 1.1
     */
    public void addUptodate( UpToDate u )
    {
        conditions.add( u );
    }

    /**
     * Iterate through all conditions.
     *
     * @return The Conditions value
     * @since 1.1
     */
    protected final Enumeration getConditions()
    {
        return new ConditionEnumeration();
    }

    /**
     * Count the conditions.
     *
     * @return Description of the Returned Value
     * @since 1.1
     */
    protected int countConditions()
    {
        return conditions.size();
    }

    /**
     * Inner class that configures those conditions with a project instance that
     * need it.
     *
     * @author RT
     * @since 1.1
     */
    private class ConditionEnumeration implements Enumeration
    {
        private int currentElement = 0;

        public boolean hasMoreElements()
        {
            return countConditions() > currentElement;
        }

        public Object nextElement()
            throws NoSuchElementException
        {
            Object o = null;
            try
            {
                o = conditions.get( currentElement++ );
            }
            catch( ArrayIndexOutOfBoundsException e )
            {
                throw new NoSuchElementException();
            }
            return o;
        }
    }
}
