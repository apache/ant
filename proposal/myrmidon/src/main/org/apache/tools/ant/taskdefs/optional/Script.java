/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional;

import com.ibm.bsf.BSFException;
import com.ibm.bsf.BSFManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Task;

/**
 * Execute a script
 *
 * @author Sam Ruby <a href="mailto:rubys@us.ibm.com">rubys@us.ibm.com</a>
 */
public class Script extends Task
{
    private String script = "";
    private Hashtable beans = new Hashtable();
    private String language;

    /**
     * Defines the language (required).
     *
     * @param language The new Language value
     */
    public void setLanguage( String language )
    {
        this.language = language;
    }

    /**
     * Load the script from an external file
     *
     * @param fileName The new Src value
     */
    public void setSrc( String fileName )
    {
        File file = new File( fileName );
        if( !file.exists() ) {
            throw new TaskException( "file " + fileName + " not found." );
        }

        int count = (int)file.length();
        byte data[] = new byte[ count ];

        try
        {
            FileInputStream inStream = new FileInputStream( file );
            inStream.read( data );
            inStream.close();
        }
        catch( IOException e )
        {
            throw new TaskException( "Error", e );
        }

        script += new String( data );
    }

    /**
     * Defines the script.
     *
     * @param text The feature to be added to the Text attribute
     */
    public void addContent( String text )
    {
        this.script += text;
    }

    /**
     * Do the work.
     *
     * @exception TaskException if someting goes wrong with the build
     */
    public void execute()
        throws TaskException
    {
        try
        {
            addBeans( getProject().getProperties() );
            //In Ant2 there is no difference between properties and references
            //addBeans( getProject().getReferences() );

            beans.put( "context", getContext() );

            beans.put( "self", this );

            BSFManager manager = new BSFManager();

            for( Iterator e = beans.keys(); e.hasNext(); )
            {
                String key = (String)e.next();
                Object value = beans.get( key );
                manager.declareBean( key, value, value.getClass() );
            }

            // execute the script
            manager.exec( language, "<ANT>", 0, 0, script );
        }
        catch( BSFException be )
        {
            Throwable t = be;
            Throwable te = be.getTargetException();
            if( te != null )
            {
                if( te instanceof TaskException )
                {
                    throw (TaskException)te;
                }
                else
                {
                    t = te;
                }
            }
            throw new TaskException( "Error", t );
        }
    }

    /**
     * Add a list of named objects to the list to be exported to the script
     *
     * @param dictionary The feature to be added to the Beans attribute
     */
    private void addBeans( Hashtable dictionary )
    {
        for( Iterator e = dictionary.keys(); e.hasNext(); )
        {
            String key = (String)e.next();

            boolean isValid = key.length() > 0 &&
                Character.isJavaIdentifierStart( key.charAt( 0 ) );

            for( int i = 1; isValid && i < key.length(); i++ ) {
                isValid = Character.isJavaIdentifierPart( key.charAt( i ) );
            }

            if( isValid ) {
                beans.put( key, dictionary.get( key ) );
            }
        }
    }
}
