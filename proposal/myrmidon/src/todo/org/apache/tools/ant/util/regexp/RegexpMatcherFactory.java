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
 * Simple Factory Class that produces an implementation of RegexpMatcher based
 * on the system property <code>ant.regexp.matcherimpl</code> and the classes
 * available. <p>
 *
 * In a more general framework this class would be abstract and have a static
 * newInstance method.</p>
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class RegexpMatcherFactory
{
    protected final static String JAKARTA_REGEXP = "org.apache.tools.ant.util.regexp.JakartaRegexpRegexp";
    protected final static String JAKARTA_ORO = "org.apache.tools.ant.util.regexp.JakartaOroRegexp";
    protected final static String JDK14_REGEXP = "org.apache.tools.ant.util.regexp.Jdk14RegexpRegexp";

    /**
     * Create a new regular expression instance.
     *
     * @param p Project whose ant.regexp.regexpimpl property will be used.
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     */
    public RegexpMatcher newRegexpMatcher()
        throws TaskException
    {
        final String systemDefault = System.getProperty( "ant.regexp.regexpimpl" );
        if( systemDefault != null )
        {
            return createInstance( systemDefault );
            // XXX     should we silently catch possible exceptions and try to
            //         load a different implementation?
        }

        try
        {
            return createInstance( JDK14_REGEXP );
        }
        catch( TaskException be )
        {
        }

        try
        {
            return createInstance( JAKARTA_ORO );
        }
        catch( TaskException be )
        {
        }

        try
        {
            return createInstance( JAKARTA_REGEXP );
        }
        catch( TaskException be )
        {
        }

        final String message = "No supported regular expression matcher found";
        throw new TaskException( message );
    }

    protected RegexpMatcher createInstance( final String className )
        throws TaskException
    {
        try
        {
            Class implClass = Class.forName( className );
            return (RegexpMatcher)implClass.newInstance();
        }
        catch( Throwable t )
        {
            throw new TaskException( "Error", t );
        }
    }
}
