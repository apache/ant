/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.AbstractMatchingTask;
import org.apache.tools.ant.types.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ScannerUtil;

/**
 * This task can be used to create checksums for files. It can also be used to
 * verify checksums.
 *
 * @ant:task name="checksum"
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 * @version $Revision$ $Date$
 */
public class Checksum
    extends AbstractMatchingTask
{
    /**
     * File for which checksum is to be calculated.
     */
    private File m_file;

    /**
     * MessageDigest algorithm to be used.
     */
    private String m_algorithm = "MD5";

    /**
     * MessageDigest Algorithm provider
     */
    private String m_provider;

    /**
     * ArrayList to hold source file sets.
     */
    private ArrayList m_filesets = new ArrayList();

    /**
     * Stores SourceFile, DestFile pairs and SourceFile, Property String pairs.
     */
    private Hashtable m_includeFileMap = new Hashtable();

    /**
     * File Extension that is be to used to create or identify destination file
     */
    private String m_fileext;

    /**
     * Create new destination file? Defaults to false.
     */
    private boolean m_forceOverwrite;

    /**
     * Message Digest instance
     */
    private MessageDigest m_messageDigest;

    /**
     * Holds generated checksum and gets set as a Project Property.
     */
    private String m_property;

    /**
     * Contains the result of a checksum verification. ("true" or "false")
     */
    private String m_verifyProperty;

    /**
     * Sets the MessageDigest algorithm to be used to calculate the checksum.
     */
    public void setAlgorithm( final String algorithm )
    {
        m_algorithm = algorithm;
    }

    /**
     * Sets the file for which the checksum is to be calculated.
     */
    public void setFile( final File file )
    {
        m_file = file;
    }

    /**
     * Sets the File Extension that is be to used to create or identify
     * destination file
     */
    public void setFileext( final String fileext )
    {
        m_fileext = fileext;
    }

    /**
     * Overwrite existing file irrespective of whether it is newer than the
     * source file? Defaults to false.
     */
    public void setForceOverwrite( boolean forceOverwrite )
    {
        this.m_forceOverwrite = forceOverwrite;
    }

    /**
     * Sets the property to hold the generated checksum
     */
    public void setProperty( String property )
    {
        this.m_property = property;
    }

    /**
     * Sets the MessageDigest algorithm provider to be used to calculate the
     * checksum.
     *
     * @param provider The new Provider value
     */
    public void setProvider( final String provider )
    {
        m_provider = provider;
    }

    /**
     * Sets verify property. This project property holds the result of a
     * checksum verification - "true" or "false"
     */
    public void setVerifyproperty( final String verifyProperty )
    {
        m_verifyProperty = verifyProperty;
    }

    /**
     * Adds a set of files (nested fileset attribute).
     */
    public void addFileset( final FileSet set )
    {
        m_filesets.add( set );
    }

    /**
     * Calculate the checksum(s).
     */
    public void execute()
        throws TaskException
    {
        final boolean value = validateAndExecute();
        if( m_verifyProperty != null )
        {
            getContext().setProperty( m_verifyProperty, "" + value );
        }
    }

    /**
     * Add key-value pair to the hashtable upon which to later operate upon.
     *
     * @param file The feature to be added to the ToIncludeFileMap attribute
     * @exception TaskException Description of Exception
     */
    private void addToIncludeFileMap( final File file )
        throws TaskException
    {
        if( file != null )
        {
            if( file.exists() )
            {
                if( m_property == null )
                {
                    final File dest = new File( file.getParent(), file.getName() + m_fileext );
                    if( m_forceOverwrite ||
                        ( file.lastModified() > dest.lastModified() ) )
                    {
                        m_includeFileMap.put( file, dest );
                    }
                    else
                    {
                        final String message = file + " omitted as " + dest +
                            " is up to date.";
                        getLogger().debug( message );
                    }
                }
                else
                {
                    m_includeFileMap.put( file, m_property );
                }
            }
            else
            {
                final String message = "Could not find file " + file.getAbsolutePath() +
                    " to generate checksum for.";
                getLogger().info( message );
                throw new TaskException( message );
            }
        }
    }

    /**
     * Generate checksum(s) using the message digest created earlier.
     */
    private boolean generateChecksums()
        throws TaskException
    {
        boolean checksumMatches = true;
        final Enumeration includes = m_includeFileMap.keys();
        while( includes.hasMoreElements() )
        {
            final File src = (File)includes.nextElement();
            final String message = "Calculating " + m_algorithm + " checksum for " + src;
            getLogger().info( message );

            checksumMatches = z( src, checksumMatches );
        }

        return checksumMatches;
    }

    private boolean z( final File src, final boolean checksumMatches )
        throws TaskException
    {
        boolean match = checksumMatches;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try
        {
            fis = new FileInputStream( src );
            final byte[] fileDigest = buildDigest( fis );
            IOUtil.shutdownStream( fis );

            final StringBuffer sb = new StringBuffer();
            for( int i = 0; i < fileDigest.length; i++ )
            {
                final String hexStr = Integer.toHexString( 0x00ff & fileDigest[ i ] );
                if( hexStr.length() < 2 )
                {
                    sb.append( '0' );
                }
                sb.append( hexStr );
            }

            final String checksum = sb.toString();

            //can either be a property name string or a file
            final Object destination = m_includeFileMap.get( src );
            if( destination instanceof String )
            {
                final String prop = (String)destination;
                match = checksum.equals( m_property );
                getContext().setProperty( prop, checksum );
            }
            else if( destination instanceof File )
            {
                final File file = (File)destination;
                fos = new FileOutputStream( file );
                fos.write( checksum.getBytes() );
                fos.close();
                fos = null;
            }
        }
        catch( final Exception e )
        {
            throw new TaskException( e.getMessage(), e );
        }
        finally
        {
            IOUtil.shutdownStream( fis );
            IOUtil.shutdownStream( fos );
        }
        return match;
    }

    private byte[] buildDigest( final InputStream input )
        throws IOException
    {
        m_messageDigest.reset();

        final DigestInputStream digester =
            new DigestInputStream( input, m_messageDigest );

        while( digester.read() != -1 )
        {
        }

        digester.close();
        return m_messageDigest.digest();
    }

    /**
     * Validate attributes and get down to business.
     */
    private boolean validateAndExecute()
        throws TaskException
    {
        if( null == m_file && 0 == m_filesets.size() )
        {
            final String message = "Specify at least one source - a file or a fileset.";
            throw new TaskException( message );
        }

        if( null != m_file && m_file.exists() && m_file.isDirectory() )
        {
            final String message = "Checksum cannot be generated for directories";
            throw new TaskException( message );
        }

        if( null != m_property && null != m_fileext )
        {
            final String message = "Property and FileExt cannot co-exist.";
            throw new TaskException( message );
        }

        if( m_property != null )
        {
            if( m_forceOverwrite )
            {
                final String message =
                    "ForceOverwrite cannot be used when Property is specified";
                throw new TaskException( message );
            }

            if( m_file != null )
            {
                if( m_filesets.size() > 0 )
                {
                    final String message =
                        "Multiple files cannot be used when Property is specified";
                    throw new TaskException( message );
                }
            }
            else
            {
                if( m_filesets.size() > 1 )
                {
                    final String message =
                        "Multiple files cannot be used when Property is specified";
                    throw new TaskException( message );
                }
            }
        }

        if( m_verifyProperty != null && m_forceOverwrite )
        {
            final String message = "VerifyProperty and ForceOverwrite cannot co-exist.";
            throw new TaskException( message );
        }

        if( m_fileext == null )
        {
            m_fileext = "." + m_algorithm;
        }
        else if( m_fileext.trim().length() == 0 )
        {
            final String message = "File extension when specified must not be an empty string";
            throw new TaskException( message );
        }

        setupMessageDigest();

        if( m_messageDigest == null )
        {
            final String message = "Unable to create Message Digest";
            throw new TaskException( message );
        }

        addIncludes();

        return generateChecksums();
    }

    private void setupMessageDigest()
        throws TaskException
    {
        m_messageDigest = null;
        if( m_provider != null )
        {
            try
            {
                m_messageDigest = MessageDigest.getInstance( m_algorithm, m_provider );
            }
            catch( final NoSuchAlgorithmException nsae )
            {
                throw new TaskException( nsae.toString(), nsae );
            }
            catch( final NoSuchProviderException nspe )
            {
                throw new TaskException( nspe.toString(), nspe );
            }
        }
        else
        {
            try
            {
                m_messageDigest = MessageDigest.getInstance( m_algorithm );
            }
            catch( final NoSuchAlgorithmException nsae )
            {
                throw new TaskException( nsae.toString(), nsae );
            }
        }
    }

    private void addIncludes()
        throws TaskException
    {
        addToIncludeFileMap( m_file );

        final int size = m_filesets.size();
        for( int i = 0; i < size; i++ )
        {
            final FileSet fileSet = (FileSet)m_filesets.get( i );
            final DirectoryScanner scanner = ScannerUtil.getDirectoryScanner( fileSet );
            final String[] srcFiles = scanner.getIncludedFiles();
            for( int j = 0; j < srcFiles.length; j++ )
            {
                final File src = new File( fileSet.getDir(), srcFiles[ j ] );
                addToIncludeFileMap( src );
            }
        }
    }
}
