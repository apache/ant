/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.IOException;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.Os;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.taskdefs.exec.Execute;

/**
 * Chmod equivalent for unix-like environments.
 *
 * @author costin@eng.sun.com
 * @author Mariusz Nowostawski (Marni) <a
 *      href="mailto:mnowostawski@infoscience.otago.ac.nz">
 *      mnowostawski@infoscience.otago.ac.nz</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */

public class Chmod extends ExecuteOn
{

    private FileSet defaultSet = new FileSet();
    private boolean defaultSetDefined = false;
    private boolean havePerm = false;

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
        defaultSetDefined = true;
        defaultSet.setDefaultexcludes( useDefaultExcludes );
    }

    public void setDir( File src )
        throws TaskException
    {
        defaultSet.setDir( src );
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
        defaultSetDefined = true;
        defaultSet.setExcludes( excludes );
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
        defaultSetDefined = true;
        defaultSet.setIncludes( includes );
    }

    public void setPerm( String perm )
    {
        createArg().setValue( perm );
        havePerm = true;
    }

    public void setSkipEmptyFilesets( boolean skip )
        throws TaskException
    {
        throw new TaskException( getName() + " doesn\'t support the skipemptyfileset attribute" );
    }

    /**
     * add a name entry on the exclude list
     *
     * @return Description of the Returned Value
     */
    public PatternSet.NameEntry createExclude()
        throws TaskException
    {
        defaultSetDefined = true;
        return defaultSet.createExclude();
    }

    /**
     * add a name entry on the include list
     *
     * @return Description of the Returned Value
     */
    public PatternSet.NameEntry createInclude()
        throws TaskException
    {
        defaultSetDefined = true;
        return defaultSet.createInclude();
    }

    /**
     * add a set of patterns
     *
     * @return Description of the Returned Value
     */
    public PatternSet createPatternSet()
        throws TaskException
    {
        defaultSetDefined = true;
        return defaultSet.createPatternSet();
    }

    public void execute()
        throws TaskException
    {
        if( defaultSetDefined || defaultSet.getDir( getProject() ) == null )
        {
            super.execute();
        }
        else if( isValidOs() )
        {
            // we are chmodding the given directory
            createArg().setValue( defaultSet.getDir( getProject() ).getPath() );
            Execute execute = prepareExec();
            try
            {
                execute.setCommandline( cmdl.getCommandline() );
                runExecute( execute );
            }
            catch( IOException e )
            {
                throw new TaskException( "Execute failed: " + e, e );
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

    protected void checkConfiguration()
        throws TaskException
    {
        if( !havePerm )
        {
            throw new TaskException( "Required attribute perm not set in chmod" );
        }

        if( defaultSetDefined && defaultSet.getDir( getProject() ) != null )
        {
            addFileset( defaultSet );
        }
        super.checkConfiguration();
    }
}
