/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.service.test;

import junit.framework.Assert;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameterizable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;

/**
 * A basic class that asserts that the object is correctly set-up.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class LifecycleValidator
    extends Assert
    implements LogEnabled, Serviceable, Parameterizable, Initializable
{
    private String m_state = STATE_NOT_INIT;

    private final static String STATE_NOT_INIT = "not-prepared";
    private final static String STATE_LOG_ENABLED = "log-enabled";
    private final static String STATE_SERVICED = "serviced";
    private final static String STATE_PARAMETERISED = "parameterised";
    protected final static String STATE_INITIALISED = "initialised";

    public void enableLogging( final Logger logger )
    {
        assertEquals( STATE_NOT_INIT, m_state );
        m_state = STATE_LOG_ENABLED;
    }

    public void service( final ServiceManager serviceManager ) throws ServiceException
    {
        assertEquals( STATE_LOG_ENABLED, m_state );
        m_state = STATE_SERVICED;
    }

    public void parameterize( Parameters parameters ) throws ParameterException
    {
        assertEquals( STATE_SERVICED, m_state );
        m_state = STATE_PARAMETERISED;
    }

    public void initialize() throws Exception
    {
        assertEquals( STATE_PARAMETERISED, m_state );
        m_state = STATE_INITIALISED;
    }

    protected void assertSetup()
    {
        assertEquals( STATE_INITIALISED, m_state );
    }
}
