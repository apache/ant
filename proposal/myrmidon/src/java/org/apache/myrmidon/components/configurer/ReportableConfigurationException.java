/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer;

import org.apache.avalon.framework.configuration.ConfigurationException;

/**
 * A marker exception.
 *
 * TODO - this should extend ConfigurationException, except it is final.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
class ReportableConfigurationException
    extends Exception
{
    private ConfigurationException m_cause;

    public ReportableConfigurationException( String s )
    {
        m_cause = new ConfigurationException( s );
    }

    public ReportableConfigurationException( String s, Throwable throwable )
    {
        m_cause = new ConfigurationException( s, throwable );
    }

    public ConfigurationException getCause()
    {
        return m_cause;
    }
}
