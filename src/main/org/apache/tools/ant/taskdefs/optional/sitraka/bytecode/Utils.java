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

import org.apache.tools.ant.taskdefs.optional.depend.constantpool.*;
import java.util.Vector;

/**
 * Utilities mostly to manipulate methods and access flags.
 *
 * @author <a href="sbailliez@imediation.com">Stephane Bailliez</a>
 */
public class Utils {
	/** public access flag */
    public static final short ACC_PUBLIC = 1;
	/** private access flag */
    public static final short ACC_PRIVATE = 2;
	/** protected access flag */
    public static final short ACC_PROTECTED = 4;
	/** static access flag */
    public static final short ACC_STATIC = 8;
	/** final access flag */
    public static final short ACC_FINAL = 16;
	/** super access flag */
    public static final short ACC_SUPER = 32;
	/** synchronized access flag */
    public static final short ACC_SYNCHRONIZED = 32;
	/** volatile access flag */
    public static final short ACC_VOLATILE = 64;
	/** transient access flag */
    public static final short ACC_TRANSIENT = 128;
	/** native access flag */
    public static final short ACC_NATIVE = 256;
	/** interface access flag */
    public static final short ACC_INTERFACE = 512;
	/** abstract access flag */
    public static final short ACC_ABSTRACT = 1024;
	/** strict access flag */
    public static final short ACC_STRICT = 2048;

	/** private constructor */
	private Utils(){
	}
	
	/**
	 * return an UTF8 value from the pool located a a specific index.
	 * @param pool the constant pool to look at
	 * @param index index of the UTF8 value in the constant pool
	 * @return the value of the string if it exists
	 * @throws ClassCastException if the index is not an UTF8 constant.
	 */
	public static String getUTF8Value(ConstantPool pool, int index){
		return ((Utf8CPInfo)pool.getEntry(index)).getValue();
	}
	
	/**
	 * parse all parameters from a descritor into fields of java name.
	 * @param descriptor of a method.
	 * @return the parameter list of a given method descriptor. Each string
	 * represent a java object with its fully qualified classname or the
	 * primitive name such as int, long, ...
	 */
	public static String[] getMethodParams(String descriptor){
		int i = 0;
		if (descriptor.charAt(i) != '('){
			throw new IllegalArgumentException("Method descriptor should start with a '('");
		}
		Vector params = new Vector();
		StringBuffer param = new StringBuffer();
        i++;
        while ( (i = descriptor2java(descriptor, i, param)) < descriptor.length() ){
			params.add(param.toString());
			param.setLength(0); // reset
			if (descriptor.charAt(i) == ')'){
				i++;
				break;
			}
		}
		String[] array = new String[params.size()];
		params.copyInto(array);
		return array;
	}
	
	/**
	 * return the object type of a return type.
	 * @param descriptor
	 * @return get the return type objet of a given descriptor
	 */
	public static String getMethodReturnType(String descriptor){
		int pos = descriptor.indexOf(')');
		StringBuffer rettype = new StringBuffer();
		descriptor2java(descriptor, pos + 1, rettype);
		return rettype.toString();
    }
	
	/**
	 * Parse a single descriptor symbol and returns it java equivalent.
	 * @param descriptor the descriptor symbol.
	 * @param i the index to look at the symbol in the descriptor string
	 * @param sb the stringbuffer to return the java equivalent of the symbol
	 * @return the index after the descriptor symbol
	 */
	public static int descriptor2java(String descriptor, int i, StringBuffer sb){
		// get the dimension
		StringBuffer dim = new StringBuffer();
		for (;descriptor.charAt(i) == '['; i++){
			dim.append("[]");
		}
		// now get the type
		switch (descriptor.charAt(i)){
			case 'B': sb.append("byte"); break;
			case 'C': sb.append("char"); break;
			case 'D': sb.append("double"); break;
			case 'F': sb.append("float"); break;
			case 'I': sb.append("int"); break;
			case 'J': sb.append("long"); break;
			case 'S': sb.append("short"); break;
			case 'Z': sb.append("boolean"); break;
			case 'V': sb.append("void"); break;
			case 'L':
				// it is a class
				int pos = descriptor.indexOf(';', i + 1);
				String classname = descriptor.substring(i + 1, pos).replace('/', '.');
				sb.append(classname);
				i = pos;
				break;
			default:
				//@todo, yeah this happens because I got things like:
				// ()Ljava/lang/Object; and it will return and ) will be here
				// think about it.
				
				//ooooops should never happen
				//throw new IllegalArgumentException("Invalid descriptor symbol: '" + i + "' in '" + descriptor + "'");
		}
		sb.append(dim.toString());
		return ++i;
	}
	
	/**
	 * check for abstract access
	 * @param access_flags access flags
	 */
    public static boolean isAbstract(int access_flags) {
        return (access_flags & ACC_ABSTRACT) != 0;
    }
	/**
	 * check for public access
	 * @param access_flags access flags
	 */
    public static boolean isPublic(int access_flags) {
        return (access_flags & ACC_PUBLIC) != 0;
    }
	/**
	 * check for a static access
	 * @param access_flags access flags
	 */
    public static boolean isStatic(int access_flags) {
        return (access_flags & ACC_STATIC) != 0;
    }
	/**
	 *  check for native access
	 * @param access_flags access flags
	 */
    public static boolean isNative(int access_flags) {
        return (access_flags & ACC_NATIVE) != 0;
    }
	/**
	 * check for class access
	 * @param access_flags access flags
	 */
    public static boolean isClass(int access_flags) {
        return !isInterface(access_flags);
    }
	/**
	 * check for strict access
	 * @param access_flags access flags
	 */
    public static boolean isStrict(int access_flags) {
        return (access_flags & ACC_STRICT) != 0;
    }
	/**
	 * check for interface access
	 * @param access_flags access flags
	 */
    public static boolean isInterface(int access_flags) {
        return (access_flags & ACC_INTERFACE) != 0;
    }
	/**
	 * check for private access
	 * @param access_flags access flags
	 */
    public static boolean isPrivate(int access_flags) {
        return (access_flags & ACC_PRIVATE) != 0;
    }
	/**
	 * check for transient flag
	 * @param access_flags access flags
	 */
    public static boolean isTransient(int access_flags) {
        return (access_flags & ACC_TRANSIENT) != 0;
    }
	/**
	 * check for volatile flag
	 * @param access_flags access flags
	 */
    public static boolean isVolatile(int access_flags){
        return (access_flags & ACC_VOLATILE) != 0;
    }
	/**
	 * check for super flag
	 * @param access_flags access flag
	 */
    public static boolean isSuper(int access_flags) {
        return (access_flags & ACC_SUPER) != 0;
    }
	/**
	 * check for protected flag
	 * @param access_flags access flags
	 */
    public static boolean isProtected(int access_flags) {
        return (access_flags & ACC_PROTECTED) != 0;
    }
	/**
	 * chck for final flag
	 * @param access_flags access flags
	 */
    public static boolean isFinal(int access_flags) {
        return (access_flags & ACC_FINAL) != 0;
    }
	/**
	 * check for synchronized flag
	 * @param access_flags access flags
	 */
    public static boolean isSynchronized(int access_flags) {
        return (access_flags & ACC_SYNCHRONIZED) != 0;
    }
	
	/**
	 * return the method access flag as java modifiers
	 * @param access_flags access flags
	 * @return the access flags as modifier strings
	 */
    public static String getMethodAccess(int access_flags) {
        StringBuffer sb = new StringBuffer();
        if(isPublic(access_flags)){
            sb.append("public ");
		} else if(isPrivate(access_flags)){
            sb.append("private ");
		} else if(isProtected(access_flags)){
            sb.append("protected ");
		}
		if(isFinal(access_flags)){
            sb.append("final ");
		}
        if(isStatic(access_flags)){
            sb.append("static ");
		}
        if(isSynchronized(access_flags)){
            sb.append("synchronized ");
		}
        if(isNative(access_flags)){
            sb.append("native ");
		}
        if(isAbstract(access_flags)){
            sb.append("abstract ");
		}
        return sb.toString().trim();
    }

	/**
	 * return the field access flag as java modifiers
	 * @param access_flags access flags
	 * @return the access flags as modifier strings
	 */
    public static String getFieldAccess(int access_flags) {
        StringBuffer sb = new StringBuffer();
        if(isPublic(access_flags)){
            sb.append("public ");
		} else if(isPrivate(access_flags)){
            sb.append("private ");
		} else if (isProtected(access_flags)){
            sb.append("protected ");
		}
        if(isFinal(access_flags)){
            sb.append("final ");
		}
        if(isStatic(access_flags)){
            sb.append("static ");
		}
        if(isVolatile(access_flags)){
            sb.append("volatile ");
		}
        if(isTransient(access_flags)){
            sb.append("transient ");
		}
        return sb.toString().trim();
    }

	/**
	 * return the class access flag as java modifiers
	 * @param access_flags access flags
	 * @return the access flags as modifier strings
	 */
    public static String getClassAccess(int access_flags) {
        StringBuffer sb = new StringBuffer();
        if(isPublic(access_flags)){
            sb.append("public ");
		} else if (isProtected(access_flags)){
			sb.append("protected ");
		} else if (isPrivate(access_flags)){
			sb.append("private ");
		}
		if(isFinal(access_flags)){
            sb.append("final ");
		}
        if(isSuper(access_flags)){
            sb.append("/*super*/ ");
		}
        if(isInterface(access_flags)){
            sb.append("interface ");
		}
        if(isAbstract(access_flags)){
            sb.append("abstract ");
		}
        if(isClass(access_flags)){
            sb.append("class ");
		}
        return sb.toString().trim();
    }
}



