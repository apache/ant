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
package org.apache.tools.ant.util.depend;

import java.io.*;
import java.util.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.*;


public class Dependencies implements Visitor {
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

    public void visitCode(Code obj) {}
    public void visitCodeException(CodeException obj) {}
    
    public void visitConstantClass(ConstantClass obj) {
        if (verbose) {
            System.out.println("visit ConstantClass");
            System.out.println(obj.getConstantValue(constantPool));
        }
        dependencies.add("" + obj.getConstantValue(constantPool));
    }
    
    public void visitConstantDouble(ConstantDouble obj) {}
    public void visitConstantFieldref(ConstantFieldref obj) {}
    public void visitConstantFloat(ConstantFloat obj) {}
    public void visitConstantInteger(ConstantInteger obj) {}
    public void visitConstantInterfaceMethodref(ConstantInterfaceMethodref obj) {}
    public void visitConstantLong(ConstantLong obj) {}
    public void visitConstantMethodref(ConstantMethodref obj) {}
    public void visitConstantNameAndType(ConstantNameAndType obj) {}
    
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
    public void visitConstantString(ConstantString obj) {}
    public void visitConstantUtf8(ConstantUtf8 obj) {}
    public void visitConstantValue(ConstantValue obj) {}
    public void visitDeprecated(Deprecated obj) {}
    public void visitExceptionTable(ExceptionTable obj) {}
    
    public void visitField(Field obj) {
        if (verbose) {
            System.out.println("visit Field");
            System.out.println(obj.getSignature());
        }
        addClasses(obj.getSignature());
    }

    public void visitInnerClass(InnerClass obj) {}
    public void visitInnerClasses(InnerClasses obj) {}
    
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
    public void visitLineNumber(LineNumber obj) {}
    public void visitLineNumberTable(LineNumberTable obj) {}
    public void visitLocalVariable(LocalVariable obj) {}
    public void visitLocalVariableTable(LocalVariableTable obj) {}
    
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
    
    public void visitSourceFile(SourceFile obj) {}
    public void visitSynthetic(Synthetic obj) {}
    public void visitUnknown(Unknown obj) {}
    public void visitStackMap(StackMap obj) {}
    public void visitStackMapEntry(StackMapEntry obj) {}

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
