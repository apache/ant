/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.sitraka;

import java.util.ArrayList;

/**
 * Filters information from coverage, somewhat similar to a <tt>FileSet</tt> .
 *
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 */
public class Filters
{

    /**
     * default regexp to exclude everything
     */
    public final static String DEFAULT_EXCLUDE = "*.*():E";

    /**
     * say whether we should use the default excludes or not
     */
    protected boolean defaultExclude = true;

    /**
     * user defined filters
     */
    protected ArrayList filters = new ArrayList();

    public Filters()
    {
    }

    public void setDefaultExclude( boolean value )
    {
        defaultExclude = value;
    }

    public void addExclude( Exclude excl )
    {
        filters.add( excl );
    }

    public void addInclude( Include incl )
    {
        filters.add( incl );
    }

    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        final int size = filters.size();
        if( defaultExclude )
        {
            buf.append( DEFAULT_EXCLUDE );
            if( size > 0 )
            {
                buf.append( ',' );
            }
        }
        for( int i = 0; i < size; i++ )
        {
            buf.append( filters.get( i ).toString() );
            if( i < size - 1 )
            {
                buf.append( ',' );
            }
        }
        return buf.toString();
    }

    public static class Exclude extends FilterElement
    {
        public String toString()
        {
            return super.toString() + ":E" + ( enabled ? "" : "#" );
        }
    }

    public abstract static class FilterElement
    {
        protected String method = "*";// default is all methods
        protected boolean enabled = true;
        protected String clazz;

        public void setClass( String value )
        {
            clazz = value;
        }

        public void setEnabled( boolean value )
        {
            enabled = value;
        }

        public void setMethod( String value )
        {
            method = value;
        }// default is enable

        public void setName( String value )
        {// this one is deprecated.
            clazz = value;
        }

        public String toString()
        {
            return clazz + "." + method + "()";
        }
    }

    public static class Include extends FilterElement
    {
        public String toString()
        {
            return super.toString() + ":I" + ( enabled ? "" : "#" );
        }
    }
}


