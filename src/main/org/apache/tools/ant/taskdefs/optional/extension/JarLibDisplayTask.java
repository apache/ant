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
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * Displays the "Optional Package" and "Package Specification" information
 * contained within the specified JARs.
 *
 * <p>Prior to JDK1.3, an "Optional Package" was known as an Extension.
 * The specification for this mechanism is available in the JDK1.3
 * documentation in the directory
 * $JDK_HOME/docs/guide/extensions/versioning.html. Alternatively it is
 * available online at <a href="http://java.sun.com/j2se/1.3/docs/guide/extensions/versioning.html">
 * http://java.sun.com/j2se/1.3/docs/guide/extensions/versioning.html</a>.</p>
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @ant.task name="jarlib-display"
 */
public class JarLibDisplayTask
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
    private final Vector m_filesets = new Vector();

    /**
     * The JAR library to display information for.
     *
     * @param file The jar library to display information for.
     */
    public void setFile( final File file )
    {
        m_file = file;
    }

    /**
     * Adds a set of files about which library data will be displayed.
     *
     * @param fileSet a set of files about which library data will be displayed.
     */
    public void addFileset( final FileSet fileSet )
    {
        m_filesets.addElement( fileSet );
    }

    public void execute()
        throws BuildException
    {
        validate();

        final LibraryDisplayer displayer = new LibraryDisplayer();
        // Check if list of files to check has been specified
        if( !m_filesets.isEmpty() )
        {
            final Iterator iterator = m_filesets.iterator();
            while( iterator.hasNext() )
            {
                final FileSet fileSet = (FileSet)iterator.next();
                final DirectoryScanner scanner = fileSet.getDirectoryScanner( getProject() );
                final File basedir = scanner.getBasedir();
                final String[] files = scanner.getIncludedFiles();
                for( int i = 0; i < files.length; i++ )
                {
                    final File file = new File( basedir, files[ i ] );
                    displayer.displayLibrary( file );
                }
            }
        }
        else
        {
            displayer.displayLibrary( m_file );
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
        if( null == m_file && m_filesets.isEmpty() )
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
