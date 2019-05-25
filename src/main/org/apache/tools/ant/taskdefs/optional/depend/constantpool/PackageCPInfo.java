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
 * Represents the package info (within a module) constant pool entry
 */
public class PackageCPInfo extends ConstantCPInfo {

    private int packageNameIndex;
    private String packageName;

    public PackageCPInfo() {
        super(CONSTANT_PACKAGEINFO, 1);
    }

    @Override
    public void read(final DataInputStream cpStream) throws IOException {
        this.packageNameIndex = cpStream.readUnsignedShort();
    }

    @Override
    public void resolve(final ConstantPool constantPool) {
        this.packageName = ((Utf8CPInfo) constantPool.getEntry(this.packageNameIndex)).getValue();

        super.resolve(constantPool);
    }

    @Override
    public String toString() {
        return "Package info Constant Pool Entry for " + this.packageName + "[" + this.packageNameIndex + "]";
    }
}
