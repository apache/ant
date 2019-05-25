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

package org.apache.tools.ant.types.selectors;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;

/**
 * A convenience base class that you can subclass Selectors from. It
 * provides some helpful common behaviour. Note that there is no need
 * for Selectors to inherit from this class, it is only necessary that
 * they implement FileSelector.
 *
 * @since 1.5
 */
public abstract class BaseSelector extends DataType implements FileSelector {

    private String errmsg = null;
    private Throwable cause;

    /**
     * Allows all selectors to indicate a setup error. Note that only
     * the first error message is recorded.
     *
     * @param msg The error message any BuildException should throw.
     */
    public void setError(String msg) {
        if (errmsg == null) {
            errmsg = msg;
        }
    }

    /**
     * Allows all selectors to indicate a setup error. Note that only
     * the first error message is recorded.
     *
     * @param msg The error message any BuildException should throw.
     * @param cause Throwable
     */
    public void setError(String msg, Throwable cause) {
        if (errmsg == null) {
            errmsg = msg;
            this.cause = cause;
        }
    }

    /**
     * Returns any error messages that have been set.
     *
     * @return the error condition
     */
    public String getError() {
        return errmsg;
    }

    /**
     * <p>Subclasses can override this method to provide checking of their
     * state. So long as they call validate() from isSelected(), this will
     * be called automatically (unless they override validate()).</p>
     * <p>Implementations should check for incorrect settings and call
     * setError() as necessary.</p>
     */
    public void verifySettings() {
        if (isReference()) {
            getRef().verifySettings();
        }
    }

    /**
     * Subclasses can use this to throw the requisite exception
     * in isSelected() in the case of an error condition.
     */
    public void validate() {
        if (getError() == null) {
            verifySettings();
        }
        if (getError() != null) {
            throw new BuildException(errmsg, cause);
        }
        if (!isReference()) {
            dieOnCircularReference();
        }
    }

    /**
     * Method that each selector will implement to create their
     * selection behaviour. If there is a problem with the setup
     * of a selector, it can throw a BuildException to indicate
     * the problem.
     *
     * @param basedir A java.io.File object for the base directory
     * @param filename The name of the file to check
     * @param file A File object for this filename
     * @return whether the file should be selected or not
     */
    public abstract boolean isSelected(File basedir, String filename,
                                       File file);

    private BaseSelector getRef() {
        return getCheckedRef(BaseSelector.class);
    }
}


