/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.unix;

import java.io.File;
import java.io.IOException;
import org.apache.aut.nativelib.Os;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.framework.Pattern;
import org.apache.myrmidon.framework.PatternSet;
import org.apache.tools.ant.taskdefs.exec.Execute;
import org.apache.tools.ant.taskdefs.exec.ExecuteOn;
import org.apache.tools.ant.types.Argument;
import org.apache.tools.ant.types.FileSet;

/**
 * Chmod equivalent for unix-like environments.
 *
 * @author costin@eng.sun.com
 * @author Mariusz Nowostawski (Marni) <a
 *      href="mailto:mnowostawski@infoscience.otago.ac.nz">
 *      mnowostawski@infoscience.otago.ac.nz</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class Chmod
    extends ExecuteOn
{
    private FileSet m_defaultSet = new FileSet();
    private boolean m_defaultSetDefined;
    private boolean m_havePerm;

    public Chmod()
        throws TaskException
    {
        super.setExecutable( "chmod" );
        super.setParallel( true );
        super.setSkipEmptyFilesets( true );
    }

    /**
     * Sets whether default exclusions should be used or not.
     *
     * @param useDefaultExcludes "true"|"on"|"yes" when default exclusions
     *      should be used, "false"|"off"|"no" when they shouldn't be used.
     */
    public void setDefaultExcludes( boolean useDefaultExcludes )
    {
        m_defaultSetDefined = true;
        m_defaultSet.setDefaultExcludes( useDefaultExcludes );
    }

    public void setDir( File src )
    {
        m_defaultSet.setDir( src );
    }

    /**
     * Sets the set of exclude patterns. Patterns may be separated by a comma or
     * a space.
     */
    public void setExcludes( String excludes )
    {
        m_defaultSetDefined = true;
        m_defaultSet.setExcludes( excludes );
    }

    public void setExecutable( String e )
        throws TaskException
    {
        throw new TaskException( getContext().getName() + " doesn\'t support the executable attribute" );
    }

    public void setFile( File src )
    {
        final FileSet fileSet = new FileSet();
        fileSet.setDir( new File( src.getParent() ) );
        fileSet.addInclude( new Pattern( src.getName() ) );
        addFileset( fileSet );
    }

    /**
     * Sets the set of include patterns. Patterns may be separated by a comma or
     * a space.
     *
     * @param includes the string containing the include patterns
     */
    public void setIncludes( String includes )
    {
        m_defaultSetDefined = true;
        m_defaultSet.setIncludes( includes );
    }

    public void setPerm( String perm )
    {
        addArg( new Argument( perm ) );
        m_havePerm = true;
    }

    public void setSkipEmptyFilesets( final boolean skip )
    {
        final String message = getContext().getName() + " doesn\'t support the skipemptyfileset attribute";
        throw new IllegalArgumentException( message );
    }

    /**
     * add a name entry on the exclude list
     */
    public void addExclude( final Pattern pattern )
    {
        m_defaultSetDefined = true;
        m_defaultSet.addExclude( pattern );
    }

    /**
     * add a name entry on the include list
     */
    public void addInclude( final Pattern pattern )
    {
        m_defaultSetDefined = true;
        m_defaultSet.addInclude( pattern );
    }

    /**
     * add a set of patterns
     */
    public void addPatternSet( final PatternSet set )
    {
        m_defaultSetDefined = true;
        m_defaultSet.addPatternSet( set );
    }

    public void execute()
        throws TaskException
    {
        if( m_defaultSetDefined || m_defaultSet.getDir() == null )
        {
            super.execute();
        }
        else if( isValidOs() )
        {
            // we are chmodding the given directory
            final Argument argument =
                new Argument( m_defaultSet.getDir().getPath() );
            addArg( argument );
            final Execute execute = prepareExec();
            try
            {
                execute.setCommandline( getCommand().getCommandline() );
                runExecute( execute );
            }
            catch( final IOException ioe )
            {
                final String message = "Execute failed: " + ioe;
                throw new TaskException( message, ioe );
            }
            finally
            {
                // close the output file if required
                logFlush();
            }
        }
    }

    protected boolean isValidOs()
    {
        return Os.isFamily( "unix" ) && super.isValidOs();
    }

    protected void validate()
        throws TaskException
    {
        if( !m_havePerm )
        {
            throw new TaskException( "Required attribute perm not set in chmod" );
        }

        if( m_defaultSetDefined && m_defaultSet.getDir() != null )
        {
            addFileset( m_defaultSet );
        }
        super.validate();
    }
}
