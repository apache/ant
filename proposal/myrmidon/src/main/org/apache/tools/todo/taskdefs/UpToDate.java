/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.framework.FileNameMapper;
import org.apache.tools.todo.types.DirectoryScanner;
import org.apache.tools.todo.types.FileSet;
import org.apache.tools.todo.types.ScannerUtil;
import org.apache.tools.todo.types.SourceFileScanner;
import org.apache.tools.todo.util.mappers.MergingMapper;

/**
 * Will set the given property if the specified target has a timestamp greater
 * than all of the source files.
 *
 * @author William Ferguson <a href="mailto:williamf@mincom.com">
 *      williamf@mincom.com</a>
 * @author Hiroaki Nakamura <a href="mailto:hnakamur@mc.neweb.ne.jp">
 *      hnakamur@mc.neweb.ne.jp</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class UpToDate
    extends AbstractTask
{
    private final ArrayList m_fileSets = new ArrayList();
    private FileNameMapper m_mapper;

    private String m_property;
    private File m_targetFile;
    private String m_value;

    /**
     * The property to set if the target file is more up to date than each of
     * the source files.
     *
     * @param property the name of the property to set if Target is up to date.
     */
    public void setProperty( final String property )
    {
        m_property = property;
    }

    /**
     * The file which must be more up to date than each of the source files if
     * the property is to be set.
     *
     * @param file the file which we are checking against.
     */
    public void setTargetFile( final File file )
    {
        m_targetFile = file;
    }

    /**
     * The value to set the named property to if the target file is more up to
     * date than each of the source files. Defaults to 'true'.
     *
     * @param value the value to set the property to if Target is up to date
     */
    public void setValue( final String value )
    {
        m_value = value;
    }

    /**
     * Nested &lt;srcfiles&gt; element.
     *
     * @param fs The feature to be added to the Srcfiles attribute
     */
    public void addSrcfiles( final FileSet fs )
    {
        m_fileSets.add( fs );
    }

    /**
     * Defines the FileNameMapper to use (nested mapper element).
     */
    public void addMapper( final FileNameMapper mapper )
        throws TaskException
    {
        if( m_mapper != null )
        {
            throw new TaskException( "Cannot define more than one mapper" );
        }
        m_mapper = mapper;
    }

    /**
     * Evaluate all target and source files, see if the targets are up-to-date.
     *
     * @return Description of the Returned Value
     */
    public boolean eval()
        throws TaskException
    {
        if( m_fileSets.size() == 0 )
        {
            throw new TaskException( "At least one <srcfiles> element must be set" );
        }

        if( m_targetFile == null && m_mapper == null )
        {
            throw new TaskException( "The targetfile attribute or a nested mapper element must be set" );
        }

        // if not there then it can't be up to date
        if( m_targetFile != null && !m_targetFile.exists() )
        {
            return false;
        }

        Iterator enum = m_fileSets.iterator();
        boolean upToDate = true;
        while( upToDate && enum.hasNext() )
        {
            FileSet fs = (FileSet)enum.next();
            DirectoryScanner ds = ScannerUtil.getDirectoryScanner( fs );
            upToDate = upToDate && scanDir( fs.getDir(),
                                            ds.getIncludedFiles() );
        }
        return upToDate;
    }

    /**
     * Sets property to true if target files have a more recent timestamp than
     * each of the corresponding source files.
     *
     * @exception org.apache.myrmidon.api.TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        boolean upToDate = eval();
        if( upToDate )
        {
            final String name = m_property;
            final Object value = this.getValue();
            getContext().setProperty( name, value );
            if( m_mapper == null )
            {
                getContext().debug( "File \"" + m_targetFile.getAbsolutePath() + "\" is up to date." );
            }
            else
            {
                getContext().debug( "All target files have been up to date." );
            }
        }
    }

    protected boolean scanDir( File srcDir, String files[] )
        throws TaskException
    {
        SourceFileScanner scanner = new SourceFileScanner();
        FileNameMapper mapper = null;
        File dir = srcDir;
        if( m_mapper == null )
        {
            MergingMapper mm = new MergingMapper();
            mm.setTo( m_targetFile.getAbsolutePath() );
            mapper = mm;
            dir = null;
        }
        else
        {
            mapper = m_mapper;
        }
        return scanner.restrict( files, srcDir, dir, mapper, getContext() ).length == 0;
    }

    /**
     * Returns the value, or "true" if a specific value wasn't provided.
     *
     * @return The Value value
     */
    private String getValue()
    {
        return ( m_value != null ) ? m_value : "true";
    }
}
