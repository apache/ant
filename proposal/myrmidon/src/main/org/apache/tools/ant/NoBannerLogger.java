/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant;

/**
 * Extends DefaultLogger to strip out empty targets.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class NoBannerLogger extends DefaultLogger
{
    private final static String lSep = System.getProperty( "line.separator" );

    protected String targetName;

    public void messageLogged( BuildEvent event )
    {

        if( event.getPriority() > msgOutputLevel ||
            null == event.getMessage() ||
            "".equals( event.getMessage().trim() ) )
        {
            return;
        }

        if( null != targetName )
        {
            out.println( lSep + targetName + ":" );
            targetName = null;
        }

        super.messageLogged( event );
    }

    public void targetFinished( BuildEvent event )
    {
        targetName = null;
    }

    public void targetStarted( BuildEvent event )
    {
        targetName = event.getTarget().getName();
    }
}
