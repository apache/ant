/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.archive;

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
import java.util.zip.ZipFile;
import org.apache.aut.zip.ZipOutputStream;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.taskdefs.manifest.Manifest;
import org.apache.aut.manifest.ManifestException;
import org.apache.aut.manifest.ManifestUtil;
import org.apache.tools.ant.types.FileScanner;

/**
 * Creates a JAR archive.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 */
public class Jar
    extends Zip
{
    /**
     * The index file name.
     */
    private final static String INDEX_NAME = "META-INF/INDEX.LIST";

    /**
     * true if a manifest has been specified in the task
     */
    private boolean buildFileManifest;

    /**
     * jar index is JDK 1.3+ only
     */
    private boolean m_index;
    private Manifest m_execManifest;
    private Manifest m_manifest;
    private File m_manifestFile;

    /**
     * constructor
     */
    public Jar()
    {
        super();
        m_archiveType = "jar";
        m_emptyBehavior = "create";
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
        m_index = flag;
    }

    public void setManifest( File manifestFile )
        throws TaskException
    {
        if( !manifestFile.exists() )
        {
            final String message = "Manifest file: " + manifestFile + " does not exist.";
            throw new TaskException( message );
        }

        this.m_manifestFile = manifestFile;

        Reader r = null;
        try
        {
            r = new FileReader( manifestFile );
            Manifest newManifest = ManifestUtil.buildManifest( r );
            if( m_manifest == null )
            {
                m_manifest = ManifestUtil.getDefaultManifest();
            }
            m_manifest.merge( newManifest );
        }
        catch( ManifestException e )
        {
            final String message = "Manifest " + manifestFile + " is invalid: " + e.getMessage();
            getLogger().error( message );
            throw new TaskException( message, e );
        }
        catch( IOException e )
        {
            final String message = "Unable to read manifest file: " + manifestFile;
            throw new TaskException( message, e );
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
        final String message = "JARs are never empty, they contain at least a manifest file";
        getLogger().warn( message );
    }

    public void addManifest( Manifest newManifest )
        throws ManifestException, TaskException
    {
        if( m_manifest == null )
        {
            m_manifest = ManifestUtil.getDefaultManifest();
        }
        m_manifest.merge( newManifest );
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
        if( buildFileManifest || m_manifestFile == null )
        {
            java.util.zip.ZipFile theZipFile = null;
            try
            {
                theZipFile = new ZipFile( zipFile );
                java.util.zip.ZipEntry entry = theZipFile.getEntry( "META-INF/MANIFEST.MF" );
                if( entry == null )
                {
                    getLogger().debug( "Updating jar since the current jar has no manifest" );
                    return false;
                }
                Manifest currentManifest = ManifestUtil.buildManifest( new InputStreamReader( theZipFile.getInputStream( entry ) ) );
                if( m_manifest == null )
                {
                    m_manifest = ManifestUtil.getDefaultManifest();
                }
                if( !currentManifest.equals( m_manifest ) )
                {
                    getLogger().debug( "Updating jar since jar manifest has changed" );
                    return false;
                }
            }
            catch( Exception e )
            {
                // any problems and we will rebuild
                getLogger().debug( "Updating jar since cannot read current jar manifest: " + e.getClass().getName() + e.getMessage() );
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
        else if( m_manifestFile.lastModified() > zipFile.lastModified() )
        {
            return false;
        }
        return super.isUpToDate( scanners, zipFile );
    }

    protected boolean createEmptyZip( File zipFile )
    {
        // Jar files always contain a manifest and can never be empty
        return false;
    }

    protected void finalizeZipOutputStream( ZipOutputStream zOut )
        throws IOException, TaskException
    {
        if( m_index )
        {
            createIndexList( zOut );
        }
    }

    protected void initZipOutputStream( ZipOutputStream zOut )
        throws IOException, TaskException
    {
        try
        {
            m_execManifest = ManifestUtil.getDefaultManifest();

            if( m_manifest != null )
            {
                m_execManifest.merge( m_manifest );
            }
            /*
            for( Iterator e = m_execManifest.getWarnings(); e.hasNext(); )
            {
                getLogger().warn( "Manifest warning: " + (String)e.next() );
            }
            */

            zipDir( null, zOut, "META-INF/" );
            // time to write the manifest
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter( baos );
            Manifest manifest = m_execManifest;
            ManifestUtil.write( manifest, writer );
            writer.flush();

            ByteArrayInputStream bais = new ByteArrayInputStream( baos.toByteArray() );
            super.zipFile( bais, zOut, "META-INF/MANIFEST.MF", System.currentTimeMillis() );
            super.initZipOutputStream( zOut );
        }
        catch( ManifestException e )
        {
            getLogger().error( "Manifest is invalid: " + e.getMessage() );
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
            final String message = "Warning: selected " + m_archiveType +
                " files include a META-INF/MANIFEST.MF which will be ignored " +
                "(please use manifest attribute to " + m_archiveType + " task)";
            getLogger().warn( message );
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
        writer.println( m_file.getName() );

        // JarIndex is sorting the directories by ascending order.
        // it's painful to do in JDK 1.1 and it has no value but cosmetic
        // since it will be read into a hashtable by the classloader.
        Enumeration enum = m_addedDirs.keys();
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
            if( m_execManifest == null )
            {
                m_execManifest = ManifestUtil.buildManifest( new InputStreamReader( is ) );
            }
            else if( isAddingNewFiles() )
            {
                final Manifest other = ManifestUtil.buildManifest( new InputStreamReader( is ) );
                m_execManifest.merge( other );
            }
        }
        catch( ManifestException e )
        {
            getLogger().error( "Manifest is invalid: " + e.getMessage() );
            throw new TaskException( "Invalid Manifest", e );
        }
    }
}
