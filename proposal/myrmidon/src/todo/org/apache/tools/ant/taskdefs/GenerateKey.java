/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;

import java.util.Enumeration;
import java.util.Vector;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Commandline;

/**
 * Generates a key.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class GenerateKey
    extends Task
{
    /**
     * The alias of signer.
     */
    protected String alias;
    protected String dname;
    protected DistinguishedName expandedDname;
    protected String keyalg;
    protected String keypass;
    protected int keysize;

    /**
     * The name of keystore file.
     */
    protected String keystore;

    protected String sigalg;
    protected String storepass;
    protected String storetype;
    protected int validity;
    protected boolean verbose;

    public void setAlias( final String alias )
    {
        this.alias = alias;
    }

    public void setDname( final String dname )
        throws TaskException
    {
        if( null != expandedDname )
        {
            throw new TaskException( "It is not possible to specify dname both " +
                                     "as attribute and element." );
        }
        this.dname = dname;
    }

    public void setKeyalg( final String keyalg )
    {
        this.keyalg = keyalg;
    }

    public void setKeypass( final String keypass )
    {
        this.keypass = keypass;
    }

    public void setKeysize( final String keysize )
        throws TaskException
    {
        try
        {
            this.keysize = Integer.parseInt( keysize );
        }
        catch( final NumberFormatException nfe )
        {
            throw new TaskException( "KeySize attribute should be a integer" );
        }
    }

    public void setKeystore( final String keystore )
    {
        this.keystore = keystore;
    }

    public void setSigalg( final String sigalg )
    {
        this.sigalg = sigalg;
    }

    public void setStorepass( final String storepass )
    {
        this.storepass = storepass;
    }

    public void setStoretype( final String storetype )
    {
        this.storetype = storetype;
    }

    public void setValidity( final String validity )
        throws TaskException
    {
        try
        {
            this.validity = Integer.parseInt( validity );
        }
        catch( final NumberFormatException nfe )
        {
            throw new TaskException( "Validity attribute should be a integer" );
        }
    }

    public void setVerbose( final boolean verbose )
    {
        this.verbose = verbose;
    }

    public DistinguishedName createDname()
        throws TaskException
    {
        if( null != expandedDname )
        {
            throw new TaskException( "DName sub-element can only be specified once." );
        }
        if( null != dname )
        {
            throw new TaskException( "It is not possible to specify dname both " +
                                     "as attribute and element." );
        }
        expandedDname = new DistinguishedName();
        return expandedDname;
    }

    public void execute()
        throws TaskException
    {
        if( project.getJavaVersion().equals( Project.JAVA_1_1 ) )
        {
            throw new TaskException( "The genkey task is only available on JDK" +
                                     " versions 1.2 or greater" );
        }

        if( null == alias )
        {
            throw new TaskException( "alias attribute must be set" );
        }

        if( null == storepass )
        {
            throw new TaskException( "storepass attribute must be set" );
        }

        if( null == dname && null == expandedDname )
        {
            throw new TaskException( "dname must be set" );
        }

        getLogger().info( "Generating Key for " + alias );
        final ExecTask cmd = (ExecTask)project.createTask( "exec" );
        cmd.setExecutable( "keytool" );

        cmd.createArg().setValue( "-genkey " );

        if( verbose )
        {
            cmd.createArg().setValue( "-v " );
        }

        cmd.createArg().setValue( "-alias" );
        cmd.createArg().setValue( alias );

        if( null != dname )
        {
            cmd.createArg().setValue( "-dname" );
            cmd.createArg().setValue( dname );
        }

        if( null != expandedDname )
        {
            cmd.createArg().setValue( "-dname" );
            cmd.createArg().setValue( expandedDname.toString() );
        }

        if( null != keystore )
        {
            cmd.createArg().setValue( "-keystore" );
            cmd.createArg().setValue( keystore );
        }

        if( null != storepass )
        {
            cmd.createArg().setValue( "-storepass" );
            cmd.createArg().setValue( storepass );
        }

        if( null != storetype )
        {
            cmd.createArg().setValue( "-storetype" );
            cmd.createArg().setValue( storetype );
        }

        cmd.createArg().setValue( "-keypass" );
        if( null != keypass )
        {
            cmd.createArg().setValue( keypass );
        }
        else
        {
            cmd.createArg().setValue( storepass );
        }

        if( null != sigalg )
        {
            cmd.createArg().setValue( "-sigalg" );
            cmd.createArg().setValue( sigalg );
        }

        if( null != keyalg )
        {
            cmd.createArg().setValue( "-keyalg" );
            cmd.createArg().setValue( keyalg );
        }

        if( 0 < keysize )
        {
            cmd.createArg().setValue( "-keysize" );
            cmd.createArg().setValue( "" + keysize );
        }

        if( 0 < validity )
        {
            cmd.createArg().setValue( "-validity" );
            cmd.createArg().setValue( "" + validity );
        }

        cmd.setFailonerror( true );
        cmd.execute();
    }

    public static class DistinguishedName
    {

        private Vector params = new Vector();
        private String name;
        private String path;

        public Enumeration getParams()
        {
            return params.elements();
        }

        public Object createParam()
        {
            DnameParam param = new DnameParam();
            params.addElement( param );

            return param;
        }

        public String encode( final String string )
        {
            int end = string.indexOf( ',' );

            if( -1 == end )
                return string;

            final StringBuffer sb = new StringBuffer();

            int start = 0;

            while( -1 != end )
            {
                sb.append( string.substring( start, end ) );
                sb.append( "\\," );
                start = end + 1;
                end = string.indexOf( ',', start );
            }

            sb.append( string.substring( start ) );

            return sb.toString();
        }

        public String toString()
        {
            final int size = params.size();
            final StringBuffer sb = new StringBuffer();
            boolean firstPass = true;

            for( int i = 0; i < size; i++ )
            {
                if( !firstPass )
                {
                    sb.append( " ," );
                }
                firstPass = false;

                final DnameParam param = (DnameParam)params.elementAt( i );
                sb.append( encode( param.getName() ) );
                sb.append( '=' );
                sb.append( encode( param.getValue() ) );
            }

            return sb.toString();
        }
    }

    public static class DnameParam
    {
        private String name;
        private String value;

        public void setName( String name )
        {
            this.name = name;
        }

        public void setValue( String value )
        {
            this.value = value;
        }

        public String getName()
        {
            return name;
        }

        public String getValue()
        {
            return value;
        }
    }
}

