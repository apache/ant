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
import org.apache.myrmidon.framework.Pattern;
import org.apache.tools.ant.taskdefs.exec.Execute;
import org.apache.tools.ant.taskdefs.exec.ExecuteOn;
import org.apache.tools.ant.types.Argument;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;

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
    public void setDefaultexcludes( boolean useDefaultExcludes )
        throws TaskException
    {
        m_defaultSetDefined = true;
        m_defaultSet.setDefaultexcludes( useDefaultExcludes );
    }

    public void setDir( File src )
        throws TaskException
    {
        m_defaultSet.setDir( src );
    }

    /**
     * Sets the set of exclude patterns. Patterns may be separated by a comma or
     * a space.
     *
     * @param excludes the string containing the exclude patterns
     */
    public void setExcludes( String excludes )
        throws TaskException
    {
        m_defaultSetDefined = true;
        m_defaultSet.setExcludes( excludes );
    }

    public void setExecutable( String e )
        throws TaskException
    {
        throw new TaskException( getName() + " doesn\'t support the executable attribute" );
    }

    public void setFile( File src )
        throws TaskException
    {
        FileSet fs = new FileSet();
        fs.setDir( new File( src.getParent() ) );
        fs.createInclude().setName( src.getName() );
        addFileset( fs );
    }

    /**
     * Sets the set of include patterns. Patterns may be separated by a comma or
     * a space.
     *
     * @param includes the string containing the include patterns
     */
    public void setIncludes( String includes )
        throws TaskException
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
        final String message = getName() + " doesn\'t support the skipemptyfileset attribute";
        throw new IllegalArgumentException( message );
    }

    /**
     * add a name entry on the exclude list
     *
     * @return Description of the Returned Value
     */
    public Pattern createExclude()
        throws TaskException
    {
        m_defaultSetDefined = true;
        return m_defaultSet.createExclude();
    }

    /**
     * add a name entry on the include list
     *
     * @return Description of the Returned Value
     */
    public Pattern createInclude()
        throws TaskException
    {
        m_defaultSetDefined = true;
        return m_defaultSet.createInclude();
    }

    /**
     * add a set of patterns
     *
     * @return Description of the Returned Value
     */
    public PatternSet createPatternSet()
        throws TaskException
    {
        m_defaultSetDefined = true;
        return m_defaultSet.createPatternSet();
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
            addArg( new Argument( m_defaultSet.getDir().getPath() ) );
            Execute execute = prepareExec();
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
