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

/**
 * Simple comparison condition.
 *
 * @since Ant 1.4
 */
public class Equals implements Condition {
    private static final int REQUIRED = 1 | 2;

    private Object arg1, arg2;
    private boolean trim = false;
    private boolean caseSensitive = true;
    private int args;
    private boolean forcestring = false;

    /**
     * Set the first argument
     * @param arg1 the first argument.
     * @since Ant 1.8
     */
    public void setArg1(Object arg1) {
        if (arg1 instanceof String) {
            setArg1((String) arg1);
        } else {
            setArg1Internal(arg1);
        }
    }

    /**
     * Set the first string
     *
     * @param arg1 the first string
     */
    public void setArg1(String arg1) {
        setArg1Internal(arg1);
    }

    private void setArg1Internal(Object arg1) {
        this.arg1 = arg1;
        args |= 1;
    }

    /**
     * Set the second argument
     * @param arg2 the second argument.
     * @since Ant 1.8
     */
    public void setArg2(Object arg2) {
        if (arg2 instanceof String) {
            setArg2((String) arg2);
        } else {
            setArg2Internal(arg2);
        }
    }

    /**
     * Set the second string
     *
     * @param arg2 the second string
     */
    public void setArg2(String arg2) {
        setArg2Internal(arg2);
    }

    private void setArg2Internal(Object arg2) {
        this.arg2 = arg2;
        args |= 2;
    }

    /**
     * Should we want to trim the arguments before comparing them?
     * @param b if true trim the arguments
     * @since Ant 1.5
     */
    public void setTrim(boolean b) {
        trim = b;
    }

    /**
     * Should the comparison be case sensitive?
     * @param b if true use a case sensitive comparison (this is the
     *          default)
     * @since Ant 1.5
     */
    public void setCasesensitive(boolean b) {
        caseSensitive = b;
    }

    /**
     * Set whether to force string comparisons for non-equal, non-string objects.
     * This allows object properties (legal in Ant 1.8.x+) to be compared as strings.
     * @param forcestring value to set
     * @since Ant 1.8.1
     */
    public void setForcestring(boolean forcestring) {
        this.forcestring = forcestring;
    }

    /**
     * @return true if the two strings are equal
     * @exception BuildException if the attributes are not set correctly
     */
    public boolean eval() throws BuildException {
        if ((args & REQUIRED) != REQUIRED) {
            throw new BuildException("both arg1 and arg2 are required in equals");
        }
        if (arg1 == arg2 || arg1 != null && arg1.equals(arg2)) {
            return true;
        }
        if (forcestring) {
            arg1 = arg1 == null || arg1 instanceof String ? arg1 : arg1.toString();
            arg2 = arg2 == null || arg2 instanceof String ? arg2 : arg2.toString();
        }
        if (arg1 instanceof String && trim) {
            arg1 = ((String) arg1).trim();
        }
        if (arg2 instanceof String && trim) {
            arg2 = ((String) arg2).trim();
        }
        if (arg1 instanceof String && arg2 instanceof String) {
            String s1 = (String) arg1;
            String s2 = (String) arg2;
            return caseSensitive ? s1.equals(s2) : s1.equalsIgnoreCase(s2);
        }
        return false;
    }
}
