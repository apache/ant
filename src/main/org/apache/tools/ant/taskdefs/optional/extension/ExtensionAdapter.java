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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Reference;

/**
 * Simple class that represents an Extension and conforms to Ants
 * patterns.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 * @ant.data-type name="extension"
 */
public class ExtensionAdapter
    extends DataType
{
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
     * Set the name of extension.
     *
     * @param extensionName the name of extension
     */
    public void setExtensionName( final String extensionName )
    {
        verifyNotAReference();
        m_extensionName = extensionName;
    }

    /**
     * Set the specificationVersion of extension.
     *
     * @param specificationVersion the specificationVersion of extension
     */
    public void setSpecificationVersion( final String specificationVersion )
    {
        verifyNotAReference();
        m_specificationVersion = new DeweyDecimal( specificationVersion );
    }

    /**
     * Set the specificationVendor of extension.
     *
     * @param specificationVendor the specificationVendor of extension
     */
    public void setSpecificationVendor( final String specificationVendor )
    {
        verifyNotAReference();
        m_specificationVendor = specificationVendor;
    }

    /**
     * Set the implementationVendorID of extension.
     *
     * @param implementationVendorID the implementationVendorID of extension
     */
    public void setImplementationVendorId( final String implementationVendorID )
    {
        verifyNotAReference();
        m_implementationVendorID = implementationVendorID;
    }

    /**
     * Set the implementationVendor of extension.
     *
     * @param implementationVendor the implementationVendor of extension
     */
    public void setImplementationVendor( final String implementationVendor )
    {
        verifyNotAReference();
        m_implementationVendor = implementationVendor;
    }

    /**
     * Set the implementationVersion of extension.
     *
     * @param implementationVersion the implementationVersion of extension
     */
    public void setImplementationVersion( final String implementationVersion )
    {
        verifyNotAReference();
        m_implementationVersion = new DeweyDecimal( implementationVersion );
    }

    /**
     * Set the implementationURL of extension.
     *
     * @param implementationURL the implementationURL of extension
     */
    public void setImplementationUrl( final String implementationURL )
    {
        verifyNotAReference();
        m_implementationURL = implementationURL;
    }

    /**
     * Makes this instance in effect a reference to another ExtensionAdapter
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
        if( null != m_extensionName ||
            null != m_specificationVersion ||
            null != m_specificationVendor ||
            null != m_implementationVersion ||
            null != m_implementationVendorID ||
            null != m_implementationVendor ||
            null != m_implementationURL )
        {
            throw tooManyAttributes();
        }
        // change this to get the objects from the other reference
        Object o = reference.getReferencedObject( getProject() );
        if( o instanceof ExtensionAdapter )
        {
            final ExtensionAdapter other = (ExtensionAdapter)o;
            m_extensionName = other.m_extensionName;
            m_specificationVersion = other.m_specificationVersion;
            m_specificationVendor = other.m_specificationVendor;
            m_implementationVersion = other.m_implementationVersion;
            m_implementationVendorID = other.m_implementationVendorID;
            m_implementationVendor = other.m_implementationVendor;
            m_implementationURL = other.m_implementationURL;
        }
        else
        {
            final String message =
                reference.getRefId() + " doesn\'t refer to a Extension";
            throw new BuildException( message );
        }

        super.setRefid( reference );
    }

    private void verifyNotAReference()
        throws BuildException
    {
        if( isReference() )
        {
            throw tooManyAttributes();
        }
    }

    /**
     * Convert this adpater object into an extension object.
     *
     * @return the extension object
     */
    Extension toExtension()
        throws BuildException
    {
        if( null == m_extensionName )
        {
            final String message = "Extension is missing name.";
            throw new BuildException( message );
        }

        String specificationVersion = null;
        if( null != m_specificationVersion )
        {
            specificationVersion = m_specificationVersion.toString();
        }
        String implementationVersion = null;
        if( null != m_implementationVersion )
        {
            implementationVersion = m_implementationVersion.toString();
        }
        return new Extension( m_extensionName,
                              specificationVersion,
                              m_specificationVendor,
                              implementationVersion,
                              m_implementationVendorID,
                              m_implementationVendor,
                              m_implementationURL );
    }

    public String toString()
    {
        return "{" + toExtension().toString() + "}";
    }
}
