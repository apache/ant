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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.filters.util.ChainReaderHelper;
import org.apache.tools.ant.types.FilterChain;

/**
 * Load a file's contents as Ant properties.
 *
 * @author Magesh Umasankar
 * @since Ant 1.5
 * @ant.task category="utility"
 */
public final class LoadProperties extends Task {

    /**
     * Source file
     */
    private File srcFile = null;

    /**
     * Holds filterchains
     */
    private final Vector filterChains = new Vector();

    /**
     * Encoding to use for filenames, defaults to the platform's default
     * encoding.
     */
    private String encoding = null;

    /**
     * Sets the file to load.
     *
     * @param srcFile The new SrcFile value
     */
    public final void setSrcFile(final File srcFile) {
        this.srcFile = srcFile;
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
     * read in a source file's contents and load them up as Ant properties
     *
     * @exception BuildException if something goes wrong with the build
     */
    public final void execute() throws BuildException {
        //validation
        if (srcFile == null) {
            throw new BuildException("Source file not defined.");
        }

        if (!srcFile.exists()) {
            throw new BuildException("Source file does not exist.");
        }

        if (!srcFile.isFile()) {
            throw new BuildException("Source file is not a file.");
        }

        FileInputStream fis = null;
        BufferedInputStream bis = null;
        Reader instream = null;
        ByteArrayInputStream tis = null;

        try {
            final long len = srcFile.length();

            //open up the file
            fis = new FileInputStream(srcFile);
            bis = new BufferedInputStream(fis);
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
                if (fis != null) {
                    fis.close();
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
