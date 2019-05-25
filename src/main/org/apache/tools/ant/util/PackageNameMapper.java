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
 * Maps directory name matches into a dotted package name. This is
 * useful for matching JUnit test cases against their XML formatter
 * results.
 * <pre>
 * &lt;mapper classname="org.apache.tools.ant.util.PackageNameMapper"
 *         from="*Test.java" to="${test.data.dir}/TEST-*Test.xml"/&gt;
 * </pre>
 *
 */
public class PackageNameMapper extends GlobPatternMapper {
    /**
     *  Returns the part of the given string that matches the * in the
     *  &quot;from&quot; pattern replacing file separators with dots
     *
     *@param  name  Source filename
     *@return       Replaced variable part
     */
    @Override
    protected String extractVariablePart(String name) {
        String var = name.substring(prefixLength,
                name.length() - postfixLength);
        if (getHandleDirSep()) {
            var = var.replace('/', '.').replace('\\', '.');
        }
        return var.replace(File.separatorChar, '.');
    }
}

