/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.tools.ant.taskdefs.optional.extension;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;

/**
 * Generates a manifest that declares all the dependencies.
 * The dependencies are determined by looking in the
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
    extends Task
{
    /**
     * Version of manifest spec that task generates.
     */
    private static final String MANIFEST_VERSION = "1.0";

    /**
     * "Created-By" string used when creating manifest.
     */
    private static final String CREATED_BY = "Created-By";

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
    public void addConfiguredExtension( final ExtensionAdapter extensionAdapter )
        throws BuildException
    {
        if( null != m_extension )
        {
            final String message =
                "Can not have multiple extensions defined in one library.";
            throw new BuildException( message );
        }
        else
        {
            m_extension = extensionAdapter.toExtension();
        }
    }

    /**
     * Adds a set of extensions that this library requires.
     *
     * @param extensionSet a set of extensions that this library requires.
     */
    public void addConfiguredDepends( final ExtensionSet extensionSet )
    {
        m_dependencies.add( extensionSet );
    }

    /**
     * Adds a set of extensions that this library optionally requires.
     *
     * @param extensionSet a set of extensions that this library optionally requires.
     */
    public void addConfiguredOptions( final ExtensionSet extensionSet )
    {
        m_optionals.add( extensionSet );
    }

    /**
     * Adds an attribute that is to be put in main section of manifest.
     *
     * @param attribute an attribute that is to be put in main section of manifest.
     */
    public void addConfiguredAttribute( final ExtraAttribute attribute )
    {
        m_extraAttributes.add( attribute );
    }

    public void execute()
        throws BuildException
    {
        validate();

        final Manifest manifest = new Manifest();
        final Attributes attributes = manifest.getMainAttributes();

        attributes.put( Attributes.Name.MANIFEST_VERSION, MANIFEST_VERSION );
        final String createdBy = "Apache Ant " + getProject().getProperty( "ant.version" );
        attributes.putValue( CREATED_BY, createdBy );

        appendExtraAttributes( attributes );

        if( null != m_extension )
        {
            Extension.addExtension( m_extension, attributes );
        }

        //Add all the dependency data to manifest for dependencies
        final ArrayList depends = toExtensions( m_dependencies );
        appendExtensionList( attributes,
                             Extension.EXTENSION_LIST,
                             "lib",
                             depends.size() );
        appendLibraryList( attributes, "lib", depends );

        //Add all the dependency data to manifest for "optional"
        //dependencies
        final ArrayList option = toExtensions( m_optionals );
        appendExtensionList( attributes,
                             Extension.OPTIONAL_EXTENSION_LIST,
                             "opt",
                             option.size() );
        appendLibraryList( attributes, "opt", option );

        try
        {
            final String message = "Generating manifest " + m_destfile.getAbsoluteFile();
            log( message, Project.MSG_INFO );
            writeManifest( manifest );
        }
        catch( final IOException ioe )
        {
            throw new BuildException( ioe.getMessage(), ioe );
        }
    }

    /**
     * Validate the tasks parameters.
     *
     * @throws BuildException if invalid parameters found
     */
    private void validate()
        throws BuildException
    {
        if( null == m_destfile )
        {
            final String message = "Destfile attribute not specified.";
            throw new BuildException( message );
        }
        if( m_destfile.exists() && !m_destfile.isFile() )
        {
            final String message = m_destfile + " is not a file.";
            throw new BuildException( message );
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
            if( null != output )
            {
                try
                {
                    output.close();
                }
                catch( IOException e )
                {
                }
            }
        }
    }

    /**
     * Append specified extensions to specified attributes.
     * Use the extensionKey to list the extensions, usually "Extension-List:"
     * for required dependencies and "Optional-Extension-List:" for optional
     * dependencies. NOTE: "Optional" dependencies are not part of the
     * specification.
     *
     * @param attributes the attributes to add extensions to
     * @param extensions the list of extensions
     * @throws BuildException if an error occurs
     */
    private void appendLibraryList( final Attributes attributes,
                                    final String listPrefix,
                                    final ArrayList extensions )
        throws BuildException
    {
        final int size = extensions.size();
        for( int i = 0; i < size; i++ )
        {
            final Extension extension = (Extension)extensions.get( i );
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
     * Convert a list of ExtensionSet objects to extensions.
     *
     * @param extensionSets the list of ExtensionSets to add to list
     * @throws BuildException if an error occurs
     */
    private ArrayList toExtensions( final ArrayList extensionSets )
        throws BuildException
    {
        final ArrayList results = new ArrayList();

        final int size = extensionSets.size();
        for( int i = 0; i < size; i++ )
        {
            final ExtensionSet set = (ExtensionSet)extensionSets.get( i );
            final Extension[] extensions = set.toExtensions( getProject() );
            for( int j = 0; j < extensions.length; j++ )
            {
                results.add( extensions[ j ] );
            }
        }

        return results;
    }
}
