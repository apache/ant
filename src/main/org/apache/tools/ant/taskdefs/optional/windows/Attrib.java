/* 
 * Copyright  2002-2004 Apache Software Foundation
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

package org.apache.tools.ant.taskdefs.optional.windows;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.ExecuteOn;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.FileSet;

import java.io.File;

/**
 * Attrib equivalent for Win32 environments.
 * Note: Attrib parameters /S and /D are not handled.
 *
 * @author skanga@bigfoot.com
 * @author <a href="mailto:Jerome@jeromelacoste.com">Jerome Lacoste</a>
 *
 * @since Ant 1.6
 */
public class Attrib extends ExecuteOn {

    private static final String ATTR_READONLY = "R";
    private static final String ATTR_ARCHIVE  = "A";
    private static final String ATTR_SYSTEM   = "S";
    private static final String ATTR_HIDDEN   = "H";
    private static final String SET    = "+";
    private static final String UNSET  = "-";

    private boolean haveAttr = false;

    public Attrib() {
        super.setExecutable("attrib");
        super.setParallel(false);
    }

    public void setFile(File src) {
        FileSet fs = new FileSet();
        fs.setFile(src);
        addFileset(fs);
    }

    /** set the ReadOnly file attribute */
    public void setReadonly(boolean value) {
        addArg(value, ATTR_READONLY);
    }

    /** set the Archive file attribute */
    public void setArchive(boolean value) {
        addArg(value, ATTR_ARCHIVE);
    }

    /** set the System file attribute */
    public void setSystem(boolean value) {
        addArg(value, ATTR_SYSTEM);
    }

    /** set the Hidden file attribute */
    public void setHidden(boolean value) {
        addArg(value, ATTR_HIDDEN);
    }

    protected void checkConfiguration() {
        if (!haveAttr()) {
            throw new BuildException("Missing attribute parameter",
                                     getLocation());
        }
        super.checkConfiguration();
    }

    /**
     * @ant.attribute ignore="true"
     */
    public void setExecutable(String e) {
        throw new BuildException(getTaskType()
            + " doesn\'t support the executable attribute", getLocation());
    }

    /**
     * @ant.attribute ignore="true"
     */
    public void setCommand(String e) {
        throw new BuildException(getTaskType()
            + " doesn\'t support the command attribute", getLocation());
    }

    /**
     * @ant.attribute ignore="true"
     */
    public void setAddsourcefile(boolean b) {
        throw new BuildException(getTaskType()
            + " doesn\'t support the addsourcefile attribute", getLocation());
    }

    /**
     * @ant.attribute ignore="true"
     */
    public void setSkipEmptyFilesets(boolean skip) {
        throw new BuildException(getTaskType() + " doesn\'t support the "
                                 + "skipemptyfileset attribute",
                                 getLocation());
    }

    /**
     * @ant.attribute ignore="true"
     */
    public void setParallel(boolean parallel) {
        throw new BuildException(getTaskType()
                                 + " doesn\'t support the parallel attribute",
                                 getLocation());
    }

    /**
     * @ant.attribute ignore="true"
     */
    public void setMaxParallel(int max) {
        throw new BuildException(getTaskType()
                                 + " doesn\'t support the maxparallel attribute",
                                 getLocation());
    }

    protected boolean isValidOs() {
        return Os.isFamily("windows") && super.isValidOs();
    }

    private static String getSignString(boolean attr) {
        return (attr == true ? SET : UNSET);
    }

    private void addArg(boolean sign, String attribute) {
        createArg().setValue(getSignString(sign) + attribute);
        haveAttr = true;
    }

    private boolean haveAttr() {
        return haveAttr;
    }

}
