/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.i18n;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Locale;
import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.FileSet;

/**
 * Translates text embedded in files using Resource Bundle files.
 *
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 */
public class Translate extends MatchingTask
{
    /**
     * ArrayList to hold source file sets.
     */
    private ArrayList filesets = new ArrayList();
    /**
     * Holds key value pairs loaded from resource bundle file
     */
    private Hashtable resourceMap = new Hashtable();
    /**
     * Last Modified Timestamp of resource bundle file being used.
     */
    private long[] bundleLastModified = new long[ 7 ];
    /**
     * Has at least one file from the bundle been loaded?
     */
    private boolean loaded = false;

    /**
     * Family name of resource bundle
     */
    private String bundle;
    /**
     * Locale specific country of the resource bundle
     */
    private String bundleCountry;
    /**
     * Resource Bundle file encoding scheme, defaults to srcEncoding
     */
    private String bundleEncoding;
    /**
     * Locale specific language of the resource bundle
     */
    private String bundleLanguage;
    /**
     * Locale specific variant of the resource bundle
     */
    private String bundleVariant;
    /**
     * Destination file encoding scheme
     */
    private String destEncoding;
    /**
     * Last Modified Timestamp of destination file being used.
     */
    private long destLastModified;
    /**
     * Ending token to identify keys
     */
    private String endToken;
    /**
     * Create new destination file? Defaults to false.
     */
    private boolean forceOverwrite;
    /**
     * Generated locale based on user attributes
     */
    private Locale locale;
    /**
     * Source file encoding scheme
     */
    private String srcEncoding;
    /**
     * Last Modified Timestamp of source file being used.
     */
    private long srcLastModified;
    /**
     * Starting token to identify keys
     */
    private String startToken;
    /**
     * Destination directory
     */
    private File toDir;

    /**
     * Sets Family name of resource bundle
     *
     * @param bundle The new Bundle value
     */
    public void setBundle( String bundle )
    {
        this.bundle = bundle;
    }

    /**
     * Sets locale specific country of resource bundle
     *
     * @param bundleCountry The new BundleCountry value
     */
    public void setBundleCountry( String bundleCountry )
    {
        this.bundleCountry = bundleCountry;
    }

    /**
     * Sets Resource Bundle file encoding scheme
     *
     * @param bundleEncoding The new BundleEncoding value
     */
    public void setBundleEncoding( String bundleEncoding )
    {
        this.bundleEncoding = bundleEncoding;
    }

    /**
     * Sets locale specific language of resource bundle
     *
     * @param bundleLanguage The new BundleLanguage value
     */
    public void setBundleLanguage( String bundleLanguage )
    {
        this.bundleLanguage = bundleLanguage;
    }

    /**
     * Sets locale specific variant of resource bundle
     *
     * @param bundleVariant The new BundleVariant value
     */
    public void setBundleVariant( String bundleVariant )
    {
        this.bundleVariant = bundleVariant;
    }

    /**
     * Sets destination file encoding scheme. Defaults to source file encoding
     *
     * @param destEncoding The new DestEncoding value
     */
    public void setDestEncoding( String destEncoding )
    {
        this.destEncoding = destEncoding;
    }

    /**
     * Sets ending token to identify keys
     *
     * @param endToken The new EndToken value
     */
    public void setEndToken( String endToken )
    {
        this.endToken = endToken;
    }

    /**
     * Overwrite existing file irrespective of whether it is newer than the
     * source file as well as the resource bundle file? Defaults to false.
     *
     * @param forceOverwrite The new ForceOverwrite value
     */
    public void setForceOverwrite( boolean forceOverwrite )
    {
        this.forceOverwrite = forceOverwrite;
    }

    /**
     * Sets source file encoding scheme
     *
     * @param srcEncoding The new SrcEncoding value
     */
    public void setSrcEncoding( String srcEncoding )
    {
        this.srcEncoding = srcEncoding;
    }

    /**
     * Sets starting token to identify keys
     *
     * @param startToken The new StartToken value
     */
    public void setStartToken( String startToken )
    {
        this.startToken = startToken;
    }

    /**
     * Sets Destination directory
     *
     * @param toDir The new ToDir value
     */
    public void setToDir( File toDir )
    {
        this.toDir = toDir;
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
     * Check attributes values, load resource map and translate
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        if( bundle == null )
        {
            throw new TaskException( "The bundle attribute must be set." );
        }

        if( startToken == null )
        {
            throw new TaskException( "The starttoken attribute must be set." );
        }

        if( startToken.length() != 1 )
        {
            throw new TaskException(
                "The starttoken attribute must be a single character." );
        }

        if( endToken == null )
        {
            throw new TaskException( "The endtoken attribute must be set." );
        }

        if( endToken.length() != 1 )
        {
            throw new TaskException(
                "The endtoken attribute must be a single character." );
        }

        if( bundleLanguage == null )
        {
            Locale l = Locale.getDefault();
            bundleLanguage = l.getLanguage();
        }

        if( bundleCountry == null )
        {
            bundleCountry = Locale.getDefault().getCountry();
        }

        locale = new Locale( bundleLanguage, bundleCountry );

        if( bundleVariant == null )
        {
            Locale l = new Locale( bundleLanguage, bundleCountry );
            bundleVariant = l.getVariant();
        }

        if( toDir == null )
        {
            throw new TaskException( "The todir attribute must be set." );
        }

        if( !toDir.exists() )
        {
            toDir.mkdirs();
        }
        else
        {
            if( toDir.isFile() )
            {
                throw new TaskException( toDir + " is not a directory" );
            }
        }

        if( srcEncoding == null )
        {
            srcEncoding = System.getProperty( "file.encoding" );
        }

        if( destEncoding == null )
        {
            destEncoding = srcEncoding;
        }

        if( bundleEncoding == null )
        {
            bundleEncoding = srcEncoding;
        }

        loadResourceMaps();

        translate();
    }

    /**
     * Load resourceMap with key value pairs. Values of existing keys are not
     * overwritten. Bundle's encoding scheme is used.
     *
     * @param ins Description of Parameter
     * @exception TaskException Description of Exception
     */
    private void loadResourceMap( FileInputStream ins )
        throws TaskException
    {
        try
        {
            BufferedReader in = null;
            InputStreamReader isr = new InputStreamReader( ins, bundleEncoding );
            in = new BufferedReader( isr );
            String line = null;
            while( ( line = in.readLine() ) != null )
            {
                //So long as the line isn't empty and isn't a comment...
                if( line.trim().length() > 1 &&
                    ( '#' != line.charAt( 0 ) || '!' != line.charAt( 0 ) ) )
                {
                    //Legal Key-Value separators are :, = and white space.
                    int sepIndex = line.indexOf( '=' );
                    if( -1 == sepIndex )
                    {
                        sepIndex = line.indexOf( ':' );
                    }
                    if( -1 == sepIndex )
                    {
                        for( int k = 0; k < line.length(); k++ )
                        {
                            if( Character.isSpaceChar( line.charAt( k ) ) )
                            {
                                sepIndex = k;
                                break;
                            }
                        }
                    }
                    //Only if we do have a key is there going to be a value
                    if( -1 != sepIndex )
                    {
                        String key = line.substring( 0, sepIndex ).trim();
                        String value = line.substring( sepIndex + 1 ).trim();
                        //Handle line continuations, if any
                        while( value.endsWith( "\\" ) )
                        {
                            value = value.substring( 0, value.length() - 1 );
                            if( ( line = in.readLine() ) != null )
                            {
                                value = value + line.trim();
                            }
                            else
                            {
                                break;
                            }
                        }
                        if( key.length() > 0 )
                        {
                            //Has key already been loaded into resourceMap?
                            if( resourceMap.get( key ) == null )
                            {
                                resourceMap.put( key, value );
                            }
                        }
                    }
                }
            }
            if( in != null )
            {
                in.close();
            }
        }
        catch( IOException ioe )
        {
            throw new TaskException( ioe.getMessage() );
        }
    }

    /**
     * Load resource maps based on resource bundle encoding scheme. The resource
     * bundle lookup searches for resource files with various suffixes on the
     * basis of (1) the desired locale and (2) the default locale
     * (basebundlename), in the following order from lower-level (more specific)
     * to parent-level (less specific): basebundlename + "_" + language1 + "_" +
     * country1 + "_" + variant1 basebundlename + "_" + language1 + "_" +
     * country1 basebundlename + "_" + language1 basebundlename basebundlename +
     * "_" + language2 + "_" + country2 + "_" + variant2 basebundlename + "_" +
     * language2 + "_" + country2 basebundlename + "_" + language2 To the
     * generated name, a ".properties" string is appeneded and once this file is
     * located, it is treated just like a properties file but with bundle
     * encoding also considered while loading.
     *
     * @exception TaskException Description of Exception
     */
    private void loadResourceMaps()
        throws TaskException
    {
        Locale locale = new Locale( bundleLanguage,
                                    bundleCountry,
                                    bundleVariant );
        String language = locale.getLanguage().length() > 0 ?
            "_" + locale.getLanguage() :
            "";
        String country = locale.getCountry().length() > 0 ?
            "_" + locale.getCountry() :
            "";
        String variant = locale.getVariant().length() > 0 ?
            "_" + locale.getVariant() :
            "";
        String bundleFile = bundle + language + country + variant;
        processBundle( bundleFile, 0, false );

        bundleFile = bundle + language + country;
        processBundle( bundleFile, 1, false );

        bundleFile = bundle + language;
        processBundle( bundleFile, 2, false );

        bundleFile = bundle;
        processBundle( bundleFile, 3, false );

        //Load default locale bundle files
        //using default file encoding scheme.
        locale = Locale.getDefault();

        language = locale.getLanguage().length() > 0 ?
            "_" + locale.getLanguage() :
            "";
        country = locale.getCountry().length() > 0 ?
            "_" + locale.getCountry() :
            "";
        variant = locale.getVariant().length() > 0 ?
            "_" + locale.getVariant() :
            "";
        bundleEncoding = System.getProperty( "file.encoding" );

        bundleFile = bundle + language + country + variant;
        processBundle( bundleFile, 4, false );

        bundleFile = bundle + language + country;
        processBundle( bundleFile, 5, false );

        bundleFile = bundle + language;
        processBundle( bundleFile, 6, true );
    }

    /**
     * Process each file that makes up this bundle.
     *
     * @param bundleFile Description of Parameter
     * @param i Description of Parameter
     * @param checkLoaded Description of Parameter
     * @exception TaskException Description of Exception
     */
    private void processBundle( String bundleFile, int i,
                                boolean checkLoaded )
        throws TaskException
    {
        bundleFile += ".properties";
        FileInputStream ins = null;
        try
        {
            ins = new FileInputStream( bundleFile );
            loaded = true;
            bundleLastModified[ i ] = new File( bundleFile ).lastModified();
            log( "Using " + bundleFile, Project.MSG_DEBUG );
            loadResourceMap( ins );
        }
        catch( IOException ioe )
        {
            log( bundleFile + " not found.", Project.MSG_DEBUG );
            //if all resource files associated with this bundle
            //have been scanned for and still not able to
            //find a single resrouce file, throw exception
            if( !loaded && checkLoaded )
            {
                throw new TaskException( ioe.getMessage() );
            }
        }
    }

    /**
     * Reads source file line by line using the source encoding and searches for
     * keys that are sandwiched between the startToken and endToken. The values
     * for these keys are looked up from the hashtable and substituted. If the
     * hashtable doesn't contain the key, they key itself is used as the value.
     * Detination files and directories are created as needed. The destination
     * file is overwritten only if the forceoverwritten attribute is set to true
     * if the source file or any associated bundle resource file is newer than
     * the destination file.
     *
     * @exception TaskException Description of Exception
     */
    private void translate()
        throws TaskException
    {
        for( int i = 0; i < filesets.size(); i++ )
        {
            FileSet fs = (FileSet)filesets.get( i );
            DirectoryScanner ds = fs.getDirectoryScanner( getProject() );
            String[] srcFiles = ds.getIncludedFiles();
            for( int j = 0; j < srcFiles.length; j++ )
            {
                try
                {
                    File dest = FileUtil.resolveFile( toDir, srcFiles[ j ] );
                    //Make sure parent dirs exist, else, create them.
                    try
                    {
                        File destDir = new File( dest.getParent() );
                        if( !destDir.exists() )
                        {
                            destDir.mkdirs();
                        }
                    }
                    catch( Exception e )
                    {
                        log( "Exception occured while trying to check/create "
                             + " parent directory.  " + e.getMessage(),
                             Project.MSG_DEBUG );
                    }
                    destLastModified = dest.lastModified();
                    srcLastModified = new File( srcFiles[ i ] ).lastModified();
                    //Check to see if dest file has to be recreated
                    if( forceOverwrite
                        || destLastModified < srcLastModified
                        || destLastModified < bundleLastModified[ 0 ]
                        || destLastModified < bundleLastModified[ 1 ]
                        || destLastModified < bundleLastModified[ 2 ]
                        || destLastModified < bundleLastModified[ 3 ]
                        || destLastModified < bundleLastModified[ 4 ]
                        || destLastModified < bundleLastModified[ 5 ]
                        || destLastModified < bundleLastModified[ 6 ] )
                    {
                        log( "Processing " + srcFiles[ j ],
                             Project.MSG_DEBUG );
                        FileOutputStream fos = new FileOutputStream( dest );
                        BufferedWriter out = new BufferedWriter(
                            new OutputStreamWriter( fos,
                                                    destEncoding ) );
                        FileInputStream fis = new FileInputStream( srcFiles[ j ] );
                        BufferedReader in = new BufferedReader(
                            new InputStreamReader( fis,
                                                   srcEncoding ) );
                        String line;
                        while( ( line = in.readLine() ) != null )
                        {
                            StringBuffer newline = new StringBuffer( line );
                            int startIndex = -1;
                            int endIndex = -1;
                            outer :
                            while( true )
                            {
                                startIndex = line.indexOf( startToken, endIndex + 1 );
                                if( startIndex < 0 ||
                                    startIndex + 1 >= line.length() )
                                {
                                    break;
                                }
                                endIndex = line.indexOf( endToken, startIndex + 1 );
                                if( endIndex < 0 )
                                {
                                    break;
                                }
                                String matches = line.substring( startIndex + 1,
                                                                 endIndex );
                                //If there is a white space or = or :, then
                                //it isn't to be treated as a valid key.
                                for( int k = 0; k < matches.length(); k++ )
                                {
                                    char c = matches.charAt( k );
                                    if( c == ':' ||
                                        c == '=' ||
                                        Character.isSpaceChar( c ) )
                                    {
                                        endIndex = endIndex - 1;
                                        continue outer;
                                    }
                                }
                                String replace = null;
                                replace = (String)resourceMap.get( matches );
                                //If the key hasn't been loaded into resourceMap,
                                //use the key itself as the value also.
                                if( replace == null )
                                {
                                    log( "Warning: The key: " + matches
                                         + " hasn't been defined.",
                                         Project.MSG_DEBUG );
                                    replace = matches;
                                }
                                line = line.substring( 0, startIndex )
                                    + replace
                                    + line.substring( endIndex + 1 );
                                endIndex = startIndex + replace.length() + 1;
                                if( endIndex + 1 >= line.length() )
                                {
                                    break;
                                }
                            }
                            out.write( line );
                            out.newLine();
                        }
                        if( in != null )
                        {
                            in.close();
                        }
                        if( out != null )
                        {
                            out.close();
                        }
                    }
                    else
                    {
                        log( "Skipping " + srcFiles[ j ] +
                             " as destination file is up to date",
                             Project.MSG_VERBOSE );
                    }
                }
                catch( IOException ioe )
                {
                    throw new TaskException( ioe.getMessage() );
                }
            }
        }
    }
}
