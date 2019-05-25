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
package org.apache.tools.ant.taskdefs.optional.depend;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import org.apache.tools.ant.taskdefs.optional.depend.constantpool.ClassCPInfo;
import org.apache.tools.ant.taskdefs.optional.depend.constantpool.ConstantPool;
import org.apache.tools.ant.taskdefs.optional.depend.constantpool.ConstantPoolEntry;

/**
 * A ClassFile object stores information about a Java class. The class may
 * be read from a DataInputStream.and written to a DataOutputStream. These
 * are usually streams from a Java class file or a class file component of a
 * Jar file.
 *
 */
public class ClassFile {

    /** The Magic Value that marks the start of a Java class file  */
    private static final int CLASS_MAGIC = 0xCAFEBABE;

    /** This class' constant pool.  */
    private ConstantPool constantPool;

    /** The class name for this class.  */
    private String className;

    /**
     * Read the class from a data stream. This method takes an InputStream
     * as input and parses the class from the stream. <p>
     *
     *
     *
     * @param stream an InputStream from which the class will be read
     * @exception IOException if there is a problem reading from the given
     *      stream.
     * @exception ClassFormatError if the class cannot be parsed correctly
     */
    public void read(InputStream stream) throws IOException, ClassFormatError {
        DataInputStream classStream = new DataInputStream(stream);

        if (classStream.readInt() != CLASS_MAGIC) {
            throw new ClassFormatError(
                "No Magic Code Found - probably not a Java class file.");
        }

        // right we have a good looking class file.
        /* int minorVersion = */ classStream.readUnsignedShort();
        /* int majorVersion = */ classStream.readUnsignedShort();

        // read the constant pool in and resolve it
        constantPool = new ConstantPool();

        constantPool.read(classStream);
        constantPool.resolve();

        /* int accessFlags = */ classStream.readUnsignedShort();
        int thisClassIndex = classStream.readUnsignedShort();
        /* int superClassIndex = */ classStream.readUnsignedShort();
        ClassCPInfo classInfo
            = (ClassCPInfo) constantPool.getEntry(thisClassIndex);
        className  = classInfo.getClassName();
    }

    /**
     * Get the classes which this class references.
     *
     * @return a vector of class names which this class references
     */
    public Vector<String> getClassRefs() {

        Vector<String> classRefs = new Vector<>();

        final int size = constantPool.size();
        for (int i = 0; i < size; ++i) {
            ConstantPoolEntry entry = constantPool.getEntry(i);

            if (entry != null
                && entry.getTag() == ConstantPoolEntry.CONSTANT_CLASS) {
                ClassCPInfo classEntry = (ClassCPInfo) entry;

                if (!classEntry.getClassName().equals(className)) {
                    classRefs.add(
                        ClassFileUtils.convertSlashName(classEntry.getClassName()));
                }
            }
        }

        return classRefs;
    }

    /**
     * Get the class' fully qualified name in dot format.
     *
     * @return the class name in dot format (eg. java.lang.Object)
     */
    public String getFullClassName() {
        return ClassFileUtils.convertSlashName(className);
    }
}
