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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The constant pool of a Java class. The constant pool is a collection of
 * constants used in a Java class file. It stores strings, constant values,
 * class names, method names, field names etc.
 *
 * @see <a href="https://docs.oracle.com/javase/specs/index.html">The Java Virtual
 *      Machine Specification</a>
 */
public class ConstantPool {

    /** The entries in the constant pool. */
    private final List<ConstantPoolEntry> entries = new ArrayList<>();

    /**
     * A Hashtable of UTF8 entries - used to get constant pool indexes of
     * the UTF8 values quickly
     */
    private final Map<String, Integer> utf8Indexes = new HashMap<>();

    /** Initialise the constant pool. */
    public ConstantPool() {
        // The zero index is never present in the constant pool itself so
        // we add a null entry for it
        entries.add(null);
    }

    /**
     * Read the constant pool from a class input stream.
     *
     * @param classStream the DataInputStream of a class file.
     * @exception IOException if there is a problem reading the constant pool
     *      from the stream
     */
    public void read(DataInputStream classStream) throws IOException {
        int numEntries = classStream.readUnsignedShort();

        for (int i = 1; i < numEntries;) {
            ConstantPoolEntry nextEntry
                 = ConstantPoolEntry.readEntry(classStream);

            i += nextEntry.getNumEntries();

            addEntry(nextEntry);
        }
    }

    /**
     * Get the size of the constant pool.
     *
     * @return the size of the constant pool
     */
    public int size() {
        return entries.size();
    }

    /**
     * Add an entry to the constant pool.
     *
     * @param entry the new entry to be added to the constant pool.
     * @return the index into the constant pool at which the entry is
     *      stored.
     */
    public int addEntry(ConstantPoolEntry entry) {
        int index = entries.size();

        entries.add(entry);

        int numSlots = entry.getNumEntries();

        // add null entries for any additional slots required.
        for (int j = 0; j < numSlots - 1; ++j) {
            entries.add(null);
        }

        if (entry instanceof Utf8CPInfo) {
            Utf8CPInfo utf8Info = (Utf8CPInfo) entry;

            utf8Indexes.put(utf8Info.getValue(), index);
        }

        return index;
    }

    /**
     * Resolve the entries in the constant pool. Resolution of the constant
     * pool involves transforming indexes to other constant pool entries
     * into the actual data for that entry.
     */
    public void resolve() {
        for (ConstantPoolEntry poolInfo : entries) {
            if (poolInfo != null && !poolInfo.isResolved()) {
                poolInfo.resolve(this);
            }
        }
    }


    /**
     * Get an constant pool entry at a particular index.
     *
     * @param index the index into the constant pool.
     * @return the constant pool entry at that index.
     */
    public ConstantPoolEntry getEntry(int index) {
        return entries.get(index);
    }

    /**
     * Get the index of a given UTF8 constant pool entry.
     *
     * @param value the string value of the UTF8 entry.
     * @return the index at which the given string occurs in the constant
     *      pool or -1 if the value does not occur.
     */
    public int getUTF8Entry(String value) {
        int index = -1;
        Integer indexInteger = utf8Indexes.get(value);

        if (indexInteger != null) {
            index = indexInteger;
        }

        return index;
    }

    /**
     * Get the index of a given CONSTANT_CLASS entry in the constant pool.
     *
     * @param className the name of the class for which the class entry
     *      index is required.
     * @return the index at which the given class entry occurs in the
     *      constant pool or -1 if the value does not occur.
     */
    public int getClassEntry(String className) {
        int index = -1;

        final int size = entries.size();
        for (int i = 0; i < size && index == -1; ++i) {
            Object element = entries.get(i);

            if (element instanceof ClassCPInfo) {
                ClassCPInfo classinfo = (ClassCPInfo) element;

                if (classinfo.getClassName().equals(className)) {
                    index = i;
                }
            }
        }

        return index;
    }

    /**
     * Get the index of a given constant value entry in the constant pool.
     *
     * @param constantValue the constant value for which the index is
     *      required.
     * @return the index at which the given value entry occurs in the
     *      constant pool or -1 if the value does not occur.
     */
    public int getConstantEntry(Object constantValue) {
        int index = -1;

        final int size = entries.size();
        for (int i = 0; i < size && index == -1; ++i) {
            Object element = entries.get(i);

            if (element instanceof ConstantCPInfo) {
                ConstantCPInfo constantEntry = (ConstantCPInfo) element;

                if (constantEntry.getValue().equals(constantValue)) {
                    index = i;
                }
            }
        }

        return index;
    }

    /**
     * Get the index of a given CONSTANT_METHODREF entry in the constant
     * pool.
     *
     * @param methodClassName the name of the class which contains the
     *      method being referenced.
     * @param methodName the name of the method being referenced.
     * @param methodType the type descriptor of the method being referenced.
     * @return the index at which the given method ref entry occurs in the
     *      constant pool or -1 if the value does not occur.
     */
    public int getMethodRefEntry(String methodClassName, String methodName,
                                 String methodType) {
        int index = -1;

        final int size = entries.size();
        for (int i = 0; i < size && index == -1; ++i) {
            Object element = entries.get(i);

            if (element instanceof MethodRefCPInfo) {
                MethodRefCPInfo methodRefEntry = (MethodRefCPInfo) element;

                if (methodRefEntry.getMethodClassName().equals(methodClassName)
                     && methodRefEntry.getMethodName().equals(methodName)
                     && methodRefEntry.getMethodType().equals(methodType)) {
                    index = i;
                }
            }
        }

        return index;
    }

    /**
     * Get the index of a given CONSTANT_INTERFACEMETHODREF entry in the
     * constant pool.
     *
     * @param interfaceMethodClassName the name of the interface which
     *      contains the method being referenced.
     * @param interfaceMethodName the name of the method being referenced.
     * @param interfaceMethodType the type descriptor of the method being
     *      referenced.
     * @return the index at which the given method ref entry occurs in the
     *      constant pool or -1 if the value does not occur.
     */
    public int getInterfaceMethodRefEntry(String interfaceMethodClassName,
                                          String interfaceMethodName,
                                          String interfaceMethodType) {
        int index = -1;

        final int size = entries.size();
        for (int i = 0; i < size && index == -1; ++i) {
            Object element = entries.get(i);

            if (element instanceof InterfaceMethodRefCPInfo) {
                InterfaceMethodRefCPInfo interfaceMethodRefEntry
                     = (InterfaceMethodRefCPInfo) element;

                if (interfaceMethodRefEntry.getInterfaceMethodClassName().equals(
                        interfaceMethodClassName)
                     && interfaceMethodRefEntry.getInterfaceMethodName().equals(
                         interfaceMethodName)
                     && interfaceMethodRefEntry.getInterfaceMethodType().equals(
                         interfaceMethodType)) {
                    index = i;
                }
            }
        }

        return index;
    }

    /**
     * Get the index of a given CONSTANT_FIELDREF entry in the constant
     * pool.
     *
     * @param fieldClassName the name of the class which contains the field
     *      being referenced.
     * @param fieldName the name of the field being referenced.
     * @param fieldType the type descriptor of the field being referenced.
     * @return the index at which the given field ref entry occurs in the
     *      constant pool or -1 if the value does not occur.
     */
    public int getFieldRefEntry(String fieldClassName, String fieldName,
                                String fieldType) {
        int index = -1;

        final int size = entries.size();
        for (int i = 0; i < size && index == -1; ++i) {
            Object element = entries.get(i);

            if (element instanceof FieldRefCPInfo) {
                FieldRefCPInfo fieldRefEntry = (FieldRefCPInfo) element;

                if (fieldRefEntry.getFieldClassName().equals(fieldClassName)
                     && fieldRefEntry.getFieldName().equals(fieldName)
                     && fieldRefEntry.getFieldType().equals(fieldType)) {
                    index = i;
                }
            }
        }

        return index;
    }

    /**
     * Get the index of a given CONSTANT_NAMEANDTYPE entry in the constant
     * pool.
     *
     * @param name the name
     * @param type the type
     * @return the index at which the given NameAndType entry occurs in the
     *      constant pool or -1 if the value does not occur.
     */
    public int getNameAndTypeEntry(String name, String type) {
        int index = -1;

        final int size = entries.size();
        for (int i = 0; i < size && index == -1; ++i) {
            Object element = entries.get(i);

            if (element instanceof NameAndTypeCPInfo) {
                NameAndTypeCPInfo nameAndTypeEntry
                    = (NameAndTypeCPInfo) element;

                if (nameAndTypeEntry.getName().equals(name)
                     && nameAndTypeEntry.getType().equals(type)) {
                    index = i;
                }
            }
        }

        return index;
    }

    /**
     * Dump the constant pool to a string.
     *
     * @return the constant pool entries as strings
     */
    @Override
    public String toString() {
        return IntStream.range(0, entries.size())
            .mapToObj(i -> String.format("[%d] = %s", i, getEntry(i)))
            .collect(Collectors.joining("\n", "\n", "\n"));
    }

}
