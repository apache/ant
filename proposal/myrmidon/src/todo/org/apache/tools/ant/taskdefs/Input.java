/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;

/**
 * Ant task to read input line from console.
 *
 * @author Ulrich Schmidt <usch@usch.net>
 */
public class Input extends AbstractTask
{
    private String validargs = null;
    private String message = "";
    private String addproperty = null;
    private String input = null;

    /**
     * No arg constructor.
     */
    public Input()
    {
    }

    /**
     * Defines the name of a property to be created from input. Behaviour is
     * according to property task which means that existing properties cannot be
     * overriden.
     *
     * @param addproperty Name for the property to be created from input
     */
    public void setAddproperty( String addproperty )
    {
        this.addproperty = addproperty;
    }

    /**
     * Sets the Message which gets displayed to the user during the build run.
     *
     * @param message The message to be displayed.
     */
    public void setMessage( String message )
    {
        this.message = message;
    }

    /**
     * Sets surrogate input to allow automated testing.
     *
     * @param testinput The new Testinput value
     */
    public void setTestinput( String testinput )
    {
        this.input = testinput;
    }

    /**
     * Defines valid input parameters as comma separated String. If set, input
     * task will reject any input not defined as accepted and requires the user
     * to reenter it. Validargs are case sensitive. If you want 'a' and 'A' to
     * be accepted you need to define both values as accepted arguments.
     *
     * @param validargs A comma separated String defining valid input args.
     */
    public void setValidargs( String validargs )
    {
        this.validargs = validargs;
    }

    // copied n' pasted from org.apache.tools.ant.taskdefs.Exit
    /**
     * Set a multiline message.
     *
     * @param msg The feature to be added to the Text attribute
     */
    public void addContent( String msg )
        throws TaskException
    {
        message = msg;
    }

    /**
     * Actual test method executed by jakarta-ant.
     *
     * @exception TaskException
     */
    public void execute()
        throws TaskException
    {
        ArrayList accept = null;
        if( validargs != null )
        {
            accept = new ArrayList();
            StringTokenizer stok = new StringTokenizer( validargs, ",", false );
            while( stok.hasMoreTokens() )
            {
                accept.add( stok.nextToken() );
            }
        }
        getLogger().warn( message );
        if( input == null )
        {
            try
            {
                BufferedReader in = new BufferedReader( new InputStreamReader( System.in ) );
                input = in.readLine();
                if( accept != null )
                {
                    while( !accept.contains( input ) )
                    {
                        getLogger().warn( message );
                        input = in.readLine();
                    }
                }
            }
            catch( IOException e )
            {
                throw new TaskException( "Failed to read input from Console.", e );
            }
        }
        // not quite the original intention of this task but for the sake
        // of testing ;-)
        else
        {
            if( accept != null && ( !accept.contains( input ) ) )
            {
                throw new TaskException( "Invalid input please reenter." );
            }
        }
        // adopted from org.apache.tools.ant.taskdefs.Property
        if( addproperty != null )
        {
            final String name = addproperty;
            final Object value = input;
            getContext().setProperty( name, value );
        }
    }
}


