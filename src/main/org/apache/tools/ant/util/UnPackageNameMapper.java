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
package org.apache.tools.ant.util;

import java.io.File;

/**
 * Maps dotted package name matches to a directory name.
 * This is the inverse of the package mapper.
 * This is useful for matching XML formatter results against their JUnit test
 * cases.
 * <pre>
 * &lt;mapper classname="org.apache.tools.ant.util.UnPackageNameMapper"
 *         from="${test.data.dir}/TEST-*Test.xml" to="*Test.java"&gt;
 * </pre>
 *
 *
 */
public class UnPackageNameMapper extends GlobPatternMapper {
    /**
     *  Returns the part of the given string that matches the * in the
     *  &quot;from&quot; pattern replacing dots with file separators
     *
     *@param  name  Source filename
     *@return       Replaced variable part
     */
    @Override
    protected String extractVariablePart(String name) {
        String var = name.substring(prefixLength,
                name.length() - postfixLength);
        return var.replace('.', File.separatorChar);
    }
}

