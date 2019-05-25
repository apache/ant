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
package org.apache.tools.ant.taskdefs;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.filters.util.ChainReaderHelper;
import org.apache.tools.ant.filters.util.ChainReaderHelper.ChainReader;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.JavaResource;

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
    private final List<FilterChain> filterChains = new Vector<>();

    /**
     * Encoding to use for input; defaults to the platform's default encoding.
     */
    private String encoding = null;

    /**
     * Prefix for loaded properties.
     */
    private String prefix = null;
    private boolean prefixValues = true;

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
        getRequiredJavaResource().setName(resource);
    }

    /**
     * Encoding to use for input, defaults to the platform's default
     * encoding. <p>
     *
     * For a list of possible values see
     * <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html">
     * https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html
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
        getRequiredJavaResource().setClasspath(classpath);
    }

    /**
     * Add a classpath to use when looking up a resource.
     * @return The classpath to be configured
     */
    public Path createClasspath() {
        return getRequiredJavaResource().createClasspath();
    }

    /**
     * Set the classpath to use when looking up a resource,
     * given as reference to a &lt;path&gt; defined elsewhere
     * @param r The reference value
     */
    public void setClasspathRef(Reference r) {
        getRequiredJavaResource().setClasspathRef(r);
    }

    /**
     * get the classpath used by this <code>LoadProperties</code>.
     * @return The classpath
     */
    public Path getClasspath() {
        return getRequiredJavaResource().getClasspath();
    }

    /**
     * Set the prefix to load these properties under.
     * @param prefix to set
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Whether to apply the prefix when expanding properties on the
     * right hand side of a properties file as well.
     *
     * @param b boolean
     * @since Ant 1.8.2
     */
    public void setPrefixValues(boolean b) {
        prefixValues = b;
    }

    /**
     * load Ant properties from the source file or resource
     *
     * @exception BuildException if something goes wrong with the build
     */
    @Override
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

        Charset charset = encoding == null ? Charset.defaultCharset() : Charset.forName(encoding);

        try (ChainReader instream = new ChainReaderHelper(getProject(),
            new InputStreamReader(new BufferedInputStream(src.getInputStream()), charset), filterChains)
                .getAssembledReader()) {

            String text = instream.readFully();

            if (text != null && !text.isEmpty()) {
                if (!text.endsWith("\n")) {
                    text += "\n";
                }
                ByteArrayInputStream tis = new ByteArrayInputStream(
                    text.getBytes(StandardCharsets.ISO_8859_1));
                final Properties props = new Properties();
                props.load(tis);

                Property propertyTask = new Property();
                propertyTask.bindToOwner(this);
                propertyTask.setPrefix(prefix);
                propertyTask.setPrefixValues(prefixValues);
                propertyTask.addProperties(props);
            }
        } catch (final IOException ioe) {
            throw new BuildException("Unable to load file: " + ioe, ioe, getLocation());
        }
    }

    /**
     * Adds a FilterChain.
     * @param filter the filter to add
     */
    public final void addFilterChain(FilterChain filter) {
        filterChains.add(filter);
    }

    /**
     * Set the source resource.
     * @param a the resource to load as a single element Resource collection.
     * @since Ant 1.7
     */
    public synchronized void addConfigured(ResourceCollection a) {
        if (src != null) {
            throw new BuildException("only a single source is supported");
        }
        if (a.size() != 1) {
            throw new BuildException(
                    "only single-element resource collections are supported");
        }
        src = a.iterator().next();
    }

    private synchronized JavaResource getRequiredJavaResource() {
        if (src == null) {
            src = new JavaResource();
            src.setProject(getProject());
        } else if (!(src instanceof JavaResource)) {
            throw new BuildException("expected a java resource as source");
        }
        return (JavaResource) src;
    }
}
