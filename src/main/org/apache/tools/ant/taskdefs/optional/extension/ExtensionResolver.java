/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.extension;

import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Interface to locate a File that satisfies extension.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public interface ExtensionResolver
{
    /**
     * Attempt to locate File that satisfies
     * extension via resolver.
     *
     * @param extension the extension
     * @return the File satisfying extension, null
     *         if can not resolve extension
     * @throws BuildException if error occurs attempting to
     *         resolve extension
     */
    File resolve( Extension extension, Project project )
        throws BuildException;
}
