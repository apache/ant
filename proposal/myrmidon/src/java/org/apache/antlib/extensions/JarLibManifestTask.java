/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.extensions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.apache.avalon.excalibur.extension.Extension;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.myrmidon.Constants;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.todo.types.DirectoryScanner;
import org.apache.tools.todo.types.ScannerUtil;

/**
 * Task to generate a manifest that declares all the dependencies
 * in manifest. The dependencies are determined by looking in the
 * specified path and searching for Extension / "Optional Package"
 * specifications in the manifests of the jars.
 *
 * <p>Prior to JDK1.3, an "Optional Package" was known as an Extension.
 * The specification for this mechanism is available in the JDK1.3
 * documentation in the directory
 * $JDK_HOME/docs/guide/extensions/versioning.html. Alternatively it is
 * available online at <a href="http://java.sun.com/j2se/1.3/docs/guide/extensions/versioning.html">
 * http://java.sun.com/j2se/1.3/docs/guide/extensions/versioning.html</a>.</p>
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @ant.task name="jarlib-manifest"
 */
public final class JarLibManifestTask
    extends AbstractTask
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( JarLibManifestTask.class );

    /**
     * Version of manifest spec that task generates.
     */
    private static final String MANIFEST_VERSION = "1.0";

    /**
     * The library to display information about.
     */
    private File m_destfile;

    /**
     * The extension supported by this library (if any).
     */
    private Extension m_extension;

    /**
     * ExtensionAdapter objects representing
     * dependencies required by library.
     */
    private final ArrayList m_dependencies = new ArrayList();

    /**
     * ExtensionAdapter objects representing optional
     * dependencies required by library.
     */
    private final ArrayList m_optionals = new ArrayList();

    /**
     * Filesets specifying all the librarys
     * to generate dependency information about.
     */
    private final ArrayList m_dependsFilesets = new ArrayList();

    /**
     * Filesets specifying all the librarys
     * to generate optional dependency information about.
     */
    private final ArrayList m_optionalsFilesets = new ArrayList();

    /**
     * Extra attributes the user specifies for main section
     * in manifest.
     */
    private final ArrayList m_extraAttributes = new ArrayList();

    /**
     * The location where generated manifest is placed.
     *
     * @param destfile The location where generated manifest is placed.
     */
    public void setDestfile( final File destfile )
    {
        m_destfile = destfile;
    }

    /**
     * Adds an extension that this library implements.
     *
     * @param extensionAdapter an extension that this library implements.
     */
    public void addExtension( final ExtensionAdapter extensionAdapter )
        throws TaskException
    {
        if( null != m_extension )
        {
            final String message = REZ.getString( "manifest.multi-extension.error" );
            throw new TaskException( message );
        }
        else
        {
            m_extension = extensionAdapter.toExtension();
        }
    }

    /**
     * Adds an extension that this library requires.
     *
     * @param extensionAdapter an extension that this library requires.
     */
    public void addDepends( final ExtensionAdapter extensionAdapter )
    {
        m_dependencies.add( extensionAdapter );
    }

    /**
     * Adds an extension that this library optionally requires.
     *
     * @param extensionAdapter an extension that this library optionally requires.
     */
    public void addOption( final ExtensionAdapter extensionAdapter )
    {
        m_optionals.add( extensionAdapter );
    }

    /**
     * Adds a set of files about which library data will be displayed.
     *
     * @param fileSet a set of files about which library data will be displayed.
     */
    public void addDependsfileset( final LibFileSet fileSet )
    {
        m_dependsFilesets.add( fileSet );
    }

    /**
     * Adds a set of files about which library data will be displayed.
     *
     * @param fileSet a set of files about which library data will be displayed.
     */
    public void addOptionalfileset( final LibFileSet fileSet )
    {
        m_optionalsFilesets.add( fileSet );
    }

    /**
     * Adds an attribute that is to be put in main section of manifest.
     *
     * @param attribute an attribute that is to be put in main section of manifest.
     */
    public void addAttribute( final ExtraAttribute attribute )
    {
        m_extraAttributes.add( attribute );
    }

    public void execute()
        throws TaskException
    {
        validate();

        final Manifest manifest = new Manifest();
        final Attributes attributes = manifest.getMainAttributes();

        attributes.put( Attributes.Name.MANIFEST_VERSION, MANIFEST_VERSION );
        attributes.putValue( "Created-By", Constants.BUILD_DESCRIPTION );

        appendExtraAttributes( attributes );

        Extension.addExtension( m_extension, attributes );

        //Add all the dependency data to manifest for dependencies
        final ArrayList depends = toExtensions( m_dependencies );
        extractLibraryData( depends, m_dependsFilesets );
        appendExtensionList( attributes,
                             Extension.EXTENSION_LIST,
                             "lib",
                             depends.size() );
        appendLibraryList( attributes, "lib", depends );

        //Add all the dependency data to manifest for "optional"
        //dependencies
        final ArrayList option = toExtensions( m_optionals );
        extractLibraryData( option, m_optionalsFilesets );
        appendExtensionList( attributes,
                             Extension.OPTIONAL_EXTENSION_LIST,
                             "opt",
                             option.size() );
        appendLibraryList( attributes, "opt", option );

        try
        {
            final String message =
                REZ.getString( "manifest.file.notice",
                               m_destfile.getAbsoluteFile() );
            getContext().info( message );
            writeManifest( manifest );
        }
        catch( final IOException ioe )
        {
            throw new TaskException( ioe.getMessage(), ioe );
        }
    }

    /**
     * Validate the tasks parameters.
     *
     * @throws TaskException if invalid parameters found
     */
    private void validate()
        throws TaskException
    {
        if( null == m_destfile )
        {
            final String message =
                REZ.getString( "manifest.missing-file.error" );
            throw new TaskException( message );
        }
        if( m_destfile.exists() && !m_destfile.isFile() )
        {
            final String message =
                REZ.getString( "manifest.bad-file.error", m_destfile );
            throw new TaskException( message );
        }
    }

    /**
     * Add any extra attributes to the manifest.
     *
     * @param attributes the manifest section to write
     *        attributes to
     */
    private void appendExtraAttributes( final Attributes attributes )
    {
        final Iterator iterator = m_extraAttributes.iterator();
        while( iterator.hasNext() )
        {
            final ExtraAttribute attribute =
                (ExtraAttribute)iterator.next();
            attributes.putValue( attribute.getName(),
                                 attribute.getValue() );
        }
    }

    /**
     * Write out manifest to destfile.
     *
     * @param manifest the manifest
     * @throws IOException if error writing file
     */
    private void writeManifest( final Manifest manifest )
        throws IOException
    {
        FileOutputStream output = null;
        try
        {
            output = new FileOutputStream( m_destfile );
            manifest.write( output );
            output.flush();
        }
        finally
        {
            IOUtil.shutdownStream( output );
        }
    }

    /**
     * Generate a list of extensions from a specified fileset.
     *
     * @param librarys the list to add extensions to
     * @param fileset the filesets containing librarys
     * @throws TaskException if an error occurs
     */
    private void extractLibraryData( final ArrayList librarys,
                                     final ArrayList fileset )
        throws TaskException
    {
        if( !fileset.isEmpty() )
        {
            final Extension[] extensions = getExtensions( fileset );
            for( int i = 0; i < extensions.length; i++ )
            {
                librarys.add( extensions[ i ] );
            }
        }
    }

    /**
     * Append specified librarys extension data to specified attributes.
     * Use the extensionKey to list the extensions, usually "Extension-List:"
     * for required dependencies and "Optional-Extension-List:" for optional
     * dependencies. NOTE: "Optional" dependencies are not part of the
     * specification.
     *
     * @param attributes the attributes to add extensions to
     * @param librarys the filesets containing librarys
     * @throws TaskException if an error occurs
     */
    private void appendLibraryList( final Attributes attributes,
                                    final String listPrefix,
                                    final ArrayList librarys )
        throws TaskException
    {
        final int size = librarys.size();

        for( int i = 0; i < size; i++ )
        {
            final Extension extension = (Extension)librarys.get( i );
            final String prefix = listPrefix + i + "-";
            Extension.addExtension( extension, prefix, attributes );
        }
    }

    /**
     * Append an attribute such as "Extension-List: lib0 lib1 lib2"
     * using specified prefix and counting up to specified size.
     * Also use specified extensionKey so that can generate list of
     * optional dependencies aswell.
     *
     * @param size the number of librarys to list
     * @param listPrefix the prefix for all librarys
     * @param attributes the attributes to add key-value to
     * @param extensionKey the key to use
     */
    private void appendExtensionList( final Attributes attributes,
                                      final Attributes.Name extensionKey,
                                      final String listPrefix,
                                      final int size )
    {
        final StringBuffer sb = new StringBuffer();
        for( int i = 0; i < size; i++ )
        {
            sb.append( listPrefix + i );
            sb.append( ' ' );
        }

        //add in something like
        //"Extension-List: javahelp java3d"
        attributes.put( extensionKey, sb.toString() );
    }

    /**
     * Retrieve extensions from the specified librarys.
     *
     * @param librarys the filesets for librarys
     * @return the extensions contained in librarys
     * @throws TaskException if failing to scan librarys
     */
    private static Extension[] getExtensions( final ArrayList librarys )
        throws TaskException
    {
        final ArrayList extensions = new ArrayList();
        final Iterator iterator = librarys.iterator();
        while( iterator.hasNext() )
        {
            final LibFileSet fileSet = (LibFileSet)iterator.next();
            final DirectoryScanner scanner = ScannerUtil.getDirectoryScanner( fileSet );
            final File basedir = scanner.getBasedir();
            final String[] files = scanner.getIncludedFiles();
            for( int i = 0; i < files.length; i++ )
            {
                final File file = new File( basedir, files[ i ] );
                loadExtensions( file, extensions );
            }
        }
        return (Extension[])extensions.toArray( new Extension[ extensions.size() ] );
    }

    /**
     * Load list of available extensions from specified file.
     *
     * @param file the file
     * @param extensions the list to add available extensions to
     * @throws TaskException if there is an error
     */
    private static void loadExtensions( final File file,
                                        final ArrayList extensions )
        throws TaskException
    {
        try
        {
            final JarFile jarFile = new JarFile( file );
            final Extension[] extension =
                Extension.getAvailable( jarFile.getManifest() );
            for( int i = 0; i < extension.length; i++ )
            {
                extensions.add( extension[ i ] );
            }
        }
        catch( final Exception e )
        {
            throw new TaskException( e.getMessage(), e );
        }
    }

    /**
     * Convert a list of extensionAdapter objects to extensions.
     *
     * @param adapters the list of ExtensionAdapterss to add to convert
     * @throws TaskException if an error occurs
     */
    private static ArrayList toExtensions( final ArrayList adapters )
        throws TaskException
    {
        final ArrayList results = new ArrayList();

        final int size = adapters.size();
        for( int i = 0; i < size; i++ )
        {
            final ExtensionAdapter adapter =
                (ExtensionAdapter)adapters.get( i );
            final Extension extension = adapter.toExtension();
            results.add( extension );
        }

        return results;
    }
}
