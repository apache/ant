/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.cvslib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import org.apache.avalon.excalibur.util.StringUtil;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;

/**
 * CVSLogin Adds an new entry to a CVS password file
 *
 * @author <a href="jeff@custommonkey.org">Jeff Martin</a>
 * @version $Revision$ $Date$
 * @ant.task name="cvs-pass"
 */
public class CVSPass
    extends AbstractTask
{
    /**
     * CVS Root
     */
    private String m_cvsRoot;

    /**
     * Password file to add password to
     */
    private File m_passwordFile;

    /**
     * Password to add to file
     */
    private String m_password;

    /**
     * Array contain char conversion data
     */
    private final static char[] c_shifts =
        {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
            16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
            114, 120, 53, 79, 96, 109, 72, 108, 70, 64, 76, 67, 116, 74, 68, 87,
            111, 52, 75, 119, 49, 34, 82, 81, 95, 65, 112, 86, 118, 110, 122, 105,
            41, 57, 83, 43, 46, 102, 40, 89, 38, 103, 45, 50, 42, 123, 91, 35,
            125, 55, 54, 66, 124, 126, 59, 47, 92, 71, 115, 78, 88, 107, 106, 56,
            36, 121, 117, 104, 101, 100, 69, 73, 99, 63, 94, 93, 39, 37, 61, 48,
            58, 113, 32, 90, 44, 98, 60, 51, 33, 97, 62, 77, 84, 80, 85, 223,
            225, 216, 187, 166, 229, 189, 222, 188, 141, 249, 148, 200, 184, 136, 248, 190,
            199, 170, 181, 204, 138, 232, 218, 183, 255, 234, 220, 247, 213, 203, 226, 193,
            174, 172, 228, 252, 217, 201, 131, 230, 197, 211, 145, 238, 161, 179, 160, 212,
            207, 221, 254, 173, 202, 146, 224, 151, 140, 196, 205, 130, 135, 133, 143, 246,
            192, 159, 244, 239, 185, 168, 215, 144, 139, 165, 180, 157, 147, 186, 214, 176,
            227, 231, 219, 169, 175, 156, 206, 198, 129, 164, 150, 210, 154, 177, 134, 127,
            182, 128, 158, 208, 162, 132, 167, 209, 149, 241, 153, 251, 237, 236, 171, 195,
            243, 233, 253, 240, 194, 250, 191, 155, 142, 137, 245, 235, 163, 242, 178, 152
        };

    public CVSPass()
    {
        final String location = System.getProperty( "user.home" ) + "/.cvspass";
        m_passwordFile = new File( location );
    }

    /**
     * Sets cvs root to be added to the password file
     */
    public void setCvsroot( final String cvsRoot )
    {
        m_cvsRoot = cvsRoot;
    }

    /**
     * Sets the password file attribute.
     */
    public void setPassfile( final File passFile )
    {
        m_passwordFile = passFile;
    }

    /**
     * Sets the password attribute.
     */
    public void setPassword( final String password )
    {
        m_password = password;
    }

    /**
     * Does the work.
     *
     * @exception TaskException if someting goes wrong with the build
     */
    public final void execute()
        throws TaskException
    {
        if( null == m_cvsRoot )
        {
            throw new TaskException( "cvsroot is required" );
        }
        if( null == m_password )
        {
            throw new TaskException( "password is required" );
        }

        getContext().debug( "cvsRoot: " + m_cvsRoot );
        getContext().debug( "password: " + m_password );
        getContext().debug( "passFile: " + m_passwordFile );

        //FIXME: Should not be writing the whole file - Just append to the file
        //Also should have EOL configurable
        try
        {
            final StringBuffer sb = new StringBuffer();
            if( m_passwordFile.exists() )
            {
                final BufferedReader reader =
                    new BufferedReader( new FileReader( m_passwordFile ) );

                String line = null;

                while( ( line = reader.readLine() ) != null )
                {
                    if( !line.startsWith( m_cvsRoot ) )
                    {
                        sb.append( line + StringUtil.LINE_SEPARATOR );
                    }
                }

                reader.close();
            }

            final String pwdfile =
                sb.toString() + m_cvsRoot + " A" + mangle( m_password );

            getContext().debug( "Writing -> " + pwdfile );

            final PrintWriter writer =
                new PrintWriter( new FileWriter( m_passwordFile ) );

            writer.println( pwdfile );

            writer.close();
        }
        catch( IOException e )
        {
            throw new TaskException( "Error", e );
        }
    }

    /**
     * This encoding method and the accompanying LUT should
     * be pushed into another method ... somewhere.
     */
    private final String mangle( final String password )
    {
        final int size = password.length();

        final StringBuffer sb = new StringBuffer();
        for( int i = 0; i < size; i++ )
        {
            sb.append( c_shifts[ password.charAt( i ) ] );
        }
        return sb.toString();
    }
}
