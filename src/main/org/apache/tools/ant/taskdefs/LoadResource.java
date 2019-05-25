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
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.filters.util.ChainReaderHelper;
import org.apache.tools.ant.filters.util.ChainReaderHelper.ChainReader;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;

/**
 * Load a resource into a property
 *
 * @since Ant 1.7
 * @ant.task category="utility"
 */
public class LoadResource extends Task {

    /**
     * The resource to load.
     */
    private Resource src;

    /**
     * what to do when it goes pear-shaped
     */
    private boolean failOnError = true;

    /**
     * suppress error message if it goes pear-shaped, sets failOnError=false
     */
    private boolean quiet = false;

    /**
     * Encoding to use for filenames, defaults to the platform's default
     * encoding.
     */
    private String encoding = null;

    /**
     * name of property
     */
    private String property = null;

    /**
     * Holds FilterChains
     */
    private final List<FilterChain> filterChains = new Vector<>();

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
     * Property name to save to.
     *
     * @param property The new Property value
     */
    public final void setProperty(final String property) {
        this.property = property;
    }

    /**
     * If true, fail on load error.
     *
     * @param fail The new Failonerror value
     */
    public final void setFailonerror(final boolean fail) {
        failOnError = fail;
    }

    /**
     * If true, suppress the load error report and set the
     * the failonerror value to false.
     * @param quiet The new Quiet value
     */
    public void setQuiet(final boolean quiet) {
        this.quiet = quiet;
        if (quiet) {
            this.failOnError = false;
        }
    }

    /**
     * read in a source file to a property
     *
     * @exception BuildException if something goes wrong with the build
     */
    @Override
    public final void execute()
        throws BuildException {
        //validation
        if (src == null) {
            throw new BuildException("source resource not defined");
        }
        if (property == null) {
            throw new BuildException("output property not defined");
        }
        if (quiet && failOnError) {
            throw new BuildException("quiet and failonerror cannot both be set to true");
        }
        if (!src.isExists()) {
            String message = src + " doesn't exist";
            if (failOnError) {
                throw new BuildException(message);
            }
            log(message, quiet ? Project.MSG_WARN : Project.MSG_ERR);
            return;
        }

        log("loading " + src + " into property " + property,
            Project.MSG_VERBOSE);

        Charset charset = encoding == null ? Charset.defaultCharset()
            : Charset.forName(encoding);
        try {
            final long len = src.getSize();
            log("resource size = " + (len != Resource.UNKNOWN_SIZE
                ? String.valueOf(len) : "unknown"), Project.MSG_DEBUG);
            //discard most of really big resources
            final int size = (int) len;
            //open up the resource

            String text;
            if (size != 0) {
                try (ChainReader chainReader = new ChainReaderHelper(
                    getProject(),
                    new InputStreamReader(
                        new BufferedInputStream(src.getInputStream()), charset),
                    filterChains).with(crh -> {
                        if (src.getSize() != Resource.UNKNOWN_SIZE) {
                            crh.setBufferSize(size);
                        }
                    }).getAssembledReader()) {

                    text = chainReader.readFully();
                }
            } else {
                log("Do not set property " + property + " as its length is 0.",
                    quiet ? Project.MSG_VERBOSE : Project.MSG_INFO);
                text = null;
            }

            if (text != null && !text.isEmpty()) {
                getProject().setNewProperty(property, text);
                log("loaded " + text.length() + " characters",
                    Project.MSG_VERBOSE);
                log(property + " := " + text, Project.MSG_DEBUG);
            }
        } catch (final IOException ioe) {
            final String message = "Unable to load resource: " + ioe;
            if (failOnError) {
                throw new BuildException(message, ioe, getLocation());
            }
            log(message, quiet ? Project.MSG_VERBOSE : Project.MSG_ERR);
        } catch (final BuildException be) {
            if (failOnError) {
                throw be;
            }
            log(be.getMessage(), quiet ? Project.MSG_VERBOSE : Project.MSG_ERR);
        }
    }

    /**
     * Add the FilterChain element.
     * @param filter the filter to add
     */
    public final void addFilterChain(FilterChain filter) {
        filterChains.add(filter);
    }

    /**
     * Set the source resource.
     * @param a the resource to load as a single element Resource collection.
     */
    public void addConfigured(ResourceCollection a) {
        if (a.size() != 1) {
            throw new BuildException(
                "only single argument resource collections are supported");
        }
        src = a.iterator().next();
    }

}
