/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.text;

import org.apache.myrmidon.api.TaskException;
import org.apache.tools.todo.taskdefs.text.Replace;

public class Replacefilter
{
    private String m_property;
    private String m_token;
    private String m_value;
    private Replace m_replace;

    public Replacefilter( Replace replace )
    {
        m_replace = replace;
    }

    public void setProperty( final String property )
    {
        this.m_property = property;
    }

    public void setToken( String token )
    {
        this.m_token = token;
    }

    public void setValue( String value )
    {
        this.m_value = value;
    }

    public String getProperty()
    {
        return m_property;
    }

    public String getReplaceValue()
    {
        if( m_property != null )
        {
            return (String)m_replace.getProperties().getProperty( m_property );
        }
        else if( m_value != null )
        {
            return m_value;
        }
        else if( m_replace.getValue() != null )
        {
            return m_replace.getValue().getText();
        }
        else
        {
            //Default is empty string
            return "";
        }
    }

    public String getToken()
    {
        return m_token;
    }

    public String getValue()
    {
        return m_value;
    }

    public void validate()
        throws TaskException
    {
        //Validate mandatory attributes
        if( m_token == null )
        {
            String message = "token is a mandatory attribute " + "of replacefilter.";
            throw new TaskException( message );
        }

        if( "".equals( m_token ) )
        {
            String message = "The token attribute must not be an empty string.";
            throw new TaskException( message );
        }

        //value and property are mutually exclusive attributes
        if( ( m_value != null ) && ( m_property != null ) )
        {
            String message = "Either value or property " + "can be specified, but a replacefilter " + "element cannot have both.";
            throw new TaskException( message );
        }

        if( ( m_property != null ) )
        {
            //the property attribute must have access to a property file
            if( m_replace.getPropertyFile() == null )
            {
                String message = "The replacefilter's property attribute " +
                    "can only be used with the replacetask's propertyFile attribute.";
                throw new TaskException( message );
            }

            //Make sure property exists in property file
            if( m_replace.getProperties() == null ||
                m_replace.getProperties().getProperty( m_property ) == null )
            {
                String message = "property \"" + m_property + "\" was not found in " + m_replace.getPropertyFile().getPath();
                throw new TaskException( message );
            }
        }
    }
}
