/*
 * Copyright  2000-2002,2004 The Apache Software Foundation
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
package org.apache.tools.ant.taskdefs.optional;

import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Java;

/**
 * Executes the Apache Stylebook documentation generator.
 * Unlike the commandline version of this tool, all three arguments
 * are required to run stylebook.
 * <p>
 * Being extended from &lt;Java&gt;, all the parent's attributes
 * and options are available. Do not set any apart from the <tt>classpath</tt>
 * as they are not guaranteed to be there in future.
 * @todo stop extending from Java.
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 * @author <a href="mailto:marcus.boerger@post.rwth-aachen.de">Marcus
 *      B&ouml;rger</a>
 */
public class StyleBook extends Java {
    protected File m_targetDirectory;
    protected File m_skinDirectory;
    protected String m_loaderConfig;
    protected File m_book;


    public StyleBook() {
        setClassname("org.apache.stylebook.StyleBook");
        setFork(true);
        setFailonerror(true);
    }

    /**
     * The book xml file that the documentation generation starts from;
     * required.
     */

    public void setBook(final File book) {
        m_book = book;
    }


    /**
     * the directory that contains the stylebook skin;
     * required.
     */
    public void setSkinDirectory(final File skinDirectory) {
        m_skinDirectory = skinDirectory;
    }


    /**
     * the destination directory where the documentation is generated;
     * required.
     */
    public void setTargetDirectory(final File targetDirectory) {
        m_targetDirectory = targetDirectory;
    }

    /**
     * A loader configuration to send to stylebook; optional.
     */
    public void setLoaderConfig(final String loaderConfig) {
        m_loaderConfig = loaderConfig;
    }


    /**
     * call the program
     */
    public void execute()
         throws BuildException {

        if (null == m_targetDirectory) {
            throw new BuildException("TargetDirectory attribute not set.");
        }

        if (null == m_skinDirectory) {
            throw new BuildException("SkinDirectory attribute not set.");
        }

        if (null == m_book) {
            throw new BuildException("book attribute not set.");
        }

        createArg().setValue("targetDirectory=" + m_targetDirectory);
        createArg().setValue(m_book.toString());
        createArg().setValue(m_skinDirectory.toString());
        if (null != m_loaderConfig) {
            createArg().setValue("loaderConfig=" + m_loaderConfig);
        }

        super.execute();
    }
}

