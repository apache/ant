/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.filters.TokenSet;

/**
 * A set of tokens that are read from a file.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant.type type="token-set" name="tokens-file"
 */
public class FileTokenSet
    implements TokenSet
{
    private static final Resources REZ
        = ResourceManager.getPackageResources( FileTokenSet.class );

    private Map m_tokens = new HashMap();

    /**
     * set the file containing the tokens for this tokenset.
     */
    public void setFile( final File file )
        throws TaskException
    {
        // TODO - defer loading the tokens
        if( !file.isFile() )
        {
            final String message = REZ.getString( "filetokenset.not-a-file.error", file );
            throw new TaskException( message );
        }

        try
        {
            FileInputStream instr = new FileInputStream( file );

            try
            {
                Properties props = new Properties();
                props.load( instr );
                m_tokens.putAll( props );
            }
            finally
            {
                IOUtil.shutdownStream( instr );
            }
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "filetokenset.read-tokens.error", file );
            throw new TaskException( message, e );
        }
    }

    /**
     * Evaluates the value for a token.
     */
    public String getValue( String token, TaskContext context )
        throws TaskException
    {
        return (String)m_tokens.get( token );
    }
}
