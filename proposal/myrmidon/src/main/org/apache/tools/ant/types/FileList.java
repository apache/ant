/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.types;

import java.io.File;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.ArrayList;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;

/**
 * FileList represents an explicitly named list of files. FileLists are useful
 * when you want to capture a list of files regardless of whether they currently
 * exist. By contrast, FileSet operates as a filter, only returning the name of
 * a matched file if it currently exists in the file system.
 *
 * @author <a href="mailto:cstrong@arielpartners.com">Craeg Strong</a>
 * @version $Revision$ $Date$
 */
public class FileList extends DataType
{

    private ArrayList filenames = new ArrayList();
    private File dir;

    public FileList()
    {
        super();
    }

    protected FileList( FileList filelist )
    {
        this.dir = filelist.dir;
        this.filenames = filelist.filenames;
        setProject( filelist.getProject() );
    }

    public void setDir( File dir )
        throws TaskException
    {
        if( isReference() )
        {
            throw tooManyAttributes();
        }
        this.dir = dir;
    }

    public void setFiles( String filenames )
        throws TaskException
    {
        if( isReference() )
        {
            throw tooManyAttributes();
        }
        if( filenames != null && filenames.length() > 0 )
        {
            StringTokenizer tok = new StringTokenizer( filenames, ", \t\n\r\f", false );
            while( tok.hasMoreTokens() )
            {
                this.filenames.add( tok.nextToken() );
            }
        }
    }

    /**
     * Makes this instance in effect a reference to another FileList instance.
     * <p>
     *
     * You must not set another attribute or nest elements inside this element
     * if you make it a reference.</p>
     *
     * @param r The new Refid value
     * @exception TaskException Description of Exception
     */
    public void setRefid( Reference r )
        throws TaskException
    {
        if( ( dir != null ) || ( filenames.size() != 0 ) )
        {
            throw tooManyAttributes();
        }
        super.setRefid( r );
    }

    public File getDir( Project p )
        throws TaskException
    {
        if( isReference() )
        {
            return getRef( p ).getDir( p );
        }
        return dir;
    }

    /**
     * Returns the list of files represented by this FileList.
     *
     * @param p Description of Parameter
     * @return The Files value
     */
    public String[] getFiles( Project p )
        throws TaskException
    {
        if( isReference() )
        {
            return getRef( p ).getFiles( p );
        }

        if( dir == null )
        {
            throw new TaskException( "No directory specified for filelist." );
        }

        if( filenames.size() == 0 )
        {
            throw new TaskException( "No files specified for filelist." );
        }

        final String result[] = new String[ filenames.size() ];
        return (String[])filenames.toArray( result );
    }

    /**
     * Performs the check for circular references and returns the referenced
     * FileList.
     *
     * @param p Description of Parameter
     * @return The Ref value
     */
    protected FileList getRef( Project p )
        throws TaskException
    {
        if( !checked )
        {
            Stack stk = new Stack();
            stk.push( this );
            dieOnCircularReference( stk, p );
        }

        Object o = ref.getReferencedObject( p );
        if( !( o instanceof FileList ) )
        {
            String msg = ref.getRefId() + " doesn\'t denote a filelist";
            throw new TaskException( msg );
        }
        else
        {
            return (FileList)o;
        }
    }

}//-- FileList.java
