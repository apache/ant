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

import java.util.ArrayList;
import java.util.Arrays;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.FileSet;

/**
 * The Extension set lists a set of "Optional Packages" /
 * "Extensions".
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 * @ant.data-type name="extension-set"
 */
public class ExtensionSet
    extends DataType
{
    /**
     * ExtensionAdapter objects representing extensions.
     */
    private final ArrayList m_extensions = new ArrayList();

    /**
     * Filesets specifying all the extensions wanted.
     */
    private final ArrayList m_extensionsFilesets = new ArrayList();

    /**
     * Adds an extension that this library requires.
     *
     * @param extensionAdapter an extension that this library requires.
     */
    public void addExtension( final ExtensionAdapter extensionAdapter )
    {
        m_extensions.add( extensionAdapter );
    }

    /**
     * Adds a set of files about which extensions data will be extracted.
     *
     * @param fileSet a set of files about which extensions data will be extracted.
     */
    public void addLibfileset( final LibFileSet fileSet )
    {
        m_extensionsFilesets.add( fileSet );
    }

    /**
     * Adds a set of files about which extensions data will be extracted.
     *
     * @param fileSet a set of files about which extensions data will be extracted.
     */
    public void addFileset( final FileSet fileSet )
    {
        m_extensionsFilesets.add( fileSet );
    }

    /**
     * Extract a set of Extension objects from the ExtensionSet.
     *
     * @throws BuildException if an error occurs
     */
    public Extension[] toExtensions( final Project project )
        throws BuildException
    {
        final ArrayList extensions = ExtensionUtil.toExtensions( m_extensions );
        ExtensionUtil.extractExtensions( project, extensions, m_extensionsFilesets );
        return (Extension[])extensions.toArray( new Extension[ extensions.size() ] );
    }

    /**
     * Makes this instance in effect a reference to another ExtensionSet
     * instance.
     *
     * <p>You must not set another attribute or nest elements inside
     * this element if you make it a reference.</p>
     *
     * @param reference the reference to which this instance is associated
     * @exception BuildException if this instance already has been configured.
     */
    public void setRefid( final Reference reference )
        throws BuildException
    {
        if( !m_extensions.isEmpty() ||
            !m_extensionsFilesets.isEmpty() )
        {
            throw tooManyAttributes();
        }
        // change this to get the objects from the other reference
        final Object object =
            reference.getReferencedObject( getProject() );
        if( object instanceof ExtensionSet )
        {
            final ExtensionSet other = (ExtensionSet)object;
            m_extensions.addAll( other.m_extensions );
            m_extensionsFilesets.addAll( other.m_extensionsFilesets );
        }
        else
        {
            final String message =
                reference.getRefId() + " doesn\'t refer to a ExtensionSet";
            throw new BuildException( message );
        }

        super.setRefid( reference );
    }

    public String toString()
    {
        return "ExtensionSet" + Arrays.asList( toExtensions( getProject() ) );
    }
}
