/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.util.regexp;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

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

    public RegexpMatcherFactory() { }

    /**
     * Create a new regular expression instance.
     *
     * @return Description of the Returned Value
     * @exception BuildException Description of Exception
     */
    public RegexpMatcher newRegexpMatcher()
        throws BuildException
    {
        return newRegexpMatcher( null );
    }

    /**
     * Create a new regular expression instance.
     *
     * @param p Project whose ant.regexp.regexpimpl property will be used.
     * @return Description of the Returned Value
     * @exception BuildException Description of Exception
     */
    public RegexpMatcher newRegexpMatcher( Project p )
        throws BuildException
    {
        String systemDefault = null;
        if( p == null )
        {
            systemDefault = System.getProperty( "ant.regexp.regexpimpl" );
        }
        else
        {
            systemDefault = ( String )p.getProperties().get( "ant.regexp.regexpimpl" );
        }

        if( systemDefault != null )
        {
            return createInstance( systemDefault );
            // XXX     should we silently catch possible exceptions and try to
            //         load a different implementation?
        }

        try
        {
            return createInstance( "org.apache.tools.ant.util.regexp.Jdk14RegexpMatcher" );
        }
        catch( BuildException be )
        {}

        try
        {
            return createInstance( "org.apache.tools.ant.util.regexp.JakartaOroMatcher" );
        }
        catch( BuildException be )
        {}

        try
        {
            return createInstance( "org.apache.tools.ant.util.regexp.JakartaRegexpMatcher" );
        }
        catch( BuildException be )
        {}

        throw new BuildException( "No supported regular expression matcher found" );
    }

    protected RegexpMatcher createInstance( String className )
        throws BuildException
    {
        try
        {
            Class implClass = Class.forName( className );
            return ( RegexpMatcher )implClass.newInstance();
        }
        catch( Throwable t )
        {
            throw new BuildException( "Error", t );
        }
    }
}
