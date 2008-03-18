/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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
package org.apache.tools.ant.util.regexp;

// CheckStyle:HideUtilityClassConstructorCheck OFF - bc

/***
 * Regular expression utilities class which handles flag operations.
 *
 */
public class RegexpUtil {

    /**
     * Check the options has a particular flag set.
     *
     * @param options an <code>int</code> value
     * @param flag an <code>int</code> value
     * @return true if the flag is set
     */
    public static boolean hasFlag(int options, int flag) {
        return ((options & flag) > 0);
    }

    /**
     * Remove a particular flag from an int value contains the option flags.
     *
     * @param options an <code>int</code> value
     * @param flag an <code>int</code> value
     * @return the options with the flag unset
     */
    public static int removeFlag(int options, int flag) {
        return (options & (0xFFFFFFFF - flag));
    }
}
