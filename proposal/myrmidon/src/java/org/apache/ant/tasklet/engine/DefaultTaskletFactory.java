/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasklet.engine;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import org.apache.ant.tasklet.Tasklet;
import org.apache.ant.convert.ConverterLoader;
import org.apache.ant.convert.DefaultConverterFactory;
import org.apache.avalon.camelot.Entry;
import org.apache.avalon.camelot.Factory;
import org.apache.avalon.camelot.FactoryException;
import org.apache.avalon.camelot.Info;

/**
 * Facility used to load Tasklets.
 * 
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultTaskletFactory
    extends DefaultConverterFactory
    implements TaskletFactory
{
    public Object create( final Info info )
        throws FactoryException
    {
        if( !info.getClass().equals( TaskletInfo.class ) )
        {
            return super.create( info );
        }
        else
        {
            return createTasklet( (TaskletInfo)info );
        }
    }

    public Tasklet createTasklet( final TaskletInfo info )
        throws FactoryException
    {
        final TaskletLoader loader = (TaskletLoader)getLoader( info.getLocation() );

        Object object = null;
        
        try { return (Tasklet)loader.load( info.getClassname() ); }
        catch( final Exception e )
        {
            throw new FactoryException( "Failed loading tasklet from " + info.getLocation() +
                                        " due to " + e, e );
        }
    }

    protected ConverterLoader createLoader( final URL location )
    {
        if( null != location ) return new DefaultTaskletLoader( location );
        else return new DefaultTaskletLoader();
    }
}
