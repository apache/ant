/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.condition;

import org.apache.myrmidon.api.TaskException;

/**
 * Interface for conditions to use inside the &lt;condition&gt; task.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version $Revision$
 */
public interface Condition
{
    /**
     * Is this condition true?
     *
     * @return Description of the Returned Value
     * @exception BuildException Description of Exception
     */
    boolean eval()
        throws TaskException;
}

