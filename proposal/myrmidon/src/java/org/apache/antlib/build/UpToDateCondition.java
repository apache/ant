/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.build;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.FileNameMapper;
import org.apache.myrmidon.framework.conditions.Condition;
import org.apache.tools.todo.types.DirectoryScanner;
import org.apache.tools.todo.types.FileSet;
import org.apache.tools.todo.types.ScannerUtil;
import org.apache.tools.todo.types.SourceFileScanner;
import org.apache.tools.todo.util.mappers.MergingMapper;

/**
 * A condition which evaluates to true when the specified target has a
 * timestamp greater than all of the source files.
 *
 * @author William Ferguson <a href="mailto:williamf@mincom.com">
 *      williamf@mincom.com</a>
 * @author Hiroaki Nakamura <a href="mailto:hnakamur@mc.neweb.ne.jp">
 *      hnakamur@mc.neweb.ne.jp</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 *
 * @ant.type type="condition" name="uptodate"
 */
public class UpToDateCondition
    implements Condition
{
    private final ArrayList m_fileSets = new ArrayList();
    private FileNameMapper m_mapper;
    private File m_targetFile;

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
    public void add( final FileNameMapper mapper )
        throws TaskException
    {
        if( m_mapper != null )
        {
            throw new TaskException( "Cannot define more than one mapper" );
        }
        m_mapper = mapper;
    }

    /**
     * Evaluates this condition.
     *
     * @param context
     *      The context to evaluate the condition in.
     */
    public boolean evaluate( TaskContext context )
        throws TaskException
    {
        if( m_targetFile == null && m_mapper == null )
        {
            throw new TaskException( "The targetfile attribute or a nested mapper element must be set" );
        }

        // if not there then it can't be up to date
        if( m_targetFile != null && !m_targetFile.exists() )
        {
            return false;
        }

        final Iterator enum = m_fileSets.iterator();
        while( enum.hasNext() )
        {
            final FileSet fs = (FileSet)enum.next();
            final DirectoryScanner ds = ScannerUtil.getDirectoryScanner( fs );
            if ( !scanDir( fs.getDir(), ds.getIncludedFiles(), context ) )
            {
                return false;
            }
        }
        return true;
    }

    private boolean scanDir( final File srcDir,
                             final String files[],
                             final TaskContext context )
        throws TaskException
    {
        final SourceFileScanner scanner = new SourceFileScanner();
        FileNameMapper mapper = null;
        File dir = srcDir;
        if( m_mapper == null )
        {
            final MergingMapper mm = new MergingMapper();
            mm.setTo( m_targetFile.getAbsolutePath() );
            mapper = mm;
            dir = null;
        }
        else
        {
            mapper = m_mapper;
        }
        return scanner.restrict( files, srcDir, dir, mapper, context ).length == 0;
    }
}
