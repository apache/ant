/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.types;// java io classes

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.ProjectComponent;

/**
 * A set of filters to be applied to something. A filter set may have begintoken
 * and endtokens defined.
 *
 * @author <A href="mailto:gholam@xtra.co.nz"> Michael McCallum </A>
 * @created 14 March 2001
 */
public class FilterSet
    extends ProjectComponent
    implements Cloneable
{
    /**
     * The default token start string
     */
    private final static String DEFAULT_TOKEN_START = "@";

    /**
     * The default token end string
     */
    private final static String DEFAULT_TOKEN_END = "@";

    /**
     * List of ordered filters and filter files.
     */
    private ArrayList m_filters = new ArrayList();

    /**
     * set the file containing the filters for this filterset.
     *
     * @param filtersFile sets the filter fil to read filters for this filter
     *      set from.
     * @exception TaskException if there is a problem reading the filters
     */
    public void setFiltersfile( File filtersFile )
        throws TaskException
    {
        readFiltersFromFile( filtersFile );
    }

    /**
     * Gets the filter hash of the FilterSet.
     *
     * @return The hash of the tokens and values for quick lookup.
     */
    public Hashtable getFilterHash()
        throws TaskException
    {
        final int filterSize = m_filters.size();
        final Hashtable filterHash = new Hashtable( filterSize );
        final Iterator e = m_filters.iterator();
        while( e.hasNext() )
        {
            final Filter filter = (Filter)e.next();
            filterHash.put( filter.getToken(), filter.getValue() );
        }
        return filterHash;
    }

    /**
     * Create a new filter
     *
     * @param filter The feature to be added to the Filter attribute
     */
    public void addFilter( Filter filter )
    {
        m_filters.add( filter );
    }

    /**
     * Add a new filter made from the given token and value.
     *
     * @param token The token for the new filter.
     * @param value The value for the new filter.
     */
    public void addFilter( final String token, final String value )
    {
        m_filters.add( new Filter( token, value ) );
    }

    /**
     * Add a Filterset to this filter set
     *
     * @param filterSet the filterset to be added to this filterset
     */
    public void addFilterSet( final FilterSet filterSet )
    {
        final Iterator e = filterSet.m_filters.iterator();
        while( e.hasNext() )
        {
            m_filters.add( (Filter)e.next() );
        }
    }

    /**
     * Create a new FiltersFile
     *
     * @return The filter that was created.
     */
    public FiltersFile createFiltersfile()
    {
        return new FiltersFile();
    }

    /**
     * Test to see if this filter set it empty.
     *
     * @return Return true if there are filter in this set otherwise false.
     */
    public boolean hasFilters()
        throws TaskException
    {
        return m_filters.size() > 0;
    }

    /**
     * Read the filters from the given file.
     *
     * @param filtersFile the file from which filters are read
     * @exception TaskException Throw a build exception when unable to read the
     *      file.
     */
    public void readFiltersFromFile( File filtersFile )
        throws TaskException
    {
        if( filtersFile.isFile() )
        {
            getLogger().debug( "Reading filters from " + filtersFile );
            FileInputStream in = null;
            try
            {
                Properties props = new Properties();
                in = new FileInputStream( filtersFile );
                props.load( in );

                Enumeration enum = props.propertyNames();
                ArrayList filters = m_filters;
                while( enum.hasMoreElements() )
                {
                    String strPropName = (String)enum.nextElement();
                    String strValue = props.getProperty( strPropName );
                    filters.add( new Filter( strPropName, strValue ) );
                }
            }
            catch( Exception e )
            {
                throw new TaskException( "Could not read filters from file: " + filtersFile );
            }
            finally
            {
                if( in != null )
                {
                    try
                    {
                        in.close();
                    }
                    catch( IOException ioex )
                    {
                    }
                }
            }
        }
        else
        {
            throw new TaskException( "Must specify a file not a directory in the filtersfile attribute:" + filtersFile );
        }
    }

    /**
     * Does replacement on the given string with token matching. This uses the
     * defined begintoken and endtoken values which default to @ for both.
     *
     * @param line The line to process the tokens in.
     * @return The string with the tokens replaced.
     */
    public String replaceTokens( final String line )
        throws TaskException
    {
        int index = line.indexOf( DEFAULT_TOKEN_START );
        if( -1 == index )
        {
            return line;
        }

        Hashtable tokens = getFilterHash();
        try
        {
            StringBuffer b = new StringBuffer();
            int i = 0;
            String token = null;
            String value = null;

            do
            {
                int endIndex = line.indexOf( DEFAULT_TOKEN_END, index + DEFAULT_TOKEN_START.length() + 1 );
                if( endIndex == -1 )
                {
                    break;
                }
                token = line.substring( index + DEFAULT_TOKEN_START.length(), endIndex );
                b.append( line.substring( i, index ) );
                if( tokens.containsKey( token ) )
                {
                    value = (String)tokens.get( token );
                    getLogger().debug( "Replacing: " + DEFAULT_TOKEN_START + token + DEFAULT_TOKEN_END + " -> " + value );
                    b.append( value );
                    i = index + DEFAULT_TOKEN_START.length() + token.length() + DEFAULT_TOKEN_END.length();
                }
                else
                {
                    // just append beginToken and search further
                    b.append( DEFAULT_TOKEN_START );
                    i = index + DEFAULT_TOKEN_START.length();
                }
            } while( ( index = line.indexOf( DEFAULT_TOKEN_START, i ) ) > -1 );

            b.append( line.substring( i ) );
            return b.toString();
        }
        catch( StringIndexOutOfBoundsException e )
        {
            return line;
        }
    }

    /**
     * The filtersfile nested element.
     *
     * @author Michael McCallum
     * @created Thursday, April 19, 2001
     */
    public class FiltersFile
    {
        /**
         * Sets the file from which filters will be read.
         *
         * @param file the file from which filters will be read.
         */
        public void setFile( final File file )
            throws TaskException
        {
            readFiltersFromFile( file );
        }
    }
}


