/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights 
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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

import java.io.*;
import java.util.*;

/**
 * An entry in the constant pool.
 * 
 * This class contains a represenation of the constant pool entries. It is
 * an abstract base class for all the different forms of constant pool entry.
 * 
 * @author Conor MacNeill
 * @see ConstantPool
 */
public abstract class ConstantPoolEntry {

    /**
     * Tag value for UTF8 entries.
     */
    static public final int CONSTANT_Utf8 = 1;

    /**
     * Tag value for Integer entries.
     */
    static public final int CONSTANT_Integer = 3;

    /**
     * Tag value for Float entries.
     */
    static public final int CONSTANT_Float = 4;

    /**
     * Tag value for Long entries.
     */
    static public final int CONSTANT_Long = 5;

    /**
     * Tag value for Double entries.
     */
    static public final int CONSTANT_Double = 6;

    /**
     * Tag value for Class entries.
     */
    static public final int CONSTANT_Class = 7;

    /**
     * Tag value for String entries.
     */
    static public final int CONSTANT_String = 8;

    /**
     * Tag value for Field Reference entries.
     */
    static public final int CONSTANT_FieldRef = 9;

    /**
     * Tag value for Method Reference entries.
     */
    static public final int CONSTANT_MethodRef = 10;

    /**
     * Tag value for Interface Method Reference entries.
     */
    static public final int CONSTANT_InterfaceMethodRef = 11;

    /**
     * Tag value for Name and Type entries.
     */
    static public final int CONSTANT_NameAndType = 12;

    /**
     * This entry's tag which identifies the type of this constant pool entry.
     */
    private int             tag;

    /**
     * The number of slots in the constant pool, occupied by this entry.
     */
    private int             numEntries;

    /**
     * A flag which indiciates if this entry has been resolved or not.
     */
    private boolean         resolved;

    /**
     * Initialse the constant pool entry.
     * 
     * @param tagValue the tag value which identifies which type of constant pool entry this is.
     * @param entries the number of constant pool entry slots this entry occupies.
     */
    public ConstantPoolEntry(int tagValue, int entries) {
        tag = tagValue;
        numEntries = entries;
        resolved = false;
    }

    /**
     * Read a constant pool entry from a stream.
     * 
     * This is a factory method which reads a constant pool entry
     * form a stream and returns the appropriate subclass for the
     * entry.
     * 
     * @param cpStream the stream from which the constant pool entry is to be read.
     * 
     * @returns the appropriate ConstantPoolEntry subclass representing the
     * constant pool entry from the stream.
     * 
     * @throws IOExcception if there is a problem reading the entry from the stream.
     */
    static public ConstantPoolEntry readEntry(DataInputStream cpStream) throws IOException {
        ConstantPoolEntry cpInfo = null;
        int               cpTag = cpStream.readUnsignedByte();

        switch (cpTag) {

        case CONSTANT_Utf8:
            cpInfo = new Utf8CPInfo();

            break;

        case CONSTANT_Integer:
            cpInfo = new IntegerCPInfo();

            break;

        case CONSTANT_Float:
            cpInfo = new FloatCPInfo();

            break;

        case CONSTANT_Long:
            cpInfo = new LongCPInfo();

            break;

        case CONSTANT_Double:
            cpInfo = new DoubleCPInfo();

            break;

        case CONSTANT_Class:
            cpInfo = new ClassCPInfo();

            break;

        case CONSTANT_String:
            cpInfo = new StringCPInfo();

            break;

        case CONSTANT_FieldRef:
            cpInfo = new FieldRefCPInfo();

            break;

        case CONSTANT_MethodRef:
            cpInfo = new MethodRefCPInfo();

            break;

        case CONSTANT_InterfaceMethodRef:
            cpInfo = new InterfaceMethodRefCPInfo();

            break;

        case CONSTANT_NameAndType:
            cpInfo = new NameAndTypeCPInfo();

            break;

        default:
            throw new ClassFormatError("Invalid Constant Pool entry Type " + cpTag);
        }

        cpInfo.read(cpStream);

        return cpInfo;
    } 

    /**
     * Indicates whether this entry has been resolved.
     * 
     * In general a constant pool entry can reference another constant
     * pool entry by its index value. Resolution involves replacing this
     * index value with the constant pool entry at that index.
     * 
     * @return true if this entry has been resolved.
     */
    public boolean isResolved() {
        return resolved;
    } 

    /**
     * Resolve this constant pool entry with respect to its dependents in
     * the constant pool.
     * 
     * @param constantPool the constant pool of which this entry is a member
     * and against which this entry is to be resolved.
     */
    public void resolve(ConstantPool constantPool) {
        resolved = true;
    } 

    /**
     * read a constant pool entry from a class stream.
     * 
     * @param cpStream the DataInputStream which contains the constant pool entry to be read.
     * 
     * @throws IOException if there is a problem reading the entry from the stream.
     */
    public abstract void read(DataInputStream cpStream) throws IOException;

    /**
     * Get the Entry's type tag.
     * 
     * @return The Tag value of this entry
     */
    public int getTag() {
        return tag;
    } 

    /**
     * Get the number of Constant Pool Entry slots within the constant pool occupied by this entry.
     * 
     * @return the number of slots used.
     * 
     */
    public final int getNumEntries() {
        return numEntries;
    } 

}

