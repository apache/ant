/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.util.depend;

import java.io.File;
import java.util.Set;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.Collection;
import org.apache.bcel.classfile.EmptyVisitor;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantInterfaceMethodref;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.ConstantValue;
import org.apache.bcel.classfile.Deprecated;
import org.apache.bcel.classfile.ExceptionTable;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.InnerClass;
import org.apache.bcel.classfile.InnerClasses;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.LineNumber;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.SourceFile;
import org.apache.bcel.classfile.Synthetic;
import org.apache.bcel.classfile.Unknown;
import org.apache.bcel.classfile.StackMap;
import org.apache.bcel.classfile.StackMapEntry;
import org.apache.bcel.classfile.ClassParser;



public class Dependencies extends EmptyVisitor {
    private boolean verbose = false;

    private JavaClass javaClass;
    private ConstantPool constantPool;
    private Set dependencies = new HashSet();

    public void clearDependencies() {
        dependencies.clear();
    }
    
    public Set getDependencies() {
        return dependencies;
    }

    public void visitConstantClass(ConstantClass obj) {
        if (verbose) {
            System.out.println("visit ConstantClass");
            System.out.println(obj.getConstantValue(constantPool));
        }
        dependencies.add("" + obj.getConstantValue(constantPool));
    }
    
    public void visitConstantPool(ConstantPool obj) {
        if (verbose) {
            System.out.println("visit ConstantPool");
        }
        this.constantPool = obj;

        // visit constants
        for(int idx = 0; idx < constantPool.getLength(); idx++) {
            Constant c = constantPool.getConstant(idx);
            if (c != null) {
                c.accept(this);
            }
        }
    }
    
    public void visitField(Field obj) {
        if (verbose) {
            System.out.println("visit Field");
            System.out.println(obj.getSignature());
        }
        addClasses(obj.getSignature());
    }

    public void visitJavaClass(JavaClass obj) {
        if (verbose) {
            System.out.println("visit JavaClass");
        }
        
        this.javaClass = obj;
        dependencies.add(javaClass.getClassName().replace('.', '/'));

        // visit constant pool
        javaClass.getConstantPool().accept(this);

        // visit fields
        Field[] fields = obj.getFields();
        for(int i=0; i < fields.length; i++) {
            fields[i].accept(this);
        }

        // visit methods
        Method[] methods = obj.getMethods();
        for(int i=0; i < methods.length; i++) {
            methods[i].accept(this);
        }
    }
    
    public void visitMethod(Method obj) {
        if (verbose) {
            System.out.println("visit Method");
            System.out.println(obj.getSignature());
        }
        String signature = obj.getSignature();
        int pos = signature.indexOf(")");
        addClasses(signature.substring(1, pos));
        addClasses(signature.substring(pos + 1));
    }
    
    void addClasses(String string) {
        StringTokenizer tokens = new StringTokenizer(string, ";");
        while (tokens.hasMoreTokens()) {
            addClass(tokens.nextToken());
        }
    }

    void addClass(String string) {
        int pos = string.indexOf('L');
        if (pos != -1) {
            dependencies.add(string.substring(pos+1));
        }
    }

    public static void main(String[] args) {
        try {
            Dependencies visitor = new Dependencies();

            Set set = new TreeSet();
            Set newSet = new HashSet();

            int o=0;
            String arg = null;
            if ("-base".equals(args[0])) {
                arg = args[1];
                if (!arg.endsWith(File.separator)) {
                    arg = arg + File.separator;
                }
                o=2;
            }
            final String base = arg;

            for (int i=o; i < args.length; i++) {
                String fileName = args[i].substring(0, args[i].length() - ".class".length());
                if (base != null && fileName.startsWith(base)) {
                    fileName = fileName.substring(base.length());
                }
                newSet.add(fileName);
            }
            set.addAll(newSet);

            do {
                Iterator i = newSet.iterator();
                while (i.hasNext()) {
                    String fileName = i.next() + ".class";

                    if (base != null) {
                        fileName = base + fileName;
                    }

                    JavaClass javaClass = new ClassParser(fileName).parse();
                    javaClass.accept(visitor);
                }
                newSet.clear();
                newSet.addAll(visitor.getDependencies());
                visitor.clearDependencies();

                applyFilter(newSet, new Filter() {
                        public boolean accept(Object object) {
                            String fileName = object + ".class";
                            if (base != null) {
                                fileName = base + fileName;
                            }
                            return new File(fileName).exists();
                        }
                    });
                newSet.removeAll(set);
                set.addAll(newSet);
            }
            while (newSet.size() > 0);

            Iterator i = set.iterator();
            while (i.hasNext()) {
                System.out.println(i.next());
            }
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    public static void applyFilter(Collection collection, Filter filter) {
        Iterator i = collection.iterator();
        while (i.hasNext()) {
            Object next = i.next();
            if (!filter.accept(next)) {
                i.remove();
            }
        }
    }
}
