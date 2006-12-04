/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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
package org.apache.tools.ant.taskdefs;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;
import java.util.Vector;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.filters.util.ChainReaderHelper;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.JavaResource;
import org.apache.tools.ant.util.FileUtils;

/**
 * Load a file's contents as Ant properties.
 *
 * @since Ant 1.5
 * @ant.task category="utility"
 */
public class LoadProperties extends Task {

    /**
     * Source resource.
     */
    private Resource src = null;

    /**
     * Holds filterchains
     */
    private final Vector filterChains = new Vector();

    /**
     * Encoding to use for input; defaults to the platform's default encoding.
     */
    private String encoding = null;

    /**
     * Set the file to load.
     *
     * @param srcFile The new SrcFile value
     */
    public final void setSrcFile(final File srcFile) {
        addConfigured(new FileResource(srcFile));
    }

    /**
     * Set the resource name of a property file to load.
     *
     * @param resource resource on classpath
     */
    public void setResource(String resource) {
        assertSrcIsJavaResource();
        ((JavaResource) src).setName(resource);
    }

    /**
     * Encoding to use for input, defaults to the platform's default
     * encoding. <p>
     *
     * For a list of possible values see
     * <a href="http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html">
     * http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html
     * </a>.</p>
     *
     * @param encoding The new Encoding value
     */
    public final void setEncoding(final String encoding) {
        this.encoding = encoding;
    }

    /**
     * Set the classpath to use when looking up a resource.
     * @param classpath to add to any existing classpath
     */
    public void setClasspath(Path classpath) {
        assertSrcIsJavaResource();
        ((JavaResource) src).setClasspath(classpath);
    }

    /**
     * Add a classpath to use when looking up a resource.
     * @return The classpath to be configured
     */
    public Path createClasspath() {
        assertSrcIsJavaResource();
        return ((JavaResource) src).createClasspath();
    }

    /**
     * Set the classpath to use when looking up a resource,
     * given as reference to a &lt;path&gt; defined elsewhere
     * @param r The reference value
     */
    public void setClasspathRef(Reference r) {
        assertSrcIsJavaResource();
        ((JavaResource) src).setClasspathRef(r);
    }

    /**
     * get the classpath used by this <code>LoadProperties</code>.
     * @return The classpath
     */
    public Path getClasspath() {
        assertSrcIsJavaResource();
        return ((JavaResource) src).getClasspath();
    }

    /**
     * load Ant properties from the source file or resource
     *
     * @exception BuildException if something goes wrong with the build
     */
    public final void execute() throws BuildException {
        //validation
        if (src == null) {
            throw new BuildException("A source resource is required.");
        }
        if (!src.isExists()) {
            if (src instanceof JavaResource) {
                // dreaded backwards compatibility
                log("Unable to find resource " + src, Project.MSG_WARN);
                return;
            }
            throw new BuildException("Source resource does not exist: " + src);
        }

        BufferedInputStream bis = null;
        Reader instream = null;
        ByteArrayInputStream tis = null;

        try {
            bis = new BufferedInputStream(src.getInputStream());
            if (encoding == null) {
                instream = new InputStreamReader(bis);
            } else {
                instream = new InputStreamReader(bis, encoding);
            }

            ChainReaderHelper crh = new ChainReaderHelper();
            crh.setPrimaryReader(instream);
            crh.setFilterChains(filterChains);
            crh.setProject(getProject());
            instream = crh.getAssembledReader();

            String text = crh.readFully(instream);

            if (text != null) {
                if (!text.endsWith("\n")) {
                    text = text + "\n";
                }

                if (encoding == null) {
                    tis = new ByteArrayInputStream(text.getBytes());
                } else {
                    tis = new ByteArrayInputStream(text.getBytes(encoding));
                }
                final Properties props = new Properties();
                props.load(tis);

                Property propertyTask = new Property();
                propertyTask.bindToOwner(this);
                propertyTask.addProperties(props);
            }

        } catch (final IOException ioe) {
            final String message = "Unable to load file: " + ioe.toString();
            throw new BuildException(message, ioe, getLocation());
        } finally {
            FileUtils.close(bis);
            FileUtils.close(tis);
        }
    }

    /**
     * Adds a FilterChain.
     * @param filter the filter to add
     */
    public final void addFilterChain(FilterChain filter) {
        filterChains.addElement(filter);
    }

    /**
     * Set the source resource.
     * @param a the resource to load as a single element Resource collection.
     * @since Ant 1.7
     */
    public void addConfigured(ResourceCollection a) {
        if (src != null) {
            throw new BuildException("only a single source is supported");
        }
        if (a.size() != 1) {
            throw new BuildException("only single argument resource collections"
                                     + " are supported");
        }
        src = (Resource) a.iterator().next();
    }

    private void assertSrcIsJavaResource() {
        if (src == null) {
            src = new JavaResource();
            src.setProject(getProject());
        } else if (!(src instanceof JavaResource)) {
            throw new BuildException("expected a java resource as source");
        }
    }
}
