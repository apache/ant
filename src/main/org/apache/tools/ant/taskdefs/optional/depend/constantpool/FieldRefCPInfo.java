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
 * A FieldRef CP Info
 * 
 * @author Conor MacNeill
 */
public class FieldRefCPInfo extends ConstantPoolEntry {
    private String fieldClassName;
    private String fieldName;
    private String fieldType;
    private int    classIndex;
    private int    nameAndTypeIndex;

    /**
     * Constructor.
     * 
     */
    public FieldRefCPInfo() {
        super(CONSTANT_FieldRef, 1);
    }

    /**
     * read a constant pool entry from a class stream.
     * 
     * @param cpStream the DataInputStream which contains the constant pool entry to be read.
     * 
     * @throws IOException if there is a problem reading the entry from the stream.
     */
    public void read(DataInputStream cpStream) throws IOException {
        classIndex = cpStream.readUnsignedShort();
        nameAndTypeIndex = cpStream.readUnsignedShort();
    } 

    /**
     * Resolve this constant pool entry with respect to its dependents in
     * the constant pool.
     * 
     * @param constantPool the constant pool of which this entry is a member
     * and against which this entry is to be resolved.
     */
    public void resolve(ConstantPool constantPool) {
        ClassCPInfo fieldClass = (ClassCPInfo) constantPool.getEntry(classIndex);

        fieldClass.resolve(constantPool);

        fieldClassName = fieldClass.getClassName();

        NameAndTypeCPInfo nt = (NameAndTypeCPInfo) constantPool.getEntry(nameAndTypeIndex);

        nt.resolve(constantPool);

        fieldName = nt.getName();
        fieldType = nt.getType();

        super.resolve(constantPool);
    } 

    /**
     * Print a readable version of the constant pool entry.
     * 
     * @return the string representation of this constant pool entry.
     */
    public String toString() {
        String value;

        if (isResolved()) {
            value = "Field : Class = " + fieldClassName + ", name = " + fieldName + ", type = " + fieldType;
        } else {
            value = "Field : Class index = " + classIndex + ", name and type index = " + nameAndTypeIndex;
        } 

        return value;
    } 

    public String getFieldClassName() {
        return fieldClassName;
    } 

    public String getFieldName() {
        return fieldName;
    } 

    public String getFieldType() {
        return fieldType;
    } 

}

