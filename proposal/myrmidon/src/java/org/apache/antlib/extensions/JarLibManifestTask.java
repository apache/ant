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
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.apache.avalon.excalibur.extension.DeweyDecimal;
import org.apache.avalon.excalibur.extension.Extension;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.myrmidon.Constants;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.FileSet;
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
public class JarLibManifestTask
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
     * Filesets specifying all the librarys
     * to generate dependency information about.
     */
    private final Vector m_dependencies = new Vector();

    /**
     * Filesets specifying all the librarys
     * to generate optional dependency information about.
     */
    private final Vector m_optionals = new Vector();

    /**
     * The name of the optional package being made available, or required.
     */
    private String m_extensionName;

    /**
     * The version number (dotted decimal notation) of the specification
     * to which this optional package conforms.
     */
    private DeweyDecimal m_specificationVersion;

    /**
     * The name of the company or organization that originated the
     * specification to which this optional package conforms.
     */
    private String m_specificationVendor;

    /**
     * The unique identifier of the company that produced the optional
     * package contained in this JAR file.
     */
    private String m_implementationVendorID;

    /**
     * The name of the company or organization that produced this
     * implementation of this optional package.
     */
    private String m_implementationVendor;

    /**
     * The version number (dotted decimal notation) for this implementation
     * of the optional package.
     */
    private DeweyDecimal m_implementationVersion;

    /**
     * The URL from which the most recent version of this optional package
     * can be obtained if it is not already installed.
     */
    private String m_implementationURL;

    /**
     * Set the name of extension in generated manifest.
     *
     * @param extensionName the name of extension in generated manifest
     */
    public void setExtensionName( final String extensionName )
    {
        m_extensionName = extensionName;
    }

    /**
     * Set the specificationVersion of extension in generated manifest.
     *
     * @param specificationVersion the specificationVersion of extension in generated manifest
     */
    public void setSpecificationVersion( final String specificationVersion )
    {
        m_specificationVersion = new DeweyDecimal( specificationVersion );
    }

    /**
     * Set the specificationVendor of extension in generated manifest.
     *
     * @param specificationVendor the specificationVendor of extension in generated manifest
     */
    public void setSpecificationVendor( final String specificationVendor )
    {
        m_specificationVendor = specificationVendor;
    }

    /**
     * Set the implementationVendorID of extension in generated manifest.
     *
     * @param implementationVendorID the implementationVendorID of extension in generated manifest
     */
    public void setImplementationVendorID( final String implementationVendorID )
    {
        m_implementationVendorID = implementationVendorID;
    }

    /**
     * Set the implementationVendor of extension in generated manifest.
     *
     * @param implementationVendor the implementationVendor of extension in generated manifest
     */
    public void setImplementationVendor( final String implementationVendor )
    {
        m_implementationVendor = implementationVendor;
    }

    /**
     * Set the implementationVersion of extension in generated manifest.
     *
     * @param implementationVersion the implementationVersion of extension in generated manifest
     */
    public void setImplementationVersion( final String implementationVersion )
    {
        m_implementationVersion = new DeweyDecimal( implementationVersion );
    }

    /**
     * Set the implementationURL of extension in generated manifest.
     *
     * @param implementationURL the implementationURL of extension in generated manifest
     */
    public void setImplementationURL( final String implementationURL )
    {
        m_implementationURL = implementationURL;
    }

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
     * Adds a set of files about which library data will be displayed.
     *
     * @param fileSet a set of files about which library data will be displayed.
     */
    public void addDepends( final LibFileSet fileSet )
    {
        m_dependencies.add( fileSet );
    }

    /**
     * Adds a set of files about which library data will be displayed.
     *
     * @param fileSet a set of files about which library data will be displayed.
     */
    public void addOptional( final LibFileSet fileSet )
    {
        m_optionals.addElement( fileSet );
    }

    public void execute()
        throws TaskException
    {
        validate();

        final Manifest manifest = new Manifest();
        final Attributes attributes = manifest.getMainAttributes();

        attributes.put( Attributes.Name.MANIFEST_VERSION, MANIFEST_VERSION );
        attributes.putValue( "Created-By", Constants.BUILD_DESCRIPTION );

        appendExtensionData( attributes );

        final String extensionKey = Extension.EXTENSION_LIST.toString();
        appendLibrarys( attributes, extensionKey, m_dependencies );

        final String optionalExtensionKey =
            "Optional-" + Extension.EXTENSION_LIST.toString();
        appendLibrarys( attributes, optionalExtensionKey, m_optionals );

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
     * Append specified librarys extension data to specified attributes.
     * Use the extensionKey to list the extensions, usually "Extension-List:"
     * for required dependencies and "Optional-Extension-List:" for optional
     * dependencies. NOTE: "Optional" dependencies are not part of the
     * specification.
     *
     * @param attributes the attributes to add extensions to
     * @param extensionKey the key under which to add extensions
     * @param librarys the filesets containing librarys
     * @throws TaskException if an error occurs
     */
    private void appendLibrarys( final Attributes attributes,
                                 final String extensionKey,
                                 final Vector librarys )
        throws TaskException
    {
        if( !librarys.isEmpty() )
        {
            final Extension[] extensions = getExtensions( librarys );
            final String[] names = getNames( extensions );
            final StringBuffer sb = new StringBuffer();
            for( int i = 0; i < names.length; i++ )
            {
                sb.append( names[ i ] );
                sb.append( ' ' );
            }

            //Extension-List: javahelp java3d
            attributes.putValue( extensionKey, sb.toString() );

            for( int i = 0; i < names.length; i++ )
            {
                appendDependency( attributes,
                                  names[ i ],
                                  extensions[ i ] );
            }
        }
    }

    /**
     * add a extension dependency to manifest.
     * Use specified name as prefix name.
     *
     * @param attributes the attributes of manifest
     * @param name the name to prefix to extension
     * @param extension the extension
     * @throws TaskException if an error occurs
     */
    private void appendDependency( final Attributes attributes,
                                   final String name,
                                   final Extension extension )
        throws TaskException
    {
        final String prefix = name + "-";
        attributes.putValue( prefix + Extension.EXTENSION_NAME,
                             extension.getExtensionName() );

        final String specificationVendor = extension.getSpecificationVendor();
        if( null != specificationVendor )
        {
            attributes.putValue( prefix + Extension.SPECIFICATION_VENDOR,
                                 specificationVendor );
        }

        final DeweyDecimal specificationVersion = extension.getSpecificationVersion();
        if( null != specificationVersion )
        {
            attributes.putValue( prefix + Extension.SPECIFICATION_VERSION,
                                 specificationVersion.toString() );
        }

        final String implementationVendorID = extension.getImplementationVendorID();
        if( null != implementationVendorID )
        {
            attributes.putValue( prefix + Extension.IMPLEMENTATION_VENDOR_ID,
                                 implementationVendorID );
        }

        final String implementationVendor = extension.getImplementationVendor();
        if( null != implementationVendor )
        {
            attributes.putValue( prefix + Extension.IMPLEMENTATION_VENDOR,
                                 implementationVendor );
        }

        final DeweyDecimal implementationVersion = extension.getImplementationVersion();
        if( null != implementationVersion )
        {
            attributes.putValue( prefix + Extension.IMPLEMENTATION_VERSION,
                                 implementationVersion.toString() );
        }

        final String implementationURL = extension.getImplementationURL();
        if( null != implementationURL )
        {
            attributes.putValue( prefix + Extension.IMPLEMENTATION_URL,
                                 implementationURL );
        }
    }

    /**
     * Create an array of names that can be used for dependencies
     * list for the specified extensions.
     *
     * @param extensions the extensions
     * @return the names to use for extensions
     */
    private String[] getNames( final Extension[] extensions )
    {
        final String[] results = new String[ extensions.length ];
        for( int i = 0; i < results.length; i++ )
        {
            //Perhaps generate mangled names based on extension in future
            results[ i ] = "lib" + i;
        }

        return results;
    }

    /**
     * Retrieve extensions from the specified librarys.
     *
     * @param librarys the filesets for librarys
     * @return the extensions contained in librarys
     * @throws TaskException if failing to scan librarys
     */
    private Extension[] getExtensions( final Vector librarys )
        throws TaskException
    {
        final ArrayList extensions = new ArrayList();
        final Iterator iterator = librarys.iterator();
        while( iterator.hasNext() )
        {
            final FileSet fileSet = (FileSet)iterator.next();
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
    private void loadExtensions( final File file,
                                 final ArrayList extensions )
        throws TaskException
    {
        try
        {
            final JarFile jarFile = new JarFile( file );
            final Extension[] extension =
                Extension.getAvailable( jarFile.getManifest() );
            for( int j = 0; j < extension.length; j++ )
            {
                extensions.add( extension[ j ] );
            }
        }
        catch( final Exception e )
        {
            throw new TaskException( e.getMessage(), e );
        }
    }

    /**
     * Add extension data into specified attributes.
     *
     * @param attributes the attributes to add extension data to
     */
    private void appendExtensionData( final Attributes attributes )
    {
        attributes.put( Extension.EXTENSION_NAME, m_extensionName );
        if( null != m_specificationVendor )
        {
            attributes.put( Extension.SPECIFICATION_VENDOR,
                            m_specificationVendor );
        }
        if( null != m_specificationVersion )
        {
            attributes.put( Extension.SPECIFICATION_VERSION,
                            m_specificationVersion.toString() );
        }
        if( null != m_implementationVendorID )
        {
            attributes.put( Extension.IMPLEMENTATION_VENDOR_ID,
                            m_implementationVendorID );
        }
        if( null != m_implementationVendor )
        {
            attributes.put( Extension.IMPLEMENTATION_VENDOR,
                            m_implementationVendor );
        }
        if( null != m_implementationVersion )
        {
            attributes.put( Extension.IMPLEMENTATION_VERSION,
                            m_implementationVersion.toString() );
        }
        if( null != m_implementationURL )
        {
            attributes.put( Extension.IMPLEMENTATION_URL,
                            m_implementationURL );
        }
    }
}
