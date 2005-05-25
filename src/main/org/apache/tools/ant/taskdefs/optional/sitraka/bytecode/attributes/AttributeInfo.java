/*
 * Copyright  2001-2002,2004-2005 The Apache Software Foundation
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
package org.apache.tools.ant.taskdefs.optional.sitraka.bytecode.attributes;

/**
 * Attribute info structure that provides base methods
 *
 */
public interface AttributeInfo {

    /** The source file attribute */
    String SOURCE_FILE = "SourceFile";

    /** The constant value attribute */
    String CONSTANT_VALUE = "ConstantValue";

    /** The code attribute */
    String CODE = "Code";

    /** The exceptions attribute */
    String EXCEPTIONS = "Exceptions";

    /** The line number table attribute */
    String LINE_NUMBER_TABLE = "LineNumberTable";

    /** The local variable table attribute */
    String LOCAL_VARIABLE_TABLE = "LocalVariableTable";

    /** The inner classes attribute */
    String INNER_CLASSES = "InnerClasses";

    /** The source dir attribute */
    String SOURCE_DIR = "SourceDir";

    /** The synthetic attribute */
    String SYNTHETIC = "Synthetic";

    /** The deprecated attribute */
    String DEPRECATED = "Deprecated";

    /** The unknown attribute */
    String UNKNOWN = "Unknown";
}
