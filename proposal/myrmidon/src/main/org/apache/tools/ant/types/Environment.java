/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.types;

import java.util.ArrayList;
import org.apache.myrmidon.api.TaskException;

/**
 * Wrapper for environment variables.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class Environment
{
    protected ArrayList variables;

    public Environment()
    {
        variables = new ArrayList();
    }

    public String[] getVariables()
        throws TaskException
    {
        if( variables.size() == 0 )
        {
            return null;
        }
        String[] result = new String[ variables.size() ];
        for( int i = 0; i < result.length; i++ )
        {
            result[ i ] = ( (Variable)variables.get( i ) ).getContent();
        }
        return result;
    }

    public void addVariable( Variable var )
    {
        variables.add( var );
    }

    public static class Variable
    {
        private String key, value;

        public Variable()
        {
            super();
        }

        public void setFile( java.io.File file )
        {
            this.value = file.getAbsolutePath();
        }

        public void setKey( String key )
        {
            this.key = key;
        }

        public void setPath( Path path )
        {
            this.value = path.toString();
        }

        public void setValue( String value )
        {
            this.value = value;
        }

        public String getContent()
            throws TaskException
        {
            if( key == null || value == null )
            {
                throw new TaskException( "key and value must be specified for environment variables." );
            }
            StringBuffer sb = new StringBuffer( key.trim() );
            sb.append( "=" ).append( value.trim() );
            return sb.toString();
        }

        public String getKey()
        {
            return this.key;
        }

        public String getValue()
        {
            return this.value;
        }
    }
}
