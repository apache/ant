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
package org.apache.tools.ant.taskdefs.optional.extension.resolvers;

import java.io.File;
import java.net.URL;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Get;
import org.apache.tools.ant.taskdefs.optional.extension.Extension;
import org.apache.tools.ant.taskdefs.optional.extension.ExtensionResolver;

/**
 * Resolver that just returns s specified location.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:jeff@socialchange.net.au">Jeff Turner</>
 * @version $Revision$ $Date$
 */
public class URLResolver
    implements ExtensionResolver {
    private File m_destfile;
    private File m_destdir;
    private URL m_url;

    public void setUrl(final URL url) {
        m_url = url;
    }

    public void setDestfile(final File destfile) {
        m_destfile = destfile;
    }

    public void setDestdir(final File destdir) {
        m_destdir = destdir;
    }

    public File resolve(final Extension extension,
                         final Project project)
        throws BuildException {
        validate();

        final File file = getDest();

        final Get get = (Get) project.createTask("get");
        get.setDest(file);
        get.setSrc(m_url);
        get.execute();

        return file;
    }

    private File getDest() {
        if (null != m_destfile) {
            return m_destfile;
        } else {
            final String file = m_url.getFile();
            String filename = null;
            if (null == file || file.length() <= 1) {
                filename = "default.file";
            } else {
                int index = file.lastIndexOf('/');
                if (-1 == index) {
                    index = 0;
                }
                filename = file.substring(index);
            }

            return new File(m_destdir, filename);
        }
    }

    private void validate() {
        if (null == m_url) {
            final String message = "Must specify URL";
            throw new BuildException(message);
        }

        if (null == m_destdir && null == m_destfile) {
            final String message = "Must specify destination file or directory";
            throw new BuildException(message);
        } else if (null != m_destdir && null != m_destfile) {
            final String message = "Must not specify both destination file or directory";
            throw new BuildException(message);
        }
    }

    public String toString() {
        return "URL[" + m_url + "]";
    }
}
