/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.security;

import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.taskdefs.exec.Execute2;
import org.apache.tools.ant.types.Commandline;

import java.io.IOException;

/**
 * Generates a key.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class GenerateKey
    extends AbstractTask
{
    /**
     * The alias of signer.
     */
    private String m_alias;
    private String m_dname;
    private DistinguishedName m_expandedDname;
    private String m_keyalg;
    private String m_keypass;
    private int m_keysize;

    /**
     * The name of keystore file.
     */
    private String m_keystore;

    private String m_sigalg;
    private String m_storepass;
    private String m_storetype;
    private int m_validity;
    private boolean m_verbose;

    public void setAlias( final String alias )
    {
        m_alias = alias;
    }

    public void setDname( final String dname )
        throws TaskException
    {
        if( null != m_expandedDname )
        {
            throw new TaskException( "It is not possible to specify dname both " +
                                     "as attribute and element." );
        }
        m_dname = dname;
    }

    public void setKeyalg( final String keyalg )
    {
        m_keyalg = keyalg;
    }

    public void setKeypass( final String keypass )
    {
        m_keypass = keypass;
    }

    public void setKeysize( final String keysize )
        throws TaskException
    {
        try
        {
            m_keysize = Integer.parseInt( keysize );
        }
        catch( final NumberFormatException nfe )
        {
            throw new TaskException( "KeySize attribute should be a integer" );
        }
    }

    public void setKeystore( final String keystore )
    {
        m_keystore = keystore;
    }

    public void setSigalg( final String sigalg )
    {
        m_sigalg = sigalg;
    }

    public void setStorepass( final String storepass )
    {
        m_storepass = storepass;
    }

    public void setStoretype( final String storetype )
    {
        m_storetype = storetype;
    }

    public void setValidity( final String validity )
        throws TaskException
    {
        try
        {
            m_validity = Integer.parseInt( validity );
        }
        catch( final NumberFormatException nfe )
        {
            throw new TaskException( "Validity attribute should be a integer" );
        }
    }

    public void setVerbose( final boolean verbose )
    {
        m_verbose = verbose;
    }

    public DistinguishedName createDname()
        throws TaskException
    {
        if( null != m_expandedDname )
        {
            throw new TaskException( "DName sub-element can only be specified once." );
        }
        if( null != m_dname )
        {
            throw new TaskException( "It is not possible to specify dname both " +
                                     "as attribute and element." );
        }
        m_expandedDname = new DistinguishedName();
        return m_expandedDname;
    }

    public void execute()
        throws TaskException
    {
        validate();

        final String message = "Generating Key for " + m_alias;
        getLogger().info( message );

        final Commandline cmd = createCommand();
        final Execute2 exe = new Execute2();
        exe.setWorkingDirectory( getBaseDirectory() );
        exe.setCommandline( cmd.getCommandline() );
        try
        {
            exe.execute();
        }
        catch( final IOException ioe )
        {
            throw new TaskException( ioe.getMessage(), ioe );
        }
    }

    private Commandline createCommand()
    {
        final Commandline cmd = new Commandline();
        cmd.setExecutable( "keytool" );

        cmd.addArgument( "-genkey " );

        if( m_verbose )
        {
            cmd.addArgument( "-v " );
        }

        cmd.addArgument( "-alias" );
        cmd.addArgument( m_alias );

        if( null != m_dname )
        {
            cmd.addArgument( "-dname" );
            cmd.addArgument( m_dname );
        }

        if( null != m_expandedDname )
        {
            cmd.addArgument( "-dname" );
            cmd.addArgument( m_expandedDname.toString() );
        }

        if( null != m_keystore )
        {
            cmd.addArgument( "-keystore" );
            cmd.addArgument( m_keystore );
        }

        if( null != m_storepass )
        {
            cmd.addArgument( "-storepass" );
            cmd.addArgument( m_storepass );
        }

        if( null != m_storetype )
        {
            cmd.addArgument( "-storetype" );
            cmd.addArgument( m_storetype );
        }

        cmd.addArgument( "-keypass" );
        if( null != m_keypass )
        {
            cmd.addArgument( m_keypass );
        }
        else
        {
            cmd.addArgument( m_storepass );
        }

        if( null != m_sigalg )
        {
            cmd.addArgument( "-sigalg" );
            cmd.addArgument( m_sigalg );
        }

        if( null != m_keyalg )
        {
            cmd.addArgument( "-keyalg" );
            cmd.addArgument( m_keyalg );
        }

        if( 0 < m_keysize )
        {
            cmd.addArgument( "-keysize" );
            cmd.addArgument( "" + m_keysize );
        }

        if( 0 < m_validity )
        {
            cmd.addArgument( "-validity" );
            cmd.addArgument( "" + m_validity );
        }
        return cmd;
    }

    private void validate()
        throws TaskException
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

        if( null == m_dname && null == m_expandedDname )
        {
            final String message = "dname must be set";
            throw new TaskException( message );
        }
    }
}

