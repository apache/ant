/*
 * Copyright  2001-2002,2004 Apache Software Foundation
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
package org.apache.tools.ant.util.regexp;

/***
 * Regular expression utilities class which handles flag operations
 *
 * @author <a href="mailto:mattinger@mindless.com">Matthew Inger</a>
 */
public class RegexpUtil {
    public static final boolean hasFlag(int options, int flag) {
        return ((options & flag) > 0);
    }

    public static final int removeFlag(int options, int flag) {
        return (options & (0xFFFFFFFF - flag));
    }
}
