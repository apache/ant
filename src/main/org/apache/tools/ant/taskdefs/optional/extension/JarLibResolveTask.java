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
import java.util.ArrayList;
import java.util.jar.Manifest;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.optional.extension.resolvers.LocationResolver;
import org.apache.tools.ant.taskdefs.optional.extension.resolvers.URLResolver;
import org.apache.tools.ant.taskdefs.optional.extension.resolvers.AntResolver;

/**
 * Tries to locate a JAR to satisfy an extension and place
 * location of JAR into property.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:jeff@socialchange.net.au">Jeff Turner</a>
 * @ant.task name="jarlib-resolve"
 */
public class JarLibResolveTask
    extends Task
{
    /**
     * The name of the property in which the location of
     * library is stored.
     */
    private String m_property;

    /**
     * The extension that is required.
     */
    private Extension m_extension;

    /**
     * The set of resolvers to use to attempt to locate library.
     */
    private final ArrayList m_resolvers = new ArrayList();

    /**
     * Flag to indicate that you should check that
     * the librarys resolved actually contain
     * extension and if they don't then raise
     * an exception.
     */
    private boolean m_checkExtension = true;

    /**
     * Flag indicating whether or not you should
     * throw a BuildException if you cannot resolve
     * library.
     */
    private boolean m_failOnError = true;

    /**
     * The name of the property in which the location of
     * library is stored.
     *
     * @param property The name of the property in which the location of
     *                 library is stored.
     */
    public void setProperty( final String property )
    {
        m_property = property;
    }

    /**
     * If true, libraries returned by nested resolvers should be
     * checked to see if they supply extension.
     */
    public void setCheckExtension( final boolean checkExtension )
    {
        m_checkExtension = checkExtension;
    }

    /**
     * If true, failure to locate library should fail build.
     */
    public void setFailOnError( final boolean failOnError )
    {
        m_failOnError = failOnError;
    }

    /**
     * Adds location resolver to look for a library in a location
     * relative to project directory.
     */
    public void addConfiguredLocation( final LocationResolver location )
    {
        m_resolvers.add( location );
    }

    /**
     * Adds a URL resolver to download a library from a URL
     * to a local file.
     */
    public void addConfiguredUrl( final URLResolver url )
    {
        m_resolvers.add( url );
    }

    /**
     * Adds Ant resolver to run an Ant build file to generate a library.
     */
    public void addConfiguredAnt( final AntResolver ant )
    {
        m_resolvers.add( ant );
    }

    /**
     * Set the Extension looking for.
     *
     * @param extension Set the Extension looking for.
     */
    public void addConfiguredExtension( final ExtensionAdapter extension )
    {
        if( null != m_extension )
        {
            final String message = "Can not specify extension to " +
                "resolve multiple times.";
            throw new BuildException( message );
        }
        m_extension = extension.toExtension();
    }

    public void execute()
        throws BuildException
    {
        validate();

        getProject().log( "Resolving extension: " + m_extension,
                          Project.MSG_VERBOSE );

        String candidate =
            getProject().getProperty( m_property );

        if( null != candidate )
        {
            final String message = "Property Already set to: " + candidate;
            if( m_failOnError )
            {
                throw new BuildException( message );
            }
            else
            {
                getProject().log( message, Project.MSG_ERR );
                return;
            }
        }

        final int size = m_resolvers.size();
        for( int i = 0; i < size; i++ )
        {
            final ExtensionResolver resolver =
                (ExtensionResolver)m_resolvers.get( i );

            getProject().log( "Searching for extension using Resolver:" + resolver,
                              Project.MSG_VERBOSE );

            try
            {
                final File file =
                    resolver.resolve( m_extension, getProject() );
                try
                {
                    checkExtension( file );
                    return;
                }
                catch( final BuildException be )
                {
                    final String message =
                        "File " + file + " returned by resolver failed " +
                        "to satisfy extension due to: " + be.getMessage();
                    getProject().log( message, Project.MSG_WARN );
                }
            }
            catch( final BuildException be )
            {
                final String message =
                    "Failed to resolve extension to file " +
                    "using resolver " + resolver + " due to: " + be;
                getProject().log( message, Project.MSG_WARN );
            }
        }

        missingExtension();
    }

    /**
     * Utility method that will throw a {@link BuildException}
     * if {@link #m_failOnError} is true else it just displays
     * a warning.
     */
    private void missingExtension()
    {
        final String message =
            "Unable to resolve extension to a file";
        if( m_failOnError )
        {
            throw new BuildException( message );
        }
        else
        {
            getProject().log( message, Project.MSG_ERR );
        }
    }

    /**
     * Check if specified file satisfies extension.
     * If it does then set the relevent property
     * else throw a BuildException.
     *
     * @param file the candidate library
     * @throws BuildException if library does not satisfy extension
     */
    private void checkExtension( final File file )
    {
        if( !file.exists() )
        {
            final String message =
                "File " + file + " does not exist";
            throw new BuildException( message );
        }
        if( !file.isFile() )
        {
            final String message =
                "File " + file + " is not a file";
            throw new BuildException( message );
        }

        if( !m_checkExtension )
        {
            final String message = "Setting property to " +
                file + " without verifying library satisfies extension";
            getProject().log( message, Project.MSG_VERBOSE );
            setLibraryProperty( file );
        }
        else
        {
            getProject().log( "Checking file " + file +
                              " to see if it satisfies extension",
                              Project.MSG_VERBOSE );
            final Manifest manifest =
                ExtensionUtil.getManifest( file );
            final Extension[] extensions =
                Extension.getAvailable( manifest );
            for( int i = 0; i < extensions.length; i++ )
            {
                final Extension extension = extensions[ i ];
                if( extension.isCompatibleWith( m_extension ) )
                {
                    setLibraryProperty( file );
                    return;
                }
            }

            getProject().log( "File " + file + " skipped as it " +
                              "does not satisfy extension",
                              Project.MSG_VERBOSE );

            final String message =
                "File " + file + " does not satisfy extension";
            throw new BuildException( message );
        }
    }

    /**
     * Utility method to set the appropriate property
     * to indicate that specified file satisfies library
     * requirements.
     *
     * @param file the library
     */
    private void setLibraryProperty( final File file )
    {
        getProject().setNewProperty( m_property,
                                     file.getAbsolutePath() );
    }

    /**
     * Validate the tasks parameters.
     *
     * @throws BuildException if invalid parameters found
     */
    private void validate()
        throws BuildException
    {
        if( null == m_property )
        {
            final String message = "Property attribute must be specified.";
            throw new BuildException( message );
        }

        if( null == m_extension )
        {
            final String message = "Extension element must be specified.";
            throw new BuildException( message );
        }
    }
}
