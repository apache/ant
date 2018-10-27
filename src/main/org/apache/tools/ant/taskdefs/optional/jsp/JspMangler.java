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
package org.apache.tools.ant.taskdefs.optional.jsp;

import java.io.File;

/**
 * This is an interface to the Mangler service that jspc needs to map
 * JSP file names to java files.
 * Note the complete lack of correlation
 * with Jasper's mangler interface.
 */
public interface JspMangler {


    /**
     * map from a jsp file to a java filename; does not do packages
     *
     * @param jspFile file
     * @return java filename
     */
    String mapJspToJavaName(File jspFile);

    /**
     * taking in the substring representing the path relative to the source dir
     * return a new string representing the destination path
     * @param path the path to map.
     * @return the mapped path.
     */
    String mapPath(String path);

}
