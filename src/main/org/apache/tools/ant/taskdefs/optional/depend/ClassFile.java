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
package org.apache.tools.ant.taskdefs.optional.depend;

import java.io.*;
import java.lang.reflect.Modifier;
import java.util.*;
import org.apache.tools.ant.taskdefs.optional.depend.constantpool.*;

/**
 * A ClassFile object stores information about a Java class.
 * 
 * The class may be read from a DataInputStream.and written
 * to a DataOutputStream. These are usually streams from a Java
 * class file or a class file component of a Jar file.
 * 
 * @author Conor MacNeill
 */
public class ClassFile {

    /**
     * The Magic Value that marks the start of a Java class file
     */
    static private final int CLASS_MAGIC = 0xCAFEBABE;


    /**
     * This class' constant pool.
     */
    private ConstantPool constantPool;


    /**
     * The class name for this class.
     */
    private String className;

    /**
     * Read the class from a data stream.
     * 
     * This method takes an InputStream as input and
     * parses the class from the stream.
     * <p>
     * 
     * @param stream an InputStream from which the class will be read
     * 
     * @throws IOException if there is a problem reading from the given stream.
     * @throws ClassFormatError if the class cannot be parsed correctly
     * 
     */
    public void read(InputStream stream) throws IOException, ClassFormatError {
        DataInputStream classStream = new DataInputStream(stream);


        if (classStream.readInt() != CLASS_MAGIC) {
            throw new ClassFormatError("No Magic Code Found - probably not a Java class file.");
        } 

        // right we have a good looking class file.
        int minorVersion = classStream.readUnsignedShort();
        int majorVersion = classStream.readUnsignedShort();

        // read the constant pool in and resolve it
        constantPool = new ConstantPool();

        constantPool.read(classStream);
        constantPool.resolve();

        int accessFlags = classStream.readUnsignedShort();
        int thisClassIndex = classStream.readUnsignedShort();
        int superClassIndex = classStream.readUnsignedShort();
        className = ((ClassCPInfo) constantPool.getEntry(thisClassIndex)).getClassName();
    } 


    /**
     * Get the classes which this class references.
     */
    public Vector getClassRefs() {

        Vector classRefs = new Vector();

        for (int i = 0; i < constantPool.size(); ++i) {
            ConstantPoolEntry entry = constantPool.getEntry(i);

            if (entry != null && entry.getTag() == ConstantPoolEntry.CONSTANT_Class) {
                ClassCPInfo classEntry = (ClassCPInfo) entry;

                if (!classEntry.getClassName().equals(className)) {
                    classRefs.addElement(ClassFileUtils.convertSlashName(classEntry.getClassName()));
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

