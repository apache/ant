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
package org.apache.tools.ant.taskdefs;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.filters.util.ChainReaderHelper;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.FilterChain;

/**
 * Load a file's contents as Ant properties.
 *
 * @since Ant 1.5
 * @ant.task category="utility"
 */
public final class LoadProperties extends Task {

    /**
     * Source file
     */
    private File srcFile = null;

    /**
     * Resource
     */
    private String resource = null;

    /**
     * Classpath
     */
    private Path classpath = null;

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
        this.srcFile = srcFile;
    }

    /**
     * Set the resource name of a property file to load.
     *
     * @param resource resource on classpath
     */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /**
     * Encoding to use for input, defaults to the platform's default
     * encoding. <p>
     *
     * For a list of possible values see
     * <a href="http://java.sun.com/products/jdk/1.2/docs/guide/internat/encoding.doc.html">
     * http://java.sun.com/products/jdk/1.2/docs/guide/internat/encoding.doc.html
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
        if (this.classpath == null) {
            this.classpath = classpath;
        } else {
            this.classpath.append(classpath);
        }
    }

    /**
     * Add a classpath to use when looking up a resource.
     */
    public Path createClasspath() {
        if (this.classpath == null) {
            this.classpath = new Path(getProject());
        }
        return this.classpath.createPath();
    }

    /**
     * Set the classpath to use when looking up a resource,
     * given as reference to a &lt;path&gt; defined elsewhere
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * get the classpath used by this <CODE>LoadProperties</CODE>.
     */
    public Path getClasspath() {
        return classpath;
    }

    /**
     * load Ant properties from the source file or resource
     *
     * @exception BuildException if something goes wrong with the build
     */
    public final void execute() throws BuildException {
        //validation
        if (srcFile == null && resource == null) {
            throw new BuildException(
                "One of \"srcfile\" or \"resource\" is required.");
        }

        BufferedInputStream bis = null;

        if (srcFile != null) {
            if (!srcFile.exists()) {
                throw new BuildException("Source file does not exist.");
            }

            if (!srcFile.isFile()) {
                throw new BuildException("Source file is not a file.");
            }

            try {
                bis = new BufferedInputStream(new FileInputStream(srcFile));
            } catch (IOException eyeOhEx) {
                throw new BuildException(eyeOhEx);
            }
        } else {
            ClassLoader cL = (classpath != null)
                ? getProject().createClassLoader(classpath)
                : LoadProperties.class.getClassLoader();

            InputStream is = (cL == null)
                ? ClassLoader.getSystemResourceAsStream(resource)
                : cL.getResourceAsStream(resource);

            if (is != null) {
                bis = new BufferedInputStream(is);
            } else { // do it like Property
                log("Unable to find resource " + resource, Project.MSG_WARN);
                return;
            }
        }

        Reader instream = null;
        ByteArrayInputStream tis = null;

        try {
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

                Property propertyTask =
                    (Property) getProject().createTask("property");
                propertyTask.setTaskName(getTaskName());
                propertyTask.addProperties(props);
            }

        } catch (final IOException ioe) {
            final String message = "Unable to load file: " + ioe.toString();
            throw new BuildException(message, ioe, getLocation());
        } catch (final BuildException be) {
            throw be;
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
            } catch (IOException ioex) {
                //ignore
            }
            try {
                if (tis != null) {
                    tis.close();
                }
            } catch (IOException ioex) {
                //ignore
            }
        }
    }

    /**
     * Adds a FilterChain.
     */
    public final void addFilterChain(FilterChain filter) {
        filterChains.addElement(filter);
    }

//end class
}
