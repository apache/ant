/*
 * Copyright  2001-2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs.optional.sitraka.bytecode;

import java.util.Vector;
import org.apache.tools.ant.taskdefs.optional.depend.constantpool.ConstantPool;
import org.apache.tools.ant.taskdefs.optional.depend.constantpool.Utf8CPInfo;

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
    private Utils() {
    }

    /**
     * return an UTF8 value from the pool located a a specific index.
     * @param pool the constant pool to look at
     * @param index index of the UTF8 value in the constant pool
     * @return the value of the string if it exists
     * @throws ClassCastException if the index is not an UTF8 constant.
     */
    public static String getUTF8Value(ConstantPool pool, int index) {
        return ((Utf8CPInfo) pool.getEntry(index)).getValue();
    }

    /**
     * parse all parameters from a descritor into fields of java name.
     * @param descriptor of a method.
     * @return the parameter list of a given method descriptor. Each string
     * represent a java object with its fully qualified classname or the
     * primitive name such as int, long, ...
     */
    public static String[] getMethodParams(String descriptor) {
        int i = 0;
        if (descriptor.charAt(i) != '(') {
            throw new IllegalArgumentException("Method descriptor should start with a '('");
        }
        Vector params = new Vector();
        StringBuffer param = new StringBuffer();
        i++;
        while ((i = descriptor2java(descriptor, i, param)) < descriptor.length()) {
            params.add(param.substring(0));
            param = new StringBuffer();
            if (descriptor.charAt(i) == ')') {
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
    public static String getMethodReturnType(String descriptor) {
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
    public static int descriptor2java(String descriptor, int i, StringBuffer sb) {
        // get the dimension
        StringBuffer dim = new StringBuffer();
        for (; descriptor.charAt(i) == '['; i++) {
            dim.append("[]");
        }
        // now get the type
        switch (descriptor.charAt(i)) {
            case 'B':
                sb.append("byte");
                break;
            case 'C':
                sb.append("char");
                break;
            case 'D':
                sb.append("double");
                break;
            case 'F':
                sb.append("float");
                break;
            case 'I':
                sb.append("int");
                break;
            case 'J':
                sb.append("long");
                break;
            case 'S':
                sb.append("short");
                break;
            case 'Z':
                sb.append("boolean");
                break;
            case 'V':
                sb.append("void");
                break;
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
                //throw new IllegalArgumentException("Invalid descriptor
                // symbol: '" + i + "' in '" + descriptor + "'");
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
    public static boolean isVolatile(int access_flags) {
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
        if (isPublic(access_flags)) {
            sb.append("public ");
        } else if (isPrivate(access_flags)) {
            sb.append("private ");
        } else if (isProtected(access_flags)) {
            sb.append("protected ");
        }
        if (isFinal(access_flags)) {
            sb.append("final ");
        }
        if (isStatic(access_flags)) {
            sb.append("static ");
        }
        if (isSynchronized(access_flags)) {
            sb.append("synchronized ");
        }
        if (isNative(access_flags)) {
            sb.append("native ");
        }
        if (isAbstract(access_flags)) {
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
        if (isPublic(access_flags)) {
            sb.append("public ");
        } else if (isPrivate(access_flags)) {
            sb.append("private ");
        } else if (isProtected(access_flags)) {
            sb.append("protected ");
        }
        if (isFinal(access_flags)) {
            sb.append("final ");
        }
        if (isStatic(access_flags)) {
            sb.append("static ");
        }
        if (isVolatile(access_flags)) {
            sb.append("volatile ");
        }
        if (isTransient(access_flags)) {
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
        if (isPublic(access_flags)) {
            sb.append("public ");
        } else if (isProtected(access_flags)) {
            sb.append("protected ");
        } else if (isPrivate(access_flags)) {
            sb.append("private ");
        }
        if (isFinal(access_flags)) {
            sb.append("final ");
        }
        if (isSuper(access_flags)) {
            sb.append("/*super*/ ");
        }
        if (isInterface(access_flags)) {
            sb.append("interface ");
        }
        if (isAbstract(access_flags)) {
            sb.append("abstract ");
        }
        if (isClass(access_flags)) {
            sb.append("class ");
        }
        return sb.toString().trim();
    }
}



