/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.jsp.compilers;

import java.util.Iterator;
import java.util.ArrayList;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.jsp.JspC;
import org.apache.tools.ant.types.Commandline;

/**
 * This is the default implementation for the CompilerAdapter interface. This is
 * currently very light on the ground since only one compiler type is supported.
 *
 * @author Matthew Watson <a href="mailto:mattw@i3sp.com">mattw@i3sp.com</a>
 */
public abstract class DefaultCompilerAdapter
    implements CompilerAdapter
{
    /*
     * ------------------------------------------------------------
     */
    private static String lSep = System.getProperty( "line.separator" );
    /*
     * ------------------------------------------------------------
     */
    protected JspC attributes;

    public void setJspc( JspC attributes )
    {
        this.attributes = attributes;
    }

    public JspC getJspc()
    {
        return attributes;
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
        jspc.log( "Compilation args: " + cmd.toString(), Project.MSG_VERBOSE );

        StringBuffer niceSourceList = new StringBuffer( "File" );
        if( compileList.size() != 1 )
        {
            niceSourceList.append( "s" );
        }
        niceSourceList.append( " to be compiled:" );

        niceSourceList.append( lSep );

        Iterator enum = compileList.iterator();
        while( enum.hasNext() )
        {
            String arg = (String)enum.next();
            cmd.createArgument().setValue( arg );
            niceSourceList.append( "    " + arg + lSep );
        }

        jspc.log( niceSourceList.toString(), Project.MSG_VERBOSE );
    }
    /*
     * ------------------------------------------------------------
     */
}

