/*
 * Copyright  2001-2004 Apache Software Foundation
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Abstract Base class for pack tasks.
 *
 * @author Magesh Umasankar
 *
 * @since Ant 1.5
 */

public abstract class Pack extends Task {

    protected File zipFile;
    protected File source;

    /**
     * the required destination file.
     * @param zipFile
     */
    public void setZipfile(File zipFile) {
        this.zipFile = zipFile;
    }

    /**
     * the required destination file.
     * @param zipFile
     */
    public void setDestfile(File zipFile) {
        setZipfile(zipFile);
    }

    /**
     * the file to compress; required.
     * @param src
     */
    public void setSrc(File src) {
        source = src;
    }


    /**
     * validation routine
     * @throws BuildException if anything is invalid
     */
    private void validate() throws BuildException {
        if (zipFile == null) {
            throw new BuildException("zipfile attribute is required", getLocation());
        }

        if (zipFile.isDirectory()) {
            throw new BuildException("zipfile attribute must not "
                                    + "represent a directory!", getLocation());
        }

        if (source == null) {
            throw new BuildException("src attribute is required", getLocation());
        }

        if (source.isDirectory()) {
            throw new BuildException("Src attribute must not "
                                    + "represent a directory!", getLocation());
        }
    }

    /**
     * validate, then hand off to the subclass
     * @throws BuildException
     */
    public void execute() throws BuildException {
        validate();

        if (!source.exists()) {
            log("Nothing to do: " + source.getAbsolutePath()
                + " doesn't exist.");
        } else if (zipFile.lastModified() < source.lastModified()) {
            log("Building: " + zipFile.getAbsolutePath());
            pack();
        } else {
            log("Nothing to do: " + zipFile.getAbsolutePath()
                + " is up to date.");
        }
    }

    /**
     * zip a stream to an output stream
     * @param in
     * @param zOut
     * @throws IOException
     */
    private void zipFile(InputStream in, OutputStream zOut)
        throws IOException {
        byte[] buffer = new byte[8 * 1024];
        int count = 0;
        do {
            zOut.write(buffer, 0, count);
            count = in.read(buffer, 0, buffer.length);
        } while (count != -1);
    }

    /**
     * zip a file to an output stream
     * @param file
     * @param zOut
     * @throws IOException
     */
    protected void zipFile(File file, OutputStream zOut)
        throws IOException {
        FileInputStream fIn = new FileInputStream(file);
        try {
            zipFile(fIn, zOut);
        } finally {
            fIn.close();
        }
    }

    /**
     * subclasses must implement this method to do their compression
     */
    protected abstract void pack();
}
