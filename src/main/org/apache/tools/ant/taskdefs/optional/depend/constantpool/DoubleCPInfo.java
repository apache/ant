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
package org.apache.tools.ant.taskdefs.optional.depend.constantpool;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * The constant pool entry subclass used to represent double constant
 * values.
 *
 */
public class DoubleCPInfo extends ConstantCPInfo {
    /**
     * Constructor
     */
    public DoubleCPInfo() {
        super(CONSTANT_DOUBLE, 2);
    }

    /**
     * read a constant pool entry from a class stream.
     *
     * @param cpStream the DataInputStream which contains the constant pool
     *      entry to be read.
     * @exception IOException if there is a problem reading the entry from the
     *      stream.
     */
    @Override
    public void read(DataInputStream cpStream) throws IOException {
        setValue(Double.valueOf(cpStream.readDouble()));
    }

    /**
     * Print a readable version of the constant pool entry.
     *
     * @return the string representation of this constant pool entry.
     */
    @Override
    public String toString() {
        return "Double Constant Pool Entry: " + getValue();
    }

}
