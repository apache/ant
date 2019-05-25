/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
 */
public class URLResolver implements ExtensionResolver {
    private File destfile;
    private File destdir;
    private URL url;

    /**
     * Sets the URL
     * @param url the url
     */
    public void setUrl(final URL url) {
        this.url = url;
    }

    /**
     * Sets the destination file
     * @param destfile the destination file
     */
    public void setDestfile(final File destfile) {
        this.destfile = destfile;
    }

    /**
     * Sets the destination directory
     * @param destdir the destination directory
     */
    public void setDestdir(final File destdir) {
        this.destdir = destdir;
    }

    /**
     * Returns the file resolved from URL and directory
     * @param extension the extension
     * @param project the project
     * @return file the file resolved
     * @throws BuildException if the URL is invalid
     */
    @Override
    public File resolve(final Extension extension,
                         final Project project) throws BuildException {
        validate();

        final File file = getDest();

        final Get get = new Get();
        get.setProject(project);
        get.setDest(file);
        get.setSrc(url);
        get.execute();

        return file;
    }

    /*
     * Gets the destination file
     */
    private File getDest() {
        File result;
        if (null != destfile) {
            result = destfile;
        } else {
            final String file = url.getFile();
            String filename;
            if (null == file || file.length() <= 1) {
                filename = "default.file";
            } else {
                int index = file.lastIndexOf('/');
                if (-1 == index) {
                    index = 0;
                }
                filename = file.substring(index);
            }
            result = new File(destdir, filename);
        }
        return result;
    }

    /*
     * Validates URL
     */
    private void validate() {
        if (null == url) {
            throw new BuildException("Must specify URL");
        }
        if (null == destdir && null == destfile) {
            throw new BuildException(
                "Must specify destination file or directory");
        }
        if (null != destdir && null != destfile) {
            throw new BuildException(
                "Must not specify both destination file or directory");
        }
    }

    /**
     * Returns a string representation of the URL
     * @return the string representation
     */
    @Override
    public String toString() {
        return "URL[" + url + "]";
    }
}
