/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.extensions;

import org.apache.myrmidon.framework.FileSet;

/**
 * LibFileSet represents a fileset containing libraries.
 * Asociated with the libraries is data pertaining to
 * how they are to be handled when building manifests.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class LibFileSet
    extends FileSet
{
    /**
     * Flag indicating whether should include the
     * "Implementation-URL" attribute in manifest.
     * Defaults to false.
     */
    private boolean m_includeURL;

    /**
     * Flag indicating whether should include the
     * "Implementation-*" attributes in manifest.
     * Defaults to false.
     */
    private boolean m_includeImpl;

    /**
     * String that is the base URL for the librarys
     * when constructing the "Implementation-URL"
     * attribute. For instance setting the base to
     * "http://jakarta.apache.org/avalon/libs/" and then
     * including the library "excalibur-cli-1.0.jar" in the
     * fileset will result in the "Implementation-URL" attribute
     * being set to "http://jakarta.apache.org/avalon/libs/excalibur-cli-1.0.jar"
     *
     * Note this is only used if the library does not define
     * "Implementation-URL" itself.
     *
     * Note that this also implies includeURL=true
     */
    private String m_urlBase;

    /**
     * Flag indicating whether should include the
     * "Implementation-URL" attribute in manifest.
     * Defaults to false.
     *
     * @param includeURL the flag
     * @see #m_includeURL
     */
    public void setIncludeURL( boolean includeURL )
    {
        m_includeURL = includeURL;
    }

    /**
     * Flag indicating whether should include the
     * "Implementation-*" attributes in manifest.
     * Defaults to false.
     *
     * @param includeImpl the flag
     * @see #m_includeImpl
     */
    public void setIncludeImpl( boolean includeImpl )
    {
        m_includeImpl = includeImpl;
    }

    /**
     * Set the url base for fileset.
     *
     * @param urlBase the base url
     * @see #m_urlBase
     */
    public void setUrlBase( String urlBase )
    {
        m_urlBase = urlBase;
    }

    /**
     * Get the includeURL flag.
     *
     * @return the includeURL flag.
     */
    boolean isIncludeURL()
    {
        return m_includeURL;
    }

    /**
     * Get the includeImpl flag.
     *
     * @return the includeImpl flag.
     */
    boolean isIncludeImpl()
    {
        return m_includeImpl;
    }

    /**
     * Get the urlbase.
     *
     * @return the urlbase.
     */
    String getUrlBase()
    {
        return m_urlBase;
    }
}
