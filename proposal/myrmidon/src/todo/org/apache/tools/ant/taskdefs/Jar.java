/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Enumeration;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.FileScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.zip.ZipOutputStream;

/**
 * Creates a JAR archive.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 */
public class Jar extends Zip
{
    /**
     * The index file name.
     */
    private final static String INDEX_NAME = "META-INF/INDEX.LIST";

    /**
     * true if a manifest has been specified in the task
     */
    private boolean buildFileManifest = false;

    /**
     * jar index is JDK 1.3+ only
     */
    private boolean index = false;
    private Manifest execManifest;
    private Manifest manifest;

    private File manifestFile;

    /**
     * constructor
     */
    public Jar()
    {
        super();
        archiveType = "jar";
        emptyBehavior = "create";
        setEncoding( "UTF8" );
    }

    /**
     * Set whether or not to create an index list for classes to speed up
     * classloading.
     *
     * @param flag The new Index value
     */
    public void setIndex( boolean flag )
    {
        index = flag;
    }

    public void setManifest( File manifestFile )
        throws TaskException
    {
        if( !manifestFile.exists() )
        {
            throw new TaskException( "Manifest file: " + manifestFile + " does not exist." );
        }

        this.manifestFile = manifestFile;

        Reader r = null;
        try
        {
            r = new FileReader( manifestFile );
            Manifest newManifest = new Manifest( r );
            if( manifest == null )
            {
                manifest = Manifest.getDefaultManifest();
            }
            manifest.merge( newManifest );
        }
        catch( ManifestException e )
        {
            log( "Manifest is invalid: " + e.getMessage(), Project.MSG_ERR );
            throw new TaskException( "Invalid Manifest: " + manifestFile, e );
        }
        catch( IOException e )
        {
            throw new TaskException( "Unable to read manifest file: " + manifestFile, e );
        }
        finally
        {
            if( r != null )
            {
                try
                {
                    r.close();
                }
                catch( IOException e )
                {
                    // do nothing
                }
            }
        }
    }

    public void setWhenempty( WhenEmpty we )
    {
        log( "JARs are never empty, they contain at least a manifest file",
             Project.MSG_WARN );
    }

    public void addConfiguredManifest( Manifest newManifest )
        throws ManifestException, TaskException
    {
        if( manifest == null )
        {
            manifest = Manifest.getDefaultManifest();
        }
        manifest.merge( newManifest );
        buildFileManifest = true;
    }

    public void addMetainf( ZipFileSet fs )
    {
        // We just set the prefix for this fileset, and pass it up.
        fs.setPrefix( "META-INF/" );
        super.addFileset( fs );
    }

    /**
     * Check whether the archive is up-to-date;
     *
     * @param scanners list of prepared scanners containing files to archive
     * @param zipFile intended archive file (may or may not exist)
     * @return true if nothing need be done (may have done something already);
     *      false if archive creation should proceed
     * @exception TaskException if it likes
     */
    protected boolean isUpToDate( FileScanner[] scanners, File zipFile )
        throws TaskException
    {
        // need to handle manifest as a special check
        if( buildFileManifest || manifestFile == null )
        {
            java.util.zip.ZipFile theZipFile = null;
            try
            {
                theZipFile = new java.util.zip.ZipFile( zipFile );
                java.util.zip.ZipEntry entry = theZipFile.getEntry( "META-INF/MANIFEST.MF" );
                if( entry == null )
                {
                    log( "Updating jar since the current jar has no manifest", Project.MSG_VERBOSE );
                    return false;
                }
                Manifest currentManifest = new Manifest( new InputStreamReader( theZipFile.getInputStream( entry ) ) );
                if( manifest == null )
                {
                    manifest = Manifest.getDefaultManifest();
                }
                if( !currentManifest.equals( manifest ) )
                {
                    log( "Updating jar since jar manifest has changed", Project.MSG_VERBOSE );
                    return false;
                }
            }
            catch( Exception e )
            {
                // any problems and we will rebuild
                log( "Updating jar since cannot read current jar manifest: " + e.getClass().getName() + e.getMessage(),
                     Project.MSG_VERBOSE );
                return false;
            }
            finally
            {
                if( theZipFile != null )
                {
                    try
                    {
                        theZipFile.close();
                    }
                    catch( IOException e )
                    {
                        //ignore
                    }
                }
            }
        }
        else if( manifestFile.lastModified() > zipFile.lastModified() )
        {
            return false;
        }
        return super.isUpToDate( scanners, zipFile );
    }

    /**
     * Make sure we don't think we already have a MANIFEST next time this task
     * gets executed.
     */
    protected void cleanUp()
    {
        super.cleanUp();
    }

    protected boolean createEmptyZip( File zipFile )
    {
        // Jar files always contain a manifest and can never be empty
        return false;
    }

    protected void finalizeZipOutputStream( ZipOutputStream zOut )
        throws IOException, TaskException
    {
        if( index )
        {
            createIndexList( zOut );
        }
    }

    protected void initZipOutputStream( ZipOutputStream zOut )
        throws IOException, TaskException
    {
        try
        {
            execManifest = Manifest.getDefaultManifest();

            if( manifest != null )
            {
                execManifest.merge( manifest );
            }
            for( Enumeration e = execManifest.getWarnings(); e.hasMoreElements(); )
            {
                log( "Manifest warning: " + (String)e.nextElement(), Project.MSG_WARN );
            }

            zipDir( null, zOut, "META-INF/" );
            // time to write the manifest
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter( baos );
            execManifest.write( writer );
            writer.flush();

            ByteArrayInputStream bais = new ByteArrayInputStream( baos.toByteArray() );
            super.zipFile( bais, zOut, "META-INF/MANIFEST.MF", System.currentTimeMillis() );
            super.initZipOutputStream( zOut );
        }
        catch( ManifestException e )
        {
            log( "Manifest is invalid: " + e.getMessage(), Project.MSG_ERR );
            throw new TaskException( "Invalid Manifest", e );
        }
    }

    protected void zipFile( File file, ZipOutputStream zOut, String vPath )
        throws IOException, TaskException
    {
        // If the file being added is META-INF/MANIFEST.MF, we warn if it's not the
        // one specified in the "manifest" attribute - or if it's being added twice,
        // meaning the same file is specified by the "manifeset" attribute and in
        // a <fileset> element.
        if( vPath.equalsIgnoreCase( "META-INF/MANIFEST.MF" ) )
        {
            log( "Warning: selected " + archiveType + " files include a META-INF/MANIFEST.MF which will be ignored " +
                 "(please use manifest attribute to " + archiveType + " task)", Project.MSG_WARN );
        }
        else
        {
            super.zipFile( file, zOut, vPath );
        }

    }

    protected void zipFile( InputStream is, ZipOutputStream zOut, String vPath, long lastModified )
        throws IOException, TaskException
    {
        // If the file being added is META-INF/MANIFEST.MF, we merge it with the
        // current manifest
        if( vPath.equalsIgnoreCase( "META-INF/MANIFEST.MF" ) )
        {
            try
            {
                zipManifestEntry( is );
            }
            catch( IOException e )
            {
                throw new TaskException( "Unable to read manifest file: ", e );
            }
        }
        else
        {
            super.zipFile( is, zOut, vPath, lastModified );
        }
    }

    /**
     * Create the index list to speed up classloading. This is a JDK 1.3+
     * specific feature and is enabled by default. {@link
     * http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#JAR%20Index}
     *
     * @param zOut the zip stream representing the jar being built.
     * @throws IOException thrown if there is an error while creating the index
     *      and adding it to the zip stream.
     */
    private void createIndexList( ZipOutputStream zOut )
        throws IOException, TaskException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // encoding must be UTF8 as specified in the specs.
        PrintWriter writer = new PrintWriter( new OutputStreamWriter( baos, "UTF8" ) );

        // version-info blankline
        writer.println( "JarIndex-Version: 1.0" );
        writer.println();

        // header newline
        writer.println( zipFile.getName() );

        // JarIndex is sorting the directories by ascending order.
        // it's painful to do in JDK 1.1 and it has no value but cosmetic
        // since it will be read into a hashtable by the classloader.
        Enumeration enum = addedDirs.keys();
        while( enum.hasMoreElements() )
        {
            String dir = (String)enum.nextElement();

            // try to be smart, not to be fooled by a weird directory name
            // @fixme do we need to check for directories starting by ./ ?
            dir = dir.replace( '\\', '/' );
            int pos = dir.lastIndexOf( '/' );
            if( pos != -1 )
            {
                dir = dir.substring( 0, pos );
            }

            // looks like nothing from META-INF should be added
            // and the check is not case insensitive.
            // see sun.misc.JarIndex
            if( dir.startsWith( "META-INF" ) )
            {
                continue;
            }
            // name newline
            writer.println( dir );
        }

        writer.flush();
        ByteArrayInputStream bais = new ByteArrayInputStream( baos.toByteArray() );
        super.zipFile( bais, zOut, INDEX_NAME, System.currentTimeMillis() );
    }

    /**
     * Handle situation when we encounter a manifest file If we haven't been
     * given one, we use this one. If we have, we merge the manifest in,
     * provided it is a new file and not the old one from the JAR we are
     * updating
     *
     * @param is Description of Parameter
     * @exception IOException Description of Exception
     */
    private void zipManifestEntry( InputStream is )
        throws IOException, TaskException
    {
        try
        {
            if( execManifest == null )
            {
                execManifest = new Manifest( new InputStreamReader( is ) );
            }
            else if( isAddingNewFiles() )
            {
                execManifest.merge( new Manifest( new InputStreamReader( is ) ) );
            }
        }
        catch( ManifestException e )
        {
            log( "Manifest is invalid: " + e.getMessage(), Project.MSG_ERR );
            throw new TaskException( "Invalid Manifest", e );
        }
    }
}
