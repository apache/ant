/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.project;

import org.apache.log.format.PatternFormatter;
import org.apache.log.output.DefaultOutputLogTarget;

public class DefaultProjectListener
    extends DefaultOutputLogTarget
    implements ProjectListener
{
    protected String        m_prefix;

    /**
     * Initialize the default pattern.
     */
    protected void initPattern()
    {
        final PatternFormatter formatrer = new PatternFormatter();
        formatrer.setFormat( "%{message}\\n%{throwable}" );
        m_formatter = formatrer;
    }

    public void projectStarted( final String projectName )
    {
        output( "Starting project " + projectName + "\n" );
    }

    public void projectFinished()
    {
    }
    
    public void targetStarted( final String targetName )
    {
        output( targetName + ":\n" );
    }

    public void targetFinished()
    {
    }
    
    public void taskletStarted( final String taskletName )
    {
        m_prefix = taskletName;
    }

    public void taskletFinished()
    {
        m_prefix = null;
    }

    protected void output( final String data )
    {
        if( null != m_prefix ) super.output( "[" + m_prefix + "] " + data );
        else super.output( data );
    }
}
