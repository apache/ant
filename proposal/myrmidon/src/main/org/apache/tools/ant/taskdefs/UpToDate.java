/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;
import java.io.File;
import java.util.Enumeration;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.MergingMapper;
import org.apache.tools.ant.util.SourceFileScanner;

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

public class UpToDate extends MatchingTask implements Condition
{
    private Vector sourceFileSets = new Vector();

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
        sourceFileSets.addElement( fs );
    }

    /**
     * Defines the FileNameMapper to use (nested mapper element).
     *
     * @return Description of the Returned Value
     * @exception BuildException Description of Exception
     */
    public Mapper createMapper()
        throws BuildException
    {
        if( mapperElement != null )
        {
            throw new BuildException( "Cannot define more than one mapper"  );
        }
        mapperElement = new Mapper( project );
        return mapperElement;
    }

    /**
     * Evaluate all target and source files, see if the targets are up-to-date.
     *
     * @return Description of the Returned Value
     */
    public boolean eval()
    {
        if( sourceFileSets.size() == 0 )
        {
            throw new BuildException( "At least one <srcfiles> element must be set" );
        }

        if( _targetFile == null && mapperElement == null )
        {
            throw new BuildException( "The targetfile attribute or a nested mapper element must be set" );
        }

        // if not there then it can't be up to date
        if( _targetFile != null && !_targetFile.exists() )
            return false;

        Enumeration enum = sourceFileSets.elements();
        boolean upToDate = true;
        while( upToDate && enum.hasMoreElements() )
        {
            FileSet fs = ( FileSet )enum.nextElement();
            DirectoryScanner ds = fs.getDirectoryScanner( project );
            upToDate = upToDate && scanDir( fs.getDir( project ),
                ds.getIncludedFiles() );
        }
        return upToDate;
    }


    /**
     * Sets property to true if target files have a more recent timestamp than
     * each of the corresponding source files.
     *
     * @exception BuildException Description of Exception
     */
    public void execute()
        throws BuildException
    {
        boolean upToDate = eval();
        if( upToDate )
        {
            this.project.setProperty( _property, this.getValue() );
            if( mapperElement == null )
            {
                log( "File \"" + _targetFile.getAbsolutePath() + "\" is up to date.",
                    Project.MSG_VERBOSE );
            }
            else
            {
                log( "All target files have been up to date.",
                    Project.MSG_VERBOSE );
            }
        }
    }

    protected boolean scanDir( File srcDir, String files[] )
    {
        SourceFileScanner sfs = new SourceFileScanner( this );
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
        return sfs.restrict( files, srcDir, dir, mapper ).length == 0;
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
