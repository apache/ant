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
package org.apache.tools.ant.taskdefs.optional.sitraka.bytecode;

import java.io.*;

import org.apache.tools.ant.taskdefs.optional.depend.constantpool.ConstantPool;
import org.apache.tools.ant.taskdefs.optional.depend.constantpool.ClassCPInfo;
import org.apache.tools.ant.taskdefs.optional.sitraka.bytecode.attributes.*;


/**
 * Object representing a class.
 *
 * @author <a href="sbailliez@imediation.com">Stephane Bailliez</a>
 */
public class ClassFile {
	
	protected ConstantPool constantPool;
	
	protected InterfaceList interfaces;
	
	protected FieldInfoList fields;
	
	protected MethodInfoList methods;
		
	protected String sourceDir;
	
	protected String sourceFile;
	
	protected int access_flags;
	
	protected int this_class;
	
	protected int super_class;
	
	protected boolean isSynthetic;
	
	protected boolean isDeprecated;
	
	public ClassFile(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		constantPool = new ConstantPool();
			
		int magic = dis.readInt(); // 0xCAFEBABE
		int minor = dis.readShort();
		int major = dis.readShort();
				
		constantPool.read(dis);
		constantPool.resolve();
		
		access_flags = dis.readShort();
		this_class = dis.readShort();
		super_class = dis.readShort();
		
		interfaces = new InterfaceList(constantPool);
		interfaces.read(dis);
		//System.out.println(interfaces.toString());
		
		fields = new FieldInfoList(constantPool);
		fields.read(dis);
		//System.out.println(fields.toString());
		
		methods = new MethodInfoList(constantPool);
		methods.read(dis);
		//System.out.println(methods.toString());
		
		AttributeInfoList attributes = new AttributeInfoList(constantPool);
		attributes.read(dis);
		SourceFile srcFile = (SourceFile)attributes.getAttribute(AttributeInfo.SOURCE_FILE);
		if (srcFile != null){
			sourceFile = srcFile.getValue();
		}
		SourceDir srcDir = (SourceDir)attributes.getAttribute(AttributeInfo.SOURCE_DIR);
		if (srcDir != null){
			sourceDir = srcDir.getValue();
		}
		isSynthetic = attributes.getAttribute(AttributeInfo.SYNTHETIC) != null;
		isDeprecated = attributes.getAttribute(AttributeInfo.DEPRECATED) != null;
	}
	
	public int getAccess(){
		return access_flags;
	}
	public InterfaceList getInterfaces(){
		return interfaces;
	}
	public String getSourceFile(){
		return sourceFile;
	}
	public String getSourceDir(){
		return sourceDir;
	}
	public boolean isSynthetic() {
		return isSynthetic;
	}
	public boolean isDeprecated() {
		return isDeprecated;
	}
	public MethodInfoList getMethods(){
		return methods;
	}
	public FieldInfoList getFields(){
		return fields;
	}
	public String getSuperName(){
		return Utils.getUTF8Value(constantPool, super_class);
	}
	public String getFullName(){
		return ((ClassCPInfo)constantPool.getEntry(this_class)).getClassName().replace('/','.');
	}
	public String getName(){
		String name = getFullName();
		int pos = name.lastIndexOf('.');
		if (pos == -1){
			return "";
		}
		return name.substring(pos + 1);
	}
	public String getPackage(){
		String name = getFullName();
		int pos = name.lastIndexOf('.');
		if (pos == -1){
			return "";
		}
		return name.substring(0, pos);
	}
	
	/** dirty test method, move it into a testcase */
	public static void main(String[] args) throws Exception {
		System.out.println("loading classfile...");
		InputStream is = ClassLoader.getSystemResourceAsStream("java/util/Vector.class");
		ClassFile clazzfile = new ClassFile(is);
		System.out.println("Class name: " + clazzfile.getName());
		MethodInfoList methods = clazzfile.getMethods();
		for (int i = 0; i < methods.length(); i++){
			MethodInfo method = methods.getMethod(i);
			System.out.println("Method: " + method.getFullSignature());
			System.out.println("line: " +  method.getNumberOfLines());
			LineNumberTable lnt = method.getCode().getLineNumberTable();
		}
	}
	
}


 


