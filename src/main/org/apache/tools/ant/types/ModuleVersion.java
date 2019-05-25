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

package org.apache.tools.ant.types;

import java.util.Objects;

/**
 * Element describing the parts of a Java
 * <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/module/ModuleDescriptor.Version.html">module version</a>.
 * The version number is required;  all other parts are optional.
 *
 * @since 1.10.6
 */
public class ModuleVersion {
    /** Module version's required <em>version number</em>. */
    private String number;

    /** Module version's optional <em>pre-release version</em>. */
    private String preRelease;

    /** Module version's optional <em>build version</em>. */
    private String build;

    /**
     * Returns this element's version number.
     *
     * @return version number
     */
    public String getNumber() {
        return number;
    }

    /**
     * Sets this element's required version number.  This cannot contain
     * an ASCII hyphen ({@code -}) or plus ({@code +}), as those characters
     * are used as delimiters in a complete module version string.
     *
     * @param number version number
     * @throws NullPointerException if argument is {@code null}
     * @throws IllegalArgumentException if argument contains {@code '-'}
     *                                  or {@code '+'}
     */
    public void setNumber(final String number) {
        Objects.requireNonNull(number, "Version number cannot be null.");
        if (number.indexOf('-') >= 0 || number.indexOf('+') >= 0) {
            throw new IllegalArgumentException(
                "Version number cannot contain '-' or '+'.");
        }
        this.number = number;
    }

    /**
     * Returns this element's pre-release version, if set.
     *
     * @return pre-release value, or {@code null}
     */
    public String getPreRelease() {
        return preRelease;
    }

    /**
     * Sets this element's pre-release version.  This can be any value
     * which doesn't contain an ASCII plus ({@code +}).
     *
     * @param pre pre-release version, or {@code null}
     *
     * @throws IllegalArgumentException if argument contains "{@code +}"
     */
    public void setPreRelease(final String pre) {
        if (pre != null && pre.indexOf('+') >= 0) {
            throw new IllegalArgumentException(
                "Version's pre-release cannot contain '+'.");
        }
        this.preRelease = pre;
    }

    /**
     * Returns this element's build version, if set.
     *
     * @return build value, or {@code null}
     */
    public String getBuild() {
        return build;
    }

    /**
     * Sets this element's build version.  This can be any value, including
     * {@code null}.
     *
     * @param build build version, or {@code null}
     */
    public void setBuild(final String build) {
        this.build = build;
    }

    /**
     * Snapshots this element's state and converts it to a string compliant
     * with {@code ModuleDescriptor.Version}.
     *
     * @return Java module version string built from this object's properties
     *
     * @throws IllegalStateException if {@linkplain #getNumber() number}
     *                               is {@code null}
     */
    public String toModuleVersionString() {
        if (number == null) {
            throw new IllegalStateException("Version number cannot be null.");
        }

        StringBuilder version = new StringBuilder(number);
        if (preRelease != null || build != null) {
            version.append('-').append(Objects.toString(preRelease, ""));
        }
        if (build != null) {
            version.append('+').append(build);
        }

        return version.toString();
    }

    /**
     * Returns a summary of this object's state, suitable for debugging.
     *
     * @return string form of this instance
     */
    @Override
    public String toString() {
        return getClass().getName() +
            "[number=" + number +
            ", preRelease=" + preRelease +
            ", build=" + build +
            "]";
    }
}
