/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.jsp.compilers;

import java.util.ArrayList;
import java.util.Iterator;
import org.apache.avalon.excalibur.util.StringUtil;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.tools.ant.taskdefs.optional.jsp.JspC;
import org.apache.tools.ant.types.Commandline;

/**
 * This is the default implementation for the CompilerAdapter interface. This is
 * currently very light on the ground since only one compiler type is supported.
 *
 * @author Matthew Watson <a href="mailto:mattw@i3sp.com">mattw@i3sp.com</a>
 */
public abstract class DefaultCompilerAdapter
    extends AbstractLogEnabled
    implements CompilerAdapter
{
    private JspC m_attributes;

    public void setJspc( final JspC attributes )
    {
        this.m_attributes = attributes;
    }

    public JspC getJspc()
    {
        return m_attributes;
    }

    /*
     * ------------------------------------------------------------
     */
    /**
     * Logs the compilation parameters, adds the files to compile and logs the
     * &qout;niceSourceList&quot;
     *
     * @param jspc Description of Parameter
     * @param compileList Description of Parameter
     * @param cmd Description of Parameter
     */
    protected void logAndAddFilesToCompile( JspC jspc,
                                            ArrayList compileList,
                                            Commandline cmd )
    {
        getLogger().debug( "Compilation args: " + cmd.toString() );

        StringBuffer niceSourceList = new StringBuffer( "File" );
        if( compileList.size() != 1 )
        {
            niceSourceList.append( "s" );
        }
        niceSourceList.append( " to be compiled:" );

        niceSourceList.append( StringUtil.LINE_SEPARATOR );

        Iterator enum = compileList.iterator();
        while( enum.hasNext() )
        {
            String arg = (String)enum.next();
            cmd.createArgument().setValue( arg );
            niceSourceList.append( "    " + arg + StringUtil.LINE_SEPARATOR );
        }

        getLogger().debug( niceSourceList.toString() );
    }
}

