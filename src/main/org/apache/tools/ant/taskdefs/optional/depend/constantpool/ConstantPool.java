/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.tools.ant.taskdefs.optional.depend.constantpool;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The constant pool of a Java class. The constant pool is a collection of
 * constants used in a Java class file. It stores strings, constant values,
 * class names, method names, field names etc.
 *
 * @author Conor MacNeill
 * @see <a href="http://java.sun.com/docs/books/vmspec/">The Java Virtual
 *      Machine Specification</a>
 */
public class ConstantPool {

    /** The entries in the constant pool. */
    private Vector entries;

    /**
     * A Hashtable of UTF8 entries - used to get constant pool indexes of
     * the UTF8 values quickly
     */
    private Hashtable utf8Indexes;

    /** Initialise the constant pool. */
    public ConstantPool() {
        entries = new Vector();

        // The zero index is never present in the constant pool itself so
        // we add a null entry for it
        entries.addElement(null);

        utf8Indexes = new Hashtable();
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

        entries.addElement(entry);

        int numSlots = entry.getNumEntries();

        // add null entries for any additional slots required.
        for (int j = 0; j < numSlots - 1; ++j) {
            entries.addElement(null);
        }

        if (entry instanceof Utf8CPInfo) {
            Utf8CPInfo utf8Info = (Utf8CPInfo) entry;

            utf8Indexes.put(utf8Info.getValue(), new Integer(index));
        }

        return index;
    }

    /**
     * Resolve the entries in the constant pool. Resolution of the constant
     * pool involves transforming indexes to other constant pool entries
     * into the actual data for that entry.
     */
    public void resolve() {
        for (Enumeration i = entries.elements(); i.hasMoreElements();) {
            ConstantPoolEntry poolInfo = (ConstantPoolEntry) i.nextElement();

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
        return (ConstantPoolEntry) entries.elementAt(index);
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
        Integer indexInteger = (Integer) utf8Indexes.get(value);

        if (indexInteger != null) {
            index = indexInteger.intValue();
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

        for (int i = 0; i < entries.size() && index == -1; ++i) {
            Object element = entries.elementAt(i);

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

        for (int i = 0; i < entries.size() && index == -1; ++i) {
            Object element = entries.elementAt(i);

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
     * @param methodType the type descriptor of the metho dbeing referenced.
     * @return the index at which the given method ref entry occurs in the
     *      constant pool or -1 if the value does not occur.
     */
    public int getMethodRefEntry(String methodClassName, String methodName,
                                 String methodType) {
        int index = -1;

        for (int i = 0; i < entries.size() && index == -1; ++i) {
            Object element = entries.elementAt(i);

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
     * @param interfaceMethodType the type descriptor of the metho dbeing
     *      referenced.
     * @return the index at which the given method ref entry occurs in the
     *      constant pool or -1 if the value does not occur.
     */
    public int getInterfaceMethodRefEntry(String interfaceMethodClassName,
                                          String interfaceMethodName,
                                          String interfaceMethodType) {
        int index = -1;

        for (int i = 0; i < entries.size() && index == -1; ++i) {
            Object element = entries.elementAt(i);

            if (element instanceof InterfaceMethodRefCPInfo) {
                InterfaceMethodRefCPInfo interfaceMethodRefEntry
                     = (InterfaceMethodRefCPInfo) element;

                if (interfaceMethodRefEntry.getInterfaceMethodClassName().equals(interfaceMethodClassName)
                     && interfaceMethodRefEntry.getInterfaceMethodName().equals(interfaceMethodName)
                     && interfaceMethodRefEntry.getInterfaceMethodType().equals(interfaceMethodType)) {
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

        for (int i = 0; i < entries.size() && index == -1; ++i) {
            Object element = entries.elementAt(i);

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

        for (int i = 0; i < entries.size() && index == -1; ++i) {
            Object element = entries.elementAt(i);

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
    public String toString() {
        StringBuffer sb = new StringBuffer("\n");
        int size = entries.size();

        for (int i = 0; i < size; ++i) {
            sb.append("[" + i + "] = " + getEntry(i) + "\n");
        }

        return sb.toString();
    }

}

