/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.util.regexp;

import org.apache.myrmidon.api.TaskException;

/**
 * Regular expression factory, which will create Regexp objects. The actual
 * implementation class depends on the System or Ant Property: <code>ant.regexp.regexpimpl</code>
 * .
 *
 * @author Matthew Inger <a href="mailto:mattinger@mindless.com">
 *      mattinger@mindless.com</a>
 * @version $Revision$
 */
public class RegexpFactory
    extends RegexpMatcherFactory
{
    /**
     * Create a new regular expression matcher instance.
     */
    public Regexp newRegexp()
        throws TaskException
    {
        final String systemDefault = System.getProperty( "ant.regexp.regexpimpl" );
        if( systemDefault != null )
        {
            return createRegexpInstance( systemDefault );
            // XXX     should we silently catch possible exceptions and try to
            //         load a different implementation?
        }

        try
        {
            return createRegexpInstance( JDK14_REGEXP );
        }
        catch( TaskException be )
        {
        }

        try
        {
            return createRegexpInstance( JAKARTA_ORO );
        }
        catch( TaskException be )
        {
        }

        try
        {
            return createRegexpInstance( JAKARTA_REGEXP );
        }
        catch( TaskException be )
        {
        }

        final String message = "No supported regular expression matcher found";
        throw new TaskException( message );
    }

    /**
     * Wrapper over {@seee RegexpMatcherFactory#createInstance createInstance}
     * that ensures that we are dealing with a Regexp implementation.
     *
     * @param classname Description of Parameter
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     * @since 1.3
     */
    private Regexp createRegexpInstance( final String classname )
        throws TaskException
    {
        final RegexpMatcher m = createInstance( classname );
        if( m instanceof Regexp )
        {
            return (Regexp)m;
        }
        else
        {
            throw new TaskException( classname + " doesn't implement the Regexp interface" );
        }
    }

}
