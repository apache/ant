/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ScannerUtil;
import org.apache.tools.ant.types.SourceFileScanner;
import org.apache.tools.ant.util.mappers.FileNameMapper;
import org.apache.tools.ant.util.mappers.Mapper;
import org.apache.tools.ant.util.mappers.MergingMapper;

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

public class UpToDate extends MatchingTask
{
    private ArrayList sourceFileSets = new ArrayList();

    protected Mapper mapperElement = null;

    private String _property;
    private File _targetFile;
    private String _value;

    /**
     * The property to set if the target file is more up to date than each of
     * the source files.
     *
     * @param property the name of the property to set if Target is up to date.
     */
    public void setProperty( String property )
    {
        _property = property;
    }

    /**
     * The file which must be more up to date than each of the source files if
     * the property is to be set.
     *
     * @param file the file which we are checking against.
     */
    public void setTargetFile( File file )
    {
        _targetFile = file;
    }

    /**
     * The value to set the named property to if the target file is more up to
     * date than each of the source files. Defaults to 'true'.
     *
     * @param value the value to set the property to if Target is up to date
     */
    public void setValue( String value )
    {
        _value = value;
    }

    /**
     * Nested &lt;srcfiles&gt; element.
     *
     * @param fs The feature to be added to the Srcfiles attribute
     */
    public void addSrcfiles( FileSet fs )
    {
        sourceFileSets.add( fs );
    }

    /**
     * Defines the FileNameMapper to use (nested mapper element).
     *
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     */
    public Mapper createMapper()
        throws TaskException
    {
        if( mapperElement != null )
        {
            throw new TaskException( "Cannot define more than one mapper" );
        }
        mapperElement = new Mapper();
        return mapperElement;
    }

    /**
     * Evaluate all target and source files, see if the targets are up-to-date.
     *
     * @return Description of the Returned Value
     */
    public boolean eval()
        throws TaskException
    {
        if( sourceFileSets.size() == 0 )
        {
            throw new TaskException( "At least one <srcfiles> element must be set" );
        }

        if( _targetFile == null && mapperElement == null )
        {
            throw new TaskException( "The targetfile attribute or a nested mapper element must be set" );
        }

        // if not there then it can't be up to date
        if( _targetFile != null && !_targetFile.exists() )
        {
            return false;
        }

        Iterator enum = sourceFileSets.iterator();
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
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        boolean upToDate = eval();
        if( upToDate )
        {
            final String name = _property;
            final Object value = this.getValue();
            getContext().setProperty( name, value );
            if( mapperElement == null )
            {
                getLogger().debug( "File \"" + _targetFile.getAbsolutePath() + "\" is up to date." );
            }
            else
            {
                getLogger().debug( "All target files have been up to date." );
            }
        }
    }

    protected boolean scanDir( File srcDir, String files[] )
        throws TaskException
    {
        SourceFileScanner scanner = new SourceFileScanner();
        setupLogger( scanner );
        FileNameMapper mapper = null;
        File dir = srcDir;
        if( mapperElement == null )
        {
            MergingMapper mm = new MergingMapper();
            mm.setTo( _targetFile.getAbsolutePath() );
            mapper = mm;
            dir = null;
        }
        else
        {
            mapper = mapperElement.getImplementation();
        }
        return scanner.restrict( files, srcDir, dir, mapper ).length == 0;
    }

    /**
     * Returns the value, or "true" if a specific value wasn't provided.
     *
     * @return The Value value
     */
    private String getValue()
    {
        return ( _value != null ) ? _value : "true";
    }
}
