/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.metamata;

import java.io.File;
import java.util.ArrayList;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskContext;
import org.apache.tools.todo.types.Path;

/**
 * Metamata Audit evaluates Java code for programming errors, weaknesses, and
 * style violation. <p>
 *
 * Metamata Audit exists in three versions:
 * <ul>
 *   <li> The Lite version evaluates about 15 built-in rules.</li>
 *   <li> The Pro version evaluates about 50 built-in rules.</li>
 *   <li> The Enterprise version allows you to add your own customized rules via
 *   the API.</li>
 *   <ul>For more information, visit the website at <a
 *     href="http://www.metamata.com">www.metamata.com</a>
 *
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 */
public class MAudit
    extends AbstractMetamataTask
{
    /*
     * As of Metamata 2.0, the command line of MAudit is as follows:
     * Usage
     * maudit <option>... <path>... [-unused <search-path>...]
     * Parameters
     * path               File or directory to audit.
     * search-path        File or directory to search for declaration uses.
     * Options
     * -arguments  -A     <file>     Includes command line arguments from file.
     * -classpath  -cp    <path>     Sets class path (also source path unless one
     * explicitly set). Overrides METAPATH/CLASSPATH.
     * -exit       -x                Exits after the first error.
     * -fix        -f                Automatically fixes certain errors.
     * -fullpath                     Prints full path for locations.
     * -help       -h                Prints help and exits.
     * -list       -l                Creates listing file for each audited file.
     * -offsets    -off              Offset and length for locations.
     * -output     -o     <file>     Prints output to file.
     * -quiet      -q                Suppresses copyright and summary messages.
     * -sourcepath        <path>     Sets source path. Overrides SOURCEPATH.
     * -tab        -t                Prints a tab character after first argument.
     * -unused     -u                Finds declarations unused in search paths.
     * -verbose    -v                Prints all messages.
     * -version    -V                Prints version and exits.
     */
    //---------------------- PUBLIC METHODS ------------------------------------

    /**
     * pattern used by maudit to report the error for a file
     */
    /**
     * RE does not seems to support regexp pattern with comments so i'm
     * stripping it
     */
    // (?:file:)?((?#filepath).+):((?#line)\\d+)\\s*:\\s+((?#message).*)
    final static String AUDIT_PATTERN = "(?:file:)?(.+):(\\d+)\\s*:\\s+(.*)";

    private File m_outFile;
    private Path m_searchPath;
    private boolean m_fix;
    private boolean m_list;
    private boolean m_unused;

    /**
     * default constructor
     */
    public MAudit()
    {
        super( "com.metamata.gui.rc.MAudit" );
    }

    public void setFix( final boolean fix )
    {
        m_fix = fix;
    }

    public void setList( final boolean list )
    {
        m_list = list;
    }

    /**
     * set the destination file which should be an xml file
     */
    public void setTofile( final File outFile )
    {
        m_outFile = outFile;
    }

    public void setUnused( final boolean unused )
    {
        m_unused = unused;
    }

    public Path createSearchpath()
    {
        if( m_searchPath == null )
        {
            m_searchPath = new Path();
        }
        return m_searchPath;
    }

    protected ArrayList getOptions()
        throws TaskException
    {
        ArrayList options = new ArrayList( 512 );
        // there is a bug in Metamata 2.0 build 37. The sourcepath argument does
        // not work. So we will use the sourcepath prepended to classpath. (order
        // is important since Metamata looks at .class and .java)
        if( getSourcePath() != null )
        {
            getSourcePath().append( getClassPath() );// srcpath is prepended
            setClassPath( getSourcePath() );
            setSourcePath( null );// prevent from using -sourcepath
        }

        // don't forget to modify the pattern if you change the options reporting
        if( getClassPath() != null )
        {
            options.add( "-classpath" );
            options.add( getClassPath().toString() );
        }
        // suppress copyright msg when running, we will let it so that this
        // will be the only output to the console if in xml mode
        //      options.add("-quiet");
        if( m_fix )
        {
            options.add( "-fix" );
        }
        options.add( "-fullpath" );

        // generate .maudit files much more detailed than the report
        // I don't like it very much, I think it could be interesting
        // to get all .maudit files and include them in the XML.
        if( m_list )
        {
            options.add( "-list" );
        }
        if( getSourcePath() != null )
        {
            options.add( "-sourcepath" );
            options.add( getSourcePath().toString() );
        }

        if( m_unused )
        {
            options.add( "-unused" );
            options.add( m_searchPath.toString() );
        }
        addAllArrayList( options, getIncludedFiles().keySet().iterator() );
        return options;
    }

    protected void validate()
        throws TaskException
    {
        super.validate();
        if( m_unused && m_searchPath == null )
        {
            throw new TaskException( "'searchpath' element must be set when looking for 'unused' declarations." );
        }
        if( !m_unused && m_searchPath != null )
        {
            getContext().warn( "'searchpath' element ignored. 'unused' attribute is disabled." );
        }
    }

    protected void cleanUp()
        throws TaskException
    {
        super.cleanUp();
        // at this point if -list is used, we should move
        // the .maudit file since we cannot choose their location :(
        // the .maudit files match the .java files
        // we'll use includedFiles to get the .maudit files.

        /*
         * if (out != null){
         * / close it if not closed by the handler...
         * }
         */
    }
}

