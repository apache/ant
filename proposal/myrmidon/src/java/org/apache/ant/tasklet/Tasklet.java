/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasklet;

import org.apache.avalon.Component;
import org.apache.avalon.Contextualizable;

/**
 * This represents the individual tasks.
 * Particular instances can also implement Initializable
 * and/or Disposable, in which case init()/dispose() will 
 * be called at appropriate time.
 * The task can also implement Composer in which case required 
 * facilities will be passed via a ComponentManager. The actual
 * facilties is determined by particular task engine but will usually
 * include ProjectEngine and TaskEngine.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface Tasklet
    extends Component, Contextualizable, Runnable
{
}
