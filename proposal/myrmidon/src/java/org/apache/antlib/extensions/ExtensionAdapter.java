/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.extensions;

import org.apache.avalon.excalibur.extension.DeweyDecimal;
import org.apache.avalon.excalibur.extension.Extension;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.TaskException;

/**
 * Simple class that represents an Extension and conforms to Ants
 * patterns.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class ExtensionAdapter
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( ExtensionAdapter.class );

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
        m_extensionName = extensionName;
    }

    /**
     * Set the specificationVersion of extension.
     *
     * @param specificationVersion the specificationVersion of extension
     */
    public void setSpecificationVersion( final String specificationVersion )
    {
        m_specificationVersion = new DeweyDecimal( specificationVersion );
    }

    /**
     * Set the specificationVendor of extension.
     *
     * @param specificationVendor the specificationVendor of extension
     */
    public void setSpecificationVendor( final String specificationVendor )
    {
        m_specificationVendor = specificationVendor;
    }

    /**
     * Set the implementationVendorID of extension.
     *
     * @param implementationVendorID the implementationVendorID of extension
     */
    public void setImplementationVendorID( final String implementationVendorID )
    {
        m_implementationVendorID = implementationVendorID;
    }

    /**
     * Set the implementationVendor of extension.
     *
     * @param implementationVendor the implementationVendor of extension
     */
    public void setImplementationVendor( final String implementationVendor )
    {
        m_implementationVendor = implementationVendor;
    }

    /**
     * Set the implementationVersion of extension.
     *
     * @param implementationVersion the implementationVersion of extension
     */
    public void setImplementationVersion( final String implementationVersion )
    {
        m_implementationVersion = new DeweyDecimal( implementationVersion );
    }

    /**
     * Set the implementationURL of extension.
     *
     * @param implementationURL the implementationURL of extension
     */
    public void setImplementationURL( final String implementationURL )
    {
        m_implementationURL = implementationURL;
    }

    /**
     * Convert this adpater object into an extension object.
     *
     * @return the extension object
     */
    Extension toExtension()
        throws TaskException
    {
        if( null == m_extensionName )
        {
            final String message = REZ.getString( "extension.noname.error" );
            throw new TaskException( message );
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
}
