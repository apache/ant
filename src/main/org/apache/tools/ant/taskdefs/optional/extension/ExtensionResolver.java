/*
 * Copyright  2002,2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.taskdefs.optional.extension;

import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Interface to locate a File that satisfies extension.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:jeff@socialchange.net.au">Jeff Turner</>
 * @version $Revision$ $Date$
 */
public interface ExtensionResolver {
    /**
     * Attempt to locate File that satisfies
     * extension via resolver.
     *
     * @param extension the extension
     * @param project the Ant project instance
     * @return the File satisfying extension, null
     *         if can not resolve extension
     * @throws BuildException if error occurs attempting to
     *         resolve extension
     */
    File resolve(Extension extension, Project project)
        throws BuildException;
}
