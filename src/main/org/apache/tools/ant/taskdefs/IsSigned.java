/*
 * Copyright  2004 The Apache Software Foundation
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.Enumeration;

/**
 * Checks whether a jarfile is signed: if the name of the
 * signature is passed, the file is checked for presence of that
 * particular signature; otherwise the file is checked for the
 * existence of any signature.
 */
public class IsSigned extends ConditionAndTask {

    private static final String SIG_START = "META-INF/";
    private static final String SIG_END = ".SF";

    private String name;
    private File   file;

   /**
     * The jarfile that is to be tested for the presence
     * of a signature.
     *
     * @param file jarfile to be tested.
     */
    public void setFile(File file) {
        this.file = file;
    }

   /**
     * The signature name to check jarfile for.
     *
     * @param name signature to look for.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns <CODE>true</code> if the file exists and is signed with
     * the signature specified, or, if <CODE>name</code> wasn't
     * specified, if the file contains a signature.
     * @return true if the file is signed.
     */
    protected boolean evaluate() {
        if (file == null) {
            throw new BuildException("The file attribute must be set.");
        }
        if (file != null && !file.exists()) {
            log("The file \"" + file.getAbsolutePath()
                    + "\" does not exist.", Project.MSG_VERBOSE);
            return false;
        }

        ZipFile jarFile = null;
        try {
            jarFile = new ZipFile(file);
            if (null == name) {
                Enumeration entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    String name = ((ZipEntry) entries.nextElement()).getName();
                    if (name.startsWith(SIG_START) && name.endsWith(SIG_END)) {
                        log("File \"" + file.getAbsolutePath()
                            + "\" is signed.", Project.MSG_VERBOSE);
                        return true;
                    }
                }
                return false;
            } else {
                boolean shortSig = jarFile.getEntry(SIG_START
                                    + name.toUpperCase()
                                    + SIG_END) != null;
                boolean longSig  = jarFile.getEntry(SIG_START
                                    + name.substring(0, 8).toUpperCase()
                                    + SIG_END) != null;
                if (shortSig || longSig) {
                    log("File \"" + file.getAbsolutePath()
                        + "\" is signed.", Project.MSG_VERBOSE);
                    return true;
                } else {
                    return false;
                }
            }
        } catch (IOException e) {
            log("Got IOException reading file \"" + file.getAbsolutePath()
                + "\"" + e, Project.MSG_VERBOSE);
            return false;
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                    // Ignored
                }
            }
        }
    }
}
