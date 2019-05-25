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

package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.File;
import java.util.Vector;

/**
 * Baseclass for BatchTest and JUnitTest.
 *
 */
public abstract class BaseTest {
    // CheckStyle:VisibilityModifier OFF - bc
    protected boolean haltOnError = false;
    protected boolean haltOnFail = false;
    protected boolean filtertrace = true;
    protected boolean fork = false;
    protected String ifProperty = null;
    protected String unlessProperty = null;
    protected Vector<FormatterElement> formatters = new Vector<>();
    /** destination directory */
    protected File destDir = null;

    protected String failureProperty;
    protected String errorProperty;
    // CheckStyle:VisibilityModifier ON

    private Object ifCond;
    private Object unlessCond;
    private boolean skipNonTests;

    /**
     * Set the filtertrace attribute.
     * @param value a <code>boolean</code> value.
     */
    public void setFiltertrace(boolean value) {
        filtertrace = value;
    }

    /**
     * Get the filtertrace attribute.
     * @return the attribute.
     */
    public boolean getFiltertrace() {
        return filtertrace;
    }

    /**
     * Set the fork attribute.
     * @param value a <code>boolean</code> value.
     */
    public void setFork(boolean value) {
        fork = value;
    }

    /**
     * Get the fork attribute.
     * @return the attribute.
     */
    public boolean getFork() {
        return fork;
    }

    /**
     * Set the haltonerror attribute.
     * @param value a <code>boolean</code> value.
     */
    public void setHaltonerror(boolean value) {
        haltOnError = value;
    }

    /**
     * Set the haltonfailure attribute.
     * @param value a <code>boolean</code> value.
     */
    public void setHaltonfailure(boolean value) {
        haltOnFail = value;
    }

    /**
     * Get the haltonerror attribute.
     * @return the attribute.
     */
    public boolean getHaltonerror() {
        return haltOnError;
    }

    /**
     * Get the haltonfailure attribute.
     * @return the attribute.
     */
    public boolean getHaltonfailure() {
        return haltOnFail;
    }

    /**
     * Set the if attribute.
     * If this expression evaluates to true or the name of a property
     * which is present in project, the test will be run.
     * @param ifCondition the expression to evaluate
     * @since Ant 1.8.0
     */
    public void setIf(Object ifCondition) {
        ifCond = ifCondition;
        ifProperty = ifCondition != null ? String.valueOf(ifCondition) : null;
    }

    /**
     * Set the if attribute.
     * If this expression evaluates to true or the name of a property
     * which is present in project, the test will be run.
     * @param propertyName the expression to evaluate
     */
    public void setIf(String propertyName) {
        setIf((Object) propertyName);
    }

    /**
     * The if expression
     * @return Object
     * @since Ant 1.8.0
     */
    public Object getIfCondition() {
        return ifCond;
    }

    /**
     * Set the unless attribute.  If this expression evaluates to
     * false or the name of a property which is not present in
     * project, the test will be run.
     * @param unlessCondition the expression to evaluate
     * @since Ant 1.8.0
     */
    public void setUnless(Object unlessCondition) {
        unlessCond = unlessCondition;
        unlessProperty = unlessCondition != null
            ? String.valueOf(unlessCondition) : null;
    }

    /**
     * Set the unless attribute.  If this expression evaluates to
     * false or the name of a property which is not present in
     * project, the test will be run.
     * @param propertyName the expression to evaluate
     */
    public void setUnless(String propertyName) {
        setUnless((Object) propertyName);
    }

    /**
     * The unless expression
     * @return Object
     * @since Ant 1.8.0
     */
    public Object getUnlessCondition() {
        return unlessCond;
    }

    /**
     * Allow a formatter nested element.
     * @param elem a formatter nested element.
     */
    public void addFormatter(FormatterElement elem) {
        formatters.addElement(elem);
    }

    /**
     * Sets the destination directory.
     * @param destDir the destination directory.
     */
    public void setTodir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Get the destination directory.
     * @return the destination directory as an absolute path if it exists
     *         otherwise return <code>null</code>
     */
    public String getTodir() {
        if (destDir != null) {
            return destDir.getAbsolutePath();
        }
        return null;
    }

    /**
     * Get the failure property name.
     * @return the name of the property to set on failure.
     */
    public String getFailureProperty() {
        return failureProperty;
    }

    /**
     * Set the name of the failure property.
     * @param failureProperty the name of the property to set if
     *                        the test fails.
     */
    public void setFailureProperty(String failureProperty) {
        this.failureProperty = failureProperty;
    }

    /**
     * Get the failure property name.
     * @return the name of the property to set on failure.
     */
    public String getErrorProperty() {
        return errorProperty;
    }

    /**
     * Set the name of the error property.
     * @param errorProperty the name of the property to set if
     *                      the test has an error.
     */
    public void setErrorProperty(String errorProperty) {
        this.errorProperty = errorProperty;
    }

    public void setSkipNonTests(boolean skipNonTests) {
        this.skipNonTests = skipNonTests;
    }

    public boolean isSkipNonTests() {
        return skipNonTests;
    }
}
