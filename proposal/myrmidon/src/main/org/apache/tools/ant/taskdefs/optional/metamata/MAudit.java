/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.metamata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.exec.Execute;
import org.apache.tools.ant.taskdefs.exec.LogOutputStream;
import org.apache.tools.ant.types.Path;

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
public class MAudit extends AbstractMetamataTask
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

    protected File outFile = null;

    protected Path searchPath = null;

    protected boolean fix = false;

    protected boolean list = false;

    protected boolean unused = false;

    /**
     * default constructor
     */
    public MAudit()
    {
        super( "com.metamata.gui.rc.MAudit" );
    }

    /**
     * handy factory to create a violation
     *
     * @param line Description of Parameter
     * @param msg Description of Parameter
     * @return Description of the Returned Value
     */
    final static Violation createViolation( int line, String msg )
    {
        Violation violation = new Violation();
        violation.line = line;
        violation.error = msg;
        return violation;
    }

    public void setFix( boolean flag )
    {
        this.fix = flag;
    }

    public void setList( boolean flag )
    {
        this.list = flag;
    }

    /**
     * set the destination file which should be an xml file
     *
     * @param outFile The new Tofile value
     */
    public void setTofile( File outFile )
    {
        this.outFile = outFile;
    }

    public void setUnused( boolean flag )
    {
        this.unused = flag;
    }

    public Path createSearchpath()
    {
        if( searchPath == null )
        {
            searchPath = new Path( getProject() );
        }
        return searchPath;
    }

    protected ArrayList getOptions()
    {
        ArrayList options = new ArrayList( 512 );
        // there is a bug in Metamata 2.0 build 37. The sourcepath argument does
        // not work. So we will use the sourcepath prepended to classpath. (order
        // is important since Metamata looks at .class and .java)
        if( sourcePath != null )
        {
            sourcePath.append( classPath );// srcpath is prepended
            classPath = sourcePath;
            sourcePath = null;// prevent from using -sourcepath
        }

        // don't forget to modify the pattern if you change the options reporting
        if( classPath != null )
        {
            options.add( "-classpath" );
            options.add( classPath.toString() );
        }
        // suppress copyright msg when running, we will let it so that this
        // will be the only output to the console if in xml mode
        //      options.add("-quiet");
        if( fix )
        {
            options.add( "-fix" );
        }
        options.add( "-fullpath" );

        // generate .maudit files much more detailed than the report
        // I don't like it very much, I think it could be interesting
        // to get all .maudit files and include them in the XML.
        if( list )
        {
            options.add( "-list" );
        }
        if( sourcePath != null )
        {
            options.add( "-sourcepath" );
            options.add( sourcePath.toString() );
        }

        if( unused )
        {
            options.add( "-unused" );
            options.add( searchPath.toString() );
        }
        addAllArrayList( options, includedFiles.keySet().iterator() );
        return options;
    }

    protected void checkOptions()
        throws TaskException
    {
        super.checkOptions();
        if( unused && searchPath == null )
        {
            throw new TaskException( "'searchpath' element must be set when looking for 'unused' declarations." );
        }
        if( !unused && searchPath != null )
        {
            log( "'searchpath' element ignored. 'unused' attribute is disabled.", Project.MSG_WARN );
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

    protected void setupStreamHandler( final Execute exe )
        throws TaskException
    {
        // if we didn't specify a file, then use a screen report
        if( outFile == null )
        {
            exe.setOutput( new LogOutputStream( this, Project.MSG_INFO ) );
            exe.setError( new LogOutputStream( this, Project.MSG_WARN ) );
        }
        else
        {
            try
            {
                //XXX
                OutputStream out = new FileOutputStream( outFile );
                //handler = new MAuditStreamHandler( this, out );
                //FIXME: should behave like in Ant1.x
                exe.setOutput( out );
                exe.setError( out );
            }
            catch( IOException e )
            {
                throw new TaskException( "Error", e );
            }
        }
    }

    /**
     * the inner class used to report violation information
     *
     * @author RT
     */
    final static class Violation
    {
        String error;
        int line;
    }

}

