/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.DirectoryScanner;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.types.FileSet;

/**
 * This task can be used to create checksums for files. It can also be used to
 * verify checksums.
 *
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 */
public class Checksum extends MatchingTask implements Condition
{
    /**
     * File for which checksum is to be calculated.
     */
    private File file = null;
    /**
     * MessageDigest algorithm to be used.
     */
    private String algorithm = "MD5";
    /**
     * MessageDigest Algorithm provider
     */
    private String provider = null;
    /**
     * ArrayList to hold source file sets.
     */
    private ArrayList filesets = new ArrayList();
    /**
     * Stores SourceFile, DestFile pairs and SourceFile, Property String pairs.
     */
    private Hashtable includeFileMap = new Hashtable();
    /**
     * File Extension that is be to used to create or identify destination file
     */
    private String fileext;
    /**
     * Create new destination file? Defaults to false.
     */
    private boolean forceOverwrite;
    /**
     * is this task being used as a nested condition element?
     */
    private boolean isCondition;
    /**
     * Message Digest instance
     */
    private MessageDigest messageDigest;
    /**
     * Holds generated checksum and gets set as a Project Property.
     */
    private String property;
    /**
     * Contains the result of a checksum verification. ("true" or "false")
     */
    private String verifyProperty;

    /**
     * Sets the MessageDigest algorithm to be used to calculate the checksum.
     *
     * @param algorithm The new Algorithm value
     */
    public void setAlgorithm( String algorithm )
    {
        this.algorithm = algorithm;
    }

    /**
     * Sets the file for which the checksum is to be calculated.
     *
     * @param file The new File value
     */
    public void setFile( File file )
    {
        this.file = file;
    }

    /**
     * Sets the File Extension that is be to used to create or identify
     * destination file
     *
     * @param fileext The new Fileext value
     */
    public void setFileext( String fileext )
    {
        this.fileext = fileext;
    }

    /**
     * Overwrite existing file irrespective of whether it is newer than the
     * source file? Defaults to false.
     *
     * @param forceOverwrite The new ForceOverwrite value
     */
    public void setForceOverwrite( boolean forceOverwrite )
    {
        this.forceOverwrite = forceOverwrite;
    }

    /**
     * Sets the property to hold the generated checksum
     *
     * @param property The new Property value
     */
    public void setProperty( String property )
    {
        this.property = property;
    }

    /**
     * Sets the MessageDigest algorithm provider to be used to calculate the
     * checksum.
     *
     * @param provider The new Provider value
     */
    public void setProvider( String provider )
    {
        this.provider = provider;
    }

    /**
     * Sets verify property. This project property holds the result of a
     * checksum verification - "true" or "false"
     *
     * @param verifyProperty The new Verifyproperty value
     */
    public void setVerifyproperty( String verifyProperty )
    {
        this.verifyProperty = verifyProperty;
    }

    /**
     * Adds a set of files (nested fileset attribute).
     *
     * @param set The feature to be added to the Fileset attribute
     */
    public void addFileset( FileSet set )
    {
        filesets.add( set );
    }

    /**
     * Calculate the checksum(s)
     *
     * @return Returns true if the checksum verification test passed, false
     *      otherwise.
     * @exception TaskException Description of Exception
     */
    public boolean eval()
        throws TaskException
    {
        isCondition = true;
        return validateAndExecute();
    }

    /**
     * Calculate the checksum(s).
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        boolean value = validateAndExecute();
        if( verifyProperty != null )
        {
            setProperty( verifyProperty, new Boolean( value ).toString() );
        }
    }

    /**
     * Add key-value pair to the hashtable upon which to later operate upon.
     *
     * @param file The feature to be added to the ToIncludeFileMap attribute
     * @exception TaskException Description of Exception
     */
    private void addToIncludeFileMap( File file )
        throws TaskException
    {
        if( file != null )
        {
            if( file.exists() )
            {
                if( property == null )
                {
                    File dest = new File( file.getParent(), file.getName() + fileext );
                    if( forceOverwrite || isCondition ||
                        ( file.lastModified() > dest.lastModified() ) )
                    {
                        includeFileMap.put( file, dest );
                    }
                    else
                    {
                        getLogger().debug( file + " omitted as " + dest + " is up to date." );
                    }
                }
                else
                {
                    includeFileMap.put( file, property );
                }
            }
            else
            {
                String message = "Could not find file "
                    + file.getAbsolutePath()
                    + " to generate checksum for.";
                getLogger().info( message );
                throw new TaskException( message );
            }
        }
    }

    /**
     * Generate checksum(s) using the message digest created earlier.
     *
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     */
    private boolean generateChecksums()
        throws TaskException
    {
        boolean checksumMatches = true;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try
        {
            for( Enumeration e = includeFileMap.keys(); e.hasMoreElements(); )
            {
                messageDigest.reset();
                File src = (File)e.nextElement();
                if( !isCondition )
                {
                    getLogger().info( "Calculating " + algorithm + " checksum for " + src );
                }
                fis = new FileInputStream( src );
                DigestInputStream dis = new DigestInputStream( fis,
                                                               messageDigest );
                while( dis.read() != -1 )
                    ;
                dis.close();
                fis.close();
                fis = null;
                byte[] fileDigest = messageDigest.digest();
                String checksum = "";
                for( int i = 0; i < fileDigest.length; i++ )
                {
                    String hexStr = Integer.toHexString( 0x00ff & fileDigest[ i ] );
                    if( hexStr.length() < 2 )
                    {
                        checksum += "0";
                    }
                    checksum += hexStr;
                }
                //can either be a property name string or a file
                Object destination = includeFileMap.get( src );
                if( destination instanceof java.lang.String )
                {
                    String prop = (String)destination;
                    if( isCondition )
                    {
                        checksumMatches = checksum.equals( property );
                    }
                    else
                    {
                        setProperty( prop, checksum );
                    }
                }
                else if( destination instanceof java.io.File )
                {
                    if( isCondition )
                    {
                        File existingFile = (File)destination;
                        if( existingFile.exists() &&
                            existingFile.length() == checksum.length() )
                        {
                            fis = new FileInputStream( existingFile );
                            InputStreamReader isr = new InputStreamReader( fis );
                            BufferedReader br = new BufferedReader( isr );
                            String suppliedChecksum = br.readLine();
                            fis.close();
                            fis = null;
                            br.close();
                            isr.close();
                            checksumMatches =
                                checksum.equals( suppliedChecksum );
                        }
                        else
                        {
                            checksumMatches = false;
                        }
                    }
                    else
                    {
                        File dest = (File)destination;
                        fos = new FileOutputStream( dest );
                        fos.write( checksum.getBytes() );
                        fos.close();
                        fos = null;
                    }
                }
            }
        }
        catch( Exception e )
        {
            throw new TaskException( "Error", e );
        }
        finally
        {
            if( fis != null )
            {
                try
                {
                    fis.close();
                }
                catch( IOException e )
                {
                }
            }
            if( fos != null )
            {
                try
                {
                    fos.close();
                }
                catch( IOException e )
                {
                }
            }
        }
        return checksumMatches;
    }

    /**
     * Validate attributes and get down to business.
     *
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     */
    private boolean validateAndExecute()
        throws TaskException
    {

        if( file == null && filesets.size() == 0 )
        {
            throw new TaskException(
                "Specify at least one source - a file or a fileset." );
        }

        if( file != null && file.exists() && file.isDirectory() )
        {
            throw new TaskException(
                "Checksum cannot be generated for directories" );
        }

        if( property != null && fileext != null )
        {
            throw new TaskException(
                "Property and FileExt cannot co-exist." );
        }

        if( property != null )
        {
            if( forceOverwrite )
            {
                throw new TaskException(
                    "ForceOverwrite cannot be used when Property is specified" );
            }

            if( file != null )
            {
                if( filesets.size() > 0 )
                {
                    throw new TaskException(
                        "Multiple files cannot be used when Property is specified" );
                }
            }
            else
            {
                if( filesets.size() > 1 )
                {
                    throw new TaskException(
                        "Multiple files cannot be used when Property is specified" );
                }
            }
        }

        if( verifyProperty != null )
        {
            isCondition = true;
        }

        if( verifyProperty != null && forceOverwrite )
        {
            throw new TaskException(
                "VerifyProperty and ForceOverwrite cannot co-exist." );
        }

        if( isCondition && forceOverwrite )
        {
            throw new TaskException(
                "ForceOverwrite cannot be used when conditions are being used." );
        }

        if( fileext == null )
        {
            fileext = "." + algorithm;
        }
        else if( fileext.trim().length() == 0 )
        {
            throw new TaskException(
                "File extension when specified must not be an empty string" );
        }

        messageDigest = null;
        if( provider != null )
        {
            try
            {
                messageDigest = MessageDigest.getInstance( algorithm, provider );
            }
            catch( NoSuchAlgorithmException noalgo )
            {
                throw new TaskException( noalgo.toString(), noalgo );
            }
            catch( NoSuchProviderException noprovider )
            {
                throw new TaskException( noprovider.toString(), noprovider );
            }
        }
        else
        {
            try
            {
                messageDigest = MessageDigest.getInstance( algorithm );
            }
            catch( NoSuchAlgorithmException noalgo )
            {
                throw new TaskException( noalgo.toString(), noalgo );
            }
        }

        if( messageDigest == null )
        {
            throw new TaskException( "Unable to create Message Digest" );
        }

        addToIncludeFileMap( file );

        int sizeofFileSet = filesets.size();
        for( int i = 0; i < sizeofFileSet; i++ )
        {
            FileSet fs = (FileSet)filesets.get( i );
            DirectoryScanner ds = fs.getDirectoryScanner();
            String[] srcFiles = ds.getIncludedFiles();
            for( int j = 0; j < srcFiles.length; j++ )
            {
                File src = new File( fs.getDir(), srcFiles[ j ] );
                addToIncludeFileMap( src );
            }
        }

        return generateChecksums();
    }
}
