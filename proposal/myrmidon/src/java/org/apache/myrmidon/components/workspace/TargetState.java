/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.workspace;

/**
 * An enumerated type that represents the dependency traversal state of a
 * target.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
final class TargetState
{
    private TargetState()
    {
    }

    /** Target has not been started. */
    public final static TargetState NOT_STARTED = new TargetState();

    /**
     * Target has been started, and the dependencies of the target are being
     * traversed.
     */
    public final static TargetState TRAVERSING = new TargetState();

    /** Target has been completed. */
    public final static TargetState FINISHED = new TargetState();
}
