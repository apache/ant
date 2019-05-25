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
package org.apache.tools.ant.taskdefs.optional.depend.constantpool;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * The constant pool entry which stores class information.
 *
 */
public class ClassCPInfo extends ConstantPoolEntry {

    /**
     * The class' name. This will be only valid if the entry has been
     * resolved against the constant pool.
     */
    private String className;

    /**
     * The index into the constant pool where this class' name is stored. If
     * the class name is changed, this entry is invalid until this entry is
     * connected to a constant pool.
     */
    private int index;

    /**
     * Constructor. Sets the tag value for this entry to type Class
     */
    public ClassCPInfo() {
        super(CONSTANT_CLASS, 1);
    }

    /**
     * Read the entry from a stream.
     *
     * @param cpStream the stream containing the constant pool entry to be
     *      read.
     * @exception IOException thrown if there is a problem reading the entry
     *      from the stream.
     */
    @Override
    public void read(DataInputStream cpStream) throws IOException {
        index = cpStream.readUnsignedShort();
        className = "unresolved";
    }

    /**
     * Generate a string readable version of this entry
     *
     * @return string representation of this constant pool entry
     */
    @Override
    public String toString() {
        return "Class Constant Pool Entry for " + className + "[" + index + "]";
    }

    /**
     * Resolve this class info against the given constant pool.
     *
     * @param constantPool the constant pool with which to resolve the
     *      class.
     */
    @Override
    public void resolve(ConstantPool constantPool) {
        className = ((Utf8CPInfo) constantPool.getEntry(index)).getValue();

        super.resolve(constantPool);
    }

    /**
     * Get the class name of this entry.
     *
     * @return the class' name.
     */
    public String getClassName() {
        return className;
    }

}
