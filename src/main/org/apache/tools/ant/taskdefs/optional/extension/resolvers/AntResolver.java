/*
 * Copyright  2002-2004 The Apache Software Foundation
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
package org.apache.tools.ant.taskdefs.optional.extension.resolvers;

import java.io.File;
import java.io.IOException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Ant;
import org.apache.tools.ant.taskdefs.optional.extension.Extension;
import org.apache.tools.ant.taskdefs.optional.extension.ExtensionResolver;

/**
 * Resolver that just returns s specified location.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:jeff@socialchange.net.au">Jeff Turner</>
 * @version $Revision$ $Date$
 */
public class AntResolver
    implements ExtensionResolver {
    private File m_antfile;
    private File m_destfile;
    private String m_target;

    public void setAntfile(File antfile) {
        m_antfile = antfile;
    }

    public void setDestfile(File destfile) {
        m_destfile = destfile;
    }

    public void setTarget(final String target) {
        m_target = target;
    }

    public File resolve(final Extension extension,
                         final Project project)
        throws BuildException {
        validate();

        final Ant ant = (Ant) project.createTask("ant");
        ant.setInheritAll(false);
        ant.setAntfile(m_antfile.getName());

        try {
            final File dir =
                m_antfile.getParentFile().getCanonicalFile();
            ant.setDir(dir);
        } catch (final IOException ioe) {
            throw new BuildException(ioe.getMessage(), ioe);
        }

        if (null != m_target) {
            ant.setTarget(m_target);
        }

        ant.execute();

        return m_destfile;
    }

    private void validate() {
        if (null == m_antfile) {
            final String message = "Must specify Buildfile";
            throw new BuildException(message);
        }

        if (null == m_destfile) {
            final String message = "Must specify destination file";
            throw new BuildException(message);
        }
    }

    public String toString() {
        return "Ant[" + m_antfile + "==>" + m_destfile + "]";
    }
}
