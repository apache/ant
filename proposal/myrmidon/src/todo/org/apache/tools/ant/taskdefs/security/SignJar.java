/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.security;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.exec.ExecTask;
import org.apache.tools.ant.types.FileSet;

/**
 * Sign a archive.
 *
 * @author Peter Donald <a href="mailto:donaldp@apache.org">donaldp@apache.org</a>
 * @author Nick Fortescue <a href="mailto:nick@ox.compsoc.net">nick@ox.compsoc.net</a>
 */
public class SignJar
    extends AbstractTask
{
    /**
     * the filesets of the jars to sign
     */
    private ArrayList m_filesets = new ArrayList();

    /**
     * The alias of signer.
     */
    private String m_alias;
    private boolean m_internalsf;

    /**
     * The name of the jar file.
     */
    private File m_jar;
    private String m_keypass;

    /**
     * The name of keystore file.
     */
    private File m_keystore;

    /**
     * Whether to assume a jar which has an appropriate .SF file in is already
     * signed.
     */
    private boolean m_lazy;

    private boolean m_sectionsonly;
    private File m_sigfile;
    private File m_signedjar;

    private String m_storepass;
    private String m_storetype;
    private boolean m_verbose;

    public void setAlias( final String alias )
    {
        m_alias = alias;
    }

    public void setInternalsf( final boolean internalsf )
    {
        m_internalsf = internalsf;
    }

    public void setJar( final File jar )
    {
        m_jar = jar;
    }

    public void setKeypass( final String keypass )
    {
        m_keypass = keypass;
    }

    public void setKeystore( final File keystore )
    {
        m_keystore = keystore;
    }

    public void setLazy( final boolean lazy )
    {
        m_lazy = lazy;
    }

    public void setSectionsonly( final boolean sectionsonly )
    {
        m_sectionsonly = sectionsonly;
    }

    public void setSigfile( final File sigfile )
    {
        m_sigfile = sigfile;
    }

    public void setSignedjar( final File signedjar )
    {
        m_signedjar = signedjar;
    }

    public void setStorepass( final String storepass )
    {
        m_storepass = storepass;
    }

    public void setStoretype( final String storetype )
    {
        m_storetype = storetype;
    }

    public void setVerbose( final boolean verbose )
    {
        m_verbose = verbose;
    }

    /**
     * Adds a set of files (nested fileset attribute).
     *
     * @param set The feature to be added to the Fileset attribute
     */
    public void addFileset( final FileSet set )
    {
        m_filesets.add( set );
    }

    public void execute()
        throws TaskException
    {
        validate();

        if( null != m_jar )
        {
            doOneJar( m_jar, m_signedjar );
        }
        else
        {
            //Assume null != filesets

            // deal with the filesets
            for( int i = 0; i < m_filesets.size(); i++ )
            {
                final FileSet fileSet = (FileSet)m_filesets.get( i );
                final DirectoryScanner scanner = fileSet.getDirectoryScanner();
                final String[] jarFiles = scanner.getIncludedFiles();
                for( int j = 0; j < jarFiles.length; j++ )
                {
                    final File file =
                        new File( fileSet.getDir(), jarFiles[ j ] );
                    doOneJar( file, null );
                }
            }
        }
    }

    private void validate() throws TaskException
    {
        if( null == m_jar && null == m_filesets )
        {
            final String message = "jar must be set through jar attribute or nested filesets";
            throw new TaskException( message );
        }
        else if( null != m_jar )
        {
            if( null == m_alias )
            {
                final String message = "alias attribute must be set";
                throw new TaskException( message );
            }

            if( null == m_storepass )
            {
                final String message = "storepass attribute must be set";
                throw new TaskException( message );
            }
        }
    }

    private boolean isSigned( final File file )
    {
        final String SIG_START = "META-INF/";
        final String SIG_END = ".SF";

        if( !file.exists() )
        {
            return false;
        }
        ZipFile jarFile = null;
        try
        {
            jarFile = new ZipFile( file );
            if( null == m_alias )
            {
                final Enumeration entries = jarFile.entries();
                while( entries.hasMoreElements() )
                {
                    final String name = ( (ZipEntry)entries.nextElement() ).getName();
                    if( name.startsWith( SIG_START ) && name.endsWith( SIG_END ) )
                    {
                        return true;
                    }
                }
                return false;
            }
            else
            {
                final String name = SIG_START + m_alias.toUpperCase() + SIG_END;
                final ZipEntry entry = jarFile.getEntry( name );
                return ( entry != null );
            }
        }
        catch( final IOException ioe )
        {
            return false;
        }
        finally
        {
            if( null != jarFile )
            {
                try
                {
                    jarFile.close();
                }
                catch( final IOException ioe )
                {
                }
            }
        }
    }

    private boolean isUpToDate( final File jarFile, final File signedjarFile )
    {
        if( null == jarFile )
        {
            return false;
        }
        else if( null != signedjarFile )
        {
            if( !jarFile.exists() )
            {
                return false;
            }
            else if( !signedjarFile.exists() )
            {
                return false;
            }
            else if( jarFile.equals( signedjarFile ) )
            {
                return false;
            }
            else if( signedjarFile.lastModified() > jarFile.lastModified() )
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        else if( m_lazy )
        {
            return isSigned( jarFile );
        }
        else
        {
            return false;
        }
    }

    private void doOneJar( final File jarSource, final File jarTarget )
        throws TaskException
    {
        if( isUpToDate( jarSource, jarTarget ) )
            return;

        final StringBuffer sb = new StringBuffer();

        final ExecTask cmd = null;//(ExecTask)getProject().createTask( "exec" );
        cmd.setExecutable( "jarsigner" );

        if( null != m_keystore )
        {
            cmd.createArg().setValue( "-keystore" );
            cmd.createArg().setValue( m_keystore.toString() );
        }

        if( null != m_storepass )
        {
            cmd.createArg().setValue( "-storepass" );
            cmd.createArg().setValue( m_storepass );
        }

        if( null != m_storetype )
        {
            cmd.createArg().setValue( "-storetype" );
            cmd.createArg().setValue( m_storetype );
        }

        if( null != m_keypass )
        {
            cmd.createArg().setValue( "-keypass" );
            cmd.createArg().setValue( m_keypass );
        }

        if( null != m_sigfile )
        {
            cmd.createArg().setValue( "-sigfile" );
            cmd.createArg().setValue( m_sigfile.toString() );
        }

        if( null != jarTarget )
        {
            cmd.createArg().setValue( "-signedjar" );
            cmd.createArg().setValue( jarTarget.toString() );
        }

        if( m_verbose )
        {
            cmd.createArg().setValue( "-verbose" );
        }

        if( m_internalsf )
        {
            cmd.createArg().setValue( "-internalsf" );
        }

        if( m_sectionsonly )
        {
            cmd.createArg().setValue( "-sectionsonly" );
        }

        cmd.createArg().setValue( jarSource.toString() );

        cmd.createArg().setValue( m_alias );

        final String message = "Signing Jar : " + jarSource.getAbsolutePath();
        getLogger().info( message );
        cmd.execute();
    }
}

