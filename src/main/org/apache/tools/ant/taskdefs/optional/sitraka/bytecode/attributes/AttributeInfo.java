/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.sitraka.bytecode.attributes;

import java.io.*;

import org.apache.tools.ant.taskdefs.optional.depend.constantpool.*;
import org.apache.tools.ant.taskdefs.optional.sitraka.bytecode.*;

/**
 * Attribute info structure that provides base methods
 *
 * @author <a href="sbailliez@imediation.com">Stephane Bailliez</a>
 */
public abstract class AttributeInfo {

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
	
	protected int name_index;
	
	protected ConstantPool constantPool;
	
	protected AttributeInfo(int attr_index, ConstantPool pool){
		name_index = attr_index;
		constantPool = pool;
	}
	
	/**
	 * @param dis
	 * @throws IOException
	 */
	protected void read(DataInputStream dis) throws IOException {
		int len = dis.readInt();
		dis.skipBytes(len);
	}
	
	public String getName(){
		return Utils.getUTF8Value(constantPool, name_index);
	}
	
	/**
	 * @param attr_index
	 * @param dis
	 * @param pool
	 */
	public static AttributeInfo newAttribute(int attr_index, DataInputStream dis, ConstantPool pool) throws IOException {
		AttributeInfo attr = null;
		final String name = Utils.getUTF8Value(pool, attr_index);
		if (SOURCE_FILE.equals(name)){
			attr = new SourceFile(attr_index, pool);
		} else if (CONSTANT_VALUE.equals(name)){
			attr = new ConstantValue(attr_index, pool);
		} else if (CODE.equals(name)){
			attr = new Code(attr_index, pool);
		} else if (EXCEPTIONS.equals(name)){
			attr = new Exceptions(attr_index, pool);
		} else if (LINE_NUMBER_TABLE.equals(name)){
			attr = new LineNumberTable(attr_index, pool);
		} else if (LOCAL_VARIABLE_TABLE.equals(name)){
			attr = new LocalVariableTable(attr_index, pool);
		} else if (INNER_CLASSES.equals(name)){
			attr = new InnerClasses(attr_index, pool);
		} else if (SOURCE_DIR.equals(name)){
			attr = new SourceDir(attr_index, pool);
		} else if (SYNTHETIC.equals(name)){
			attr = new Synthetic(attr_index, pool);
		} else if (DEPRECATED.equals(name)){
			attr = new Deprecated(attr_index, pool);
		} else {
			attr = new Unknown(attr_index, pool);
		}
		attr.read(dis);
		return attr;
	}
}
