/*
 * Copyright  2000-2002,2004 Apache Software Foundation
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

package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.File;
import java.util.Vector;

/**
 * Baseclass for BatchTest and JUnitTest.
 *
 * @author Stefan Bodewig
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 */
public abstract class BaseTest {
    protected boolean haltOnError = false;
    protected boolean haltOnFail = false;
    protected boolean filtertrace = true;
    protected boolean fork = false;
    protected String ifProperty = null;
    protected String unlessProperty = null;
    protected Vector formatters = new Vector();
    /** destination directory */
    protected File destDir = null;

    protected String failureProperty;
    protected String errorProperty;

    public void setFiltertrace(boolean value) {
        filtertrace = value;
    }

    public boolean getFiltertrace() {
        return filtertrace;
    }

    public void setFork(boolean value) {
        fork = value;
    }

    public boolean getFork() {
        return fork;
    }

    public void setHaltonerror(boolean value) {
        haltOnError = value;
    }

    public void setHaltonfailure(boolean value) {
        haltOnFail = value;
    }

    public boolean getHaltonerror() {
        return haltOnError;
    }

    public boolean getHaltonfailure() {
        return haltOnFail;
    }

    public void setIf(String propertyName) {
        ifProperty = propertyName;
    }

    public void setUnless(String propertyName) {
        unlessProperty = propertyName;
    }

    public void addFormatter(FormatterElement elem) {
        formatters.addElement(elem);
    }

    /**
     * Sets the destination directory.
     */
    public void setTodir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * @return the destination directory as an absolute path if it exists
     *         otherwise return <tt>null</tt>
     */
    public String getTodir() {
        if (destDir != null) {
            return destDir.getAbsolutePath();
        }
        return null;
    }

    public java.lang.String getFailureProperty() {
        return failureProperty;
    }

    public void setFailureProperty(String failureProperty) {
        this.failureProperty = failureProperty;
    }

    public java.lang.String getErrorProperty() {
        return errorProperty;
    }

    public void setErrorProperty(String errorProperty) {
        this.errorProperty = errorProperty;
    }
}
