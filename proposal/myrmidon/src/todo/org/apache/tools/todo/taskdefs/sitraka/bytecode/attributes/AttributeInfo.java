/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.sitraka.bytecode.attributes;

/**
 * Attribute info structure that provides base methods
 *
 * @author <a href="sbailliez@imediation.com">Stephane Bailliez</a>
 */
public interface AttributeInfo
{

    public final static String SOURCE_FILE = "SourceFile";

    public final static String CONSTANT_VALUE = "ConstantValue";

    public final static String CODE = "Code";

    public final static String EXCEPTIONS = "Exceptions";

    public final static String LINE_NUMBER_TABLE = "LineNumberTable";

    public final static String LOCAL_VARIABLE_TABLE = "LocalVariableTable";

    public final static String INNER_CLASSES = "InnerClasses";

    public final static String SOURCE_DIR = "SourceDir";

    public final static String SYNTHETIC = "Synthetic";

    public final static String DEPRECATED = "Deprecated";

    public final static String UNKNOWN = "Unknown";

}
