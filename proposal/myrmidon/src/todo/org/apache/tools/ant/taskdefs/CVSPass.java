/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.myrmidon.api.TaskException;

/**
 * CVSLogin Adds an new entry to a CVS password file
 *
 * @author <a href="jeff@custommonkey.org">Jeff Martin</a>
 * @version $Revision$
 */
public class CVSPass extends Task
{
    /**
     * CVS Root
     */
    private String cvsRoot = null;
    /**
     * Password file to add password to
     */
    private File passFile = null;
    /**
     * Password to add to file
     */
    private String password = null;
    /**
     * End of line character
     */
    private final String EOL = System.getProperty( "line.separator" );

    /**
     * Array contain char conversion data
     */
    private final char shifts[] = {
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
        243, 233, 253, 240, 194, 250, 191, 155, 142, 137, 245, 235, 163, 242, 178, 152};

    public CVSPass()
    {
        passFile = new File( System.getProperty( "user.home" ) + "/.cvspass" );
    }

    /**
     * Sets cvs root to be added to the password file
     *
     * @param cvsRoot The new Cvsroot value
     */
    public void setCvsroot( String cvsRoot )
    {
        this.cvsRoot = cvsRoot;
    }

    /**
     * Sets the password file attribute.
     *
     * @param passFile The new Passfile value
     */
    public void setPassfile( File passFile )
    {
        this.passFile = passFile;
    }

    /**
     * Sets the password attribute.
     *
     * @param password The new Password value
     */
    public void setPassword( String password )
    {
        this.password = password;
    }

    /**
     * Does the work.
     *
     * @exception BuildException if someting goes wrong with the build
     */
    public final void execute()
        throws BuildException
    {
        if( cvsRoot == null )
            throw new TaskException( "cvsroot is required" );
        if( password == null )
            throw new TaskException( "password is required" );

        log( "cvsRoot: " + cvsRoot, project.MSG_DEBUG );
        log( "password: " + password, project.MSG_DEBUG );
        log( "passFile: " + passFile, project.MSG_DEBUG );

        try
        {
            StringBuffer buf = new StringBuffer();

            if( passFile.exists() )
            {
                BufferedReader reader =
                    new BufferedReader( new FileReader( passFile ) );

                String line = null;

                while( ( line = reader.readLine() ) != null )
                {
                    if( !line.startsWith( cvsRoot ) )
                    {
                        buf.append( line + EOL );
                    }
                }

                reader.close();
            }

            String pwdfile = buf.toString() + cvsRoot + " A" + mangle( password );

            log( "Writing -> " + pwdfile, project.MSG_DEBUG );

            PrintWriter writer = new PrintWriter( new FileWriter( passFile ) );

            writer.println( pwdfile );

            writer.close();
        }
        catch( IOException e )
        {
            throw new BuildException( "Error", e );
        }

    }

    private final String mangle( String password )
    {
        StringBuffer buf = new StringBuffer();
        for( int i = 0; i < password.length(); i++ )
        {
            buf.append( shifts[password.charAt( i )] );
        }
        return buf.toString();
    }

}
