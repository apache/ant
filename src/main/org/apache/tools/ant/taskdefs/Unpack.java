/*
 * Copyright  2001-2002,2004 The Apache Software Foundation
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


import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Abstract Base class for unpack tasks.
 *
 * @author Magesh Umasankar
 *
 * @since Ant 1.5
 */

public abstract class Unpack extends Task {

    protected File source;
    protected File dest;

    /**
     * @deprecated setSrc(String) is deprecated and is replaced with
     *             setSrc(File) to make Ant's Introspection
     *             mechanism do the work and also to encapsulate operations on
     *             the type in its own class.
     * @ant.attribute ignore="true"
     */
    public void setSrc(String src) {
        log("DEPRECATED - The setSrc(String) method has been deprecated."
            + " Use setSrc(File) instead.");
        setSrc(getProject().resolveFile(src));
    }

    /**
     * @deprecated setDest(String) is deprecated and is replaced with
     *             setDest(File) to make Ant's Introspection
     *             mechanism do the work and also to encapsulate operations on
     *             the type in its own class.
     * @ant.attribute ignore="true"
     */
    public void setDest(String dest) {
        log("DEPRECATED - The setDest(String) method has been deprecated."
            + " Use setDest(File) instead.");
        setDest(getProject().resolveFile(dest));
    }

    /**
     * The file to expand; required.
     * @param src file to expand
     */
    public void setSrc(File src) {
        source = src;
    }

    /**
     * The destination file or directory; optional.
     * @param dest destination file or directory
     */
    public void setDest(File dest) {
        this.dest = dest;
    }

    private void validate() throws BuildException {
        if (source == null) {
            throw new BuildException("No Src specified", getLocation());
        }

        if (!source.exists()) {
            throw new BuildException("Src doesn't exist", getLocation());
        }

        if (source.isDirectory()) {
            throw new BuildException("Cannot expand a directory", getLocation());
        }

        if (dest == null) {
            dest = new File(source.getParent());
        }

        if (dest.isDirectory()) {
            String defaultExtension = getDefaultExtension();
            createDestFile(defaultExtension);
        }
    }

    private void createDestFile(String defaultExtension) {
        String sourceName = source.getName();
        int len = sourceName.length();
        if (defaultExtension != null
            && len > defaultExtension.length()
            && defaultExtension.equalsIgnoreCase(sourceName.substring(len - defaultExtension.length()))) {
            dest = new File(dest, sourceName.substring(0,
                                                       len - defaultExtension.length()));
        } else {
            dest = new File(dest, sourceName);
        }
    }

    public void execute() throws BuildException {
        File savedDest = dest; // may be altered in validate
        try {
            validate();
            extract();
        } finally {
            dest = savedDest;
        }
    }

    protected abstract String getDefaultExtension();
    protected abstract void extract();
}
