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
import java.util.Iterator;
import java.util.Vector;
import java.util.jar.Manifest;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Checks whether an extension is present in a fileset or an extensionSet.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @ant.task name="jarlib-available"
 */
public class JarLibAvailableTask
    extends Task
{
    /**
     * The library to display information about.
     */
    private File m_file;

    /**
     * Filesets specifying all the librarys
     * to display information about.
     */
    private final Vector m_extensionSets = new Vector();

    /**
     * The name of the property to set if extension is available.
     */
    private String m_property;

    /**
     * The extension that is required.
     */
    private ExtensionAdapter m_extension;

    /**
     * The name of property to set if extensions are available.
     *
     * @param property The name of property to set if extensions is available.
     */
    public void setProperty( final String property )
    {
        m_property = property;
    }

    /**
     * The JAR library to check.
     *
     * @param file The jar library to check.
     */
    public void setFile( final File file )
    {
        m_file = file;
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
                "search for multiple times.";
            throw new BuildException( message );
        }
        m_extension = extension;
    }

    /**
     * Adds a set of extensions to search in.
     *
     * @param extensionSet a set of extensions to search in.
     */
    public void addConfiguredExtensionSet( final ExtensionSet extensionSet )
    {
        m_extensionSets.addElement( extensionSet );
    }

    public void execute()
        throws BuildException
    {
        validate();

        final Extension test = m_extension.toExtension();

        // Check if list of files to check has been specified
        if( !m_extensionSets.isEmpty() )
        {
            final Iterator iterator = m_extensionSets.iterator();
            while( iterator.hasNext() )
            {
                final ExtensionSet extensionSet = (ExtensionSet)iterator.next();
                final Extension[] extensions =
                    extensionSet.toExtensions( getProject() );
                for( int i = 0; i < extensions.length; i++ )
                {
                    final Extension extension = extensions[ i ];
                    if( extension.isCompatibleWith( test ) )
                    {
                        getProject().setNewProperty( m_property, "true" );
                    }
                }
            }
        }
        else
        {
            final Manifest manifest = ExtensionUtil.getManifest( m_file );
            final Extension[] extensions = Extension.getAvailable( manifest );
            for( int i = 0; i < extensions.length; i++ )
            {
                final Extension extension = extensions[ i ];
                if( extension.isCompatibleWith( test ) )
                {
                    getProject().setNewProperty( m_property, "true" );
                }
            }
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
        if( null == m_extension )
        {
            final String message = "Extension element must be specified.";
            throw new BuildException( message );
        }

        if( null == m_file && m_extensionSets.isEmpty() )
        {
            final String message = "File attribute not specified.";
            throw new BuildException( message );
        }
        if( null != m_file && !m_file.exists() )
        {
            final String message = "File '" + m_file + "' does not exist.";
            throw new BuildException( message );
        }
        if( null != m_file && !m_file.isFile() )
        {
            final String message = "\'" + m_file + "\' is not a file.";
            throw new BuildException( message );
        }
    }
}
