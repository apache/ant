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

import org.apache.tools.ant.taskdefs.optional.depend.constantpool.*;
import org.apache.tools.ant.taskdefs.optional.sitraka.bytecode.attributes.*;

/**
 * Method info structure.
 * @todo give a more appropriate name to methods.
 *
 * @author <a href="sbailliez@imediation.com">Stephane Bailliez</a>
 */
public class MethodInfo {
	protected ConstantPool constantPool;
	protected int access_flags;
	protected int name_index;
	protected int descriptor_index;
	protected Code code;
	protected boolean deprecated;
	protected boolean synthetic;
	protected Exceptions exceptions;
	public MethodInfo(ConstantPool pool){
		constantPool = pool;
	}
	
	public void read(DataInputStream dis) throws IOException {
		access_flags = dis.readShort();
		name_index = dis.readShort();
		descriptor_index = dis.readShort();
		AttributeInfoList attrlist = new AttributeInfoList(constantPool);
		attrlist.read(dis);
		code = (Code)attrlist.getAttribute(AttributeInfo.CODE);
		synthetic = attrlist.getAttribute(AttributeInfo.SYNTHETIC) != null;
		deprecated = attrlist.getAttribute(AttributeInfo.DEPRECATED) != null;
		exceptions = (Exceptions)attrlist.getAttribute(AttributeInfo.EXCEPTIONS);
	}
	
	public int getAccessFlags(){
		return access_flags;
	}
	
	public String getName(){
		return Utils.getUTF8Value(constantPool, name_index);
	}
	
	public String getDescriptor(){
		return Utils.getUTF8Value(constantPool, descriptor_index);
	}
	
	public String getFullSignature(){
		return getReturnType() + " " + getShortSignature();
	}
	
	public String getShortSignature(){
		StringBuffer buf = new StringBuffer(getName());
		buf.append("(");
		String[] params = getParametersType();
		for (int i = 0; i < params.length; i++){
			buf.append(params[i]);
			if (i != params.length - 1){
				buf.append(", ");
			}
		}
		buf.append(")");
		return buf.toString();
	}
	
	public String getReturnType(){
		return Utils.getMethodReturnType(getDescriptor());
	}
	
	public String[] getParametersType(){
		return Utils.getMethodParams(getDescriptor());
	}
	
	public Code getCode(){
		return code;
	}
	
	public int getNumberOfLines(){
		int len = -1;
		if (code != null){
			LineNumberTable lnt = code.getLineNumberTable();
			if (lnt != null){
				len = lnt.length();
			}
		}
		return len;
	}
	
	public boolean isDeprecated(){
		return deprecated;
	}
	
	public boolean isSynthetic(){
		return synthetic;
	}
    
	public String getAccess(){
		return Utils.getMethodAccess(access_flags);
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("Method: ").append(getAccess()).append(" ");
		sb.append(getFullSignature());
		sb.append(" synthetic:").append(synthetic);
		sb.append(" deprecated:").append(deprecated);
		return sb.toString();
	}
}


