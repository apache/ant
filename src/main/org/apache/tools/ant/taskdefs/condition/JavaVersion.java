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
package org.apache.tools.ant.taskdefs.condition;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.DeweyDecimal;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * An Java version condition.
 * @since Ant 1.10.2
 */
public class JavaVersion implements Condition {

    private String atMost = null;
    private String atLeast = null;
    private String exactly = null;

    /**
     * Evaluate the condition.
     * @return true if the condition is true.
     * @throws BuildException if an error occurs.
     */
    public boolean eval() throws BuildException {
        validate();
        DeweyDecimal actual = JavaEnvUtils.getParsedJavaVersion();
        if (null != atLeast) {
            return actual.isGreaterThanOrEqual(new DeweyDecimal(atLeast));
        }
        if (null != exactly) {
            return actual.isEqual(new DeweyDecimal(exactly));
        }
        if (atMost != null) {
            return actual.isLessThanOrEqual(new DeweyDecimal(atMost));
        }
        //default
        return false;
    }

    private void validate() throws BuildException {
        if (atLeast != null && exactly != null && atMost != null) {
            throw new BuildException("Only one of atleast or atmost or exactly may be set.");
        }
        if (null == atLeast && null == exactly && atMost == null) {
            throw new BuildException("One of atleast or atmost or exactly must be set.");
        }
        if (atLeast != null) {
            try {
                // only created for side effect
                new DeweyDecimal(atLeast); //NOSONAR
            } catch (NumberFormatException e) {
                throw new BuildException(
                    "The 'atleast' attribute is not a Dewey Decimal eg 1.1.0 : "
                    + atLeast);
            }
        } else if (atMost != null) {
            try {
                new DeweyDecimal(atMost); //NOSONAR
            } catch (NumberFormatException e) {
                throw new BuildException(
                        "The 'atmost' attribute is not a Dewey Decimal eg 1.1.0 : "
                                + atMost);
            }
        } else {
            try {
                // only created for side effect
                new DeweyDecimal(exactly); //NOSONAR
            } catch (NumberFormatException e) {
                throw new BuildException(
                    "The 'exactly' attribute is not a Dewey Decimal eg 1.1.0 : "
                    + exactly);
            }
        }
    }

    /**
     * Get the atleast attribute.
     * @return the atleast attribute.
     */
    public String getAtLeast() {
        return atLeast;
    }

    /**
     * Set the atleast attribute.
     * This is of the form major.minor.point.
     * For example 1.7.0.
     * @param atLeast the version to set
     */
    public void setAtLeast(String atLeast) {
        this.atLeast = atLeast;
    }

    /**
     * Get the atmost attribute.
     * @return the atmost attribute.
     * @since Ant 1.10.10
     */
    public String getAtMost() {
        return atMost;
    }

    /**
     * Set the atmost attribute.
     * This is of the form major.minor.point.
     * For example 11.0.2
     * @param atMost the version to set
     * @since Ant 1.10.10
     */
    public void setAtMost(String atMost) {
        this.atMost = atMost;
    }

    /**
     * Get the exactly attribute.
     * @return the exactly attribute.
     */
    public String getExactly() {
        return exactly;
    }

    /**
     * Set the exactly attribute.
     * This is of the form major.minor.point.
     * For example 1.7.0.
     * @param exactly the version to check against.
     */
    public void setExactly(String exactly) {
        this.exactly = exactly;
    }

}
