/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant;
import java.io.File;
import java.io.FilenameFilter;


/**
 * Filters filenames to determine whether or not the file is desirable.
 *
 * @author Jason Hunter [jhunter@servlets.com]
 * @author james@x180.com
 */
public class DesirableFilter implements FilenameFilter
{

    /**
     * Test the given filename to determine whether or not it's desirable. This
     * helps tasks filter temp files and files used by CVS.
     *
     * @param dir Description of Parameter
     * @param name Description of Parameter
     * @return Description of the Returned Value
     */

    public boolean accept( File dir, String name )
    {

        // emacs save file
        if( name.endsWith( "~" ) )
        {
            return false;
        }

        // emacs autosave file
        if( name.startsWith( "#" ) && name.endsWith( "#" ) )
        {
            return false;
        }

        // openwindows text editor does this I think
        if( name.startsWith( "%" ) && name.endsWith( "%" ) )
        {
            return false;
        }

        /*
         * CVS stuff -- hopefully there won't be a case with
         * an all cap file/dir named "CVS" that somebody wants
         * to keep around...
         */
        if( name.equals( "CVS" ) )
        {
            return false;
        }

        /*
         * If we are going to ignore CVS might as well ignore
         * this one as well...
         */
        if( name.equals( ".cvsignore" ) )
        {
            return false;
        }

        // CVS merge autosaves.
        if( name.startsWith( ".#" ) )
        {
            return false;
        }

        // SCCS/CSSC/TeamWare:
        if( name.equals( "SCCS" ) )
        {
            return false;
        }

        // Visual Source Save
        if( name.equals( "vssver.scc" ) )
        {
            return false;
        }

        // default
        return true;
    }
}




