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
 * 4. The names "Ant" and "Apache Software
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
package org.apache.tools.ant.util.depend.bcel;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.EmptyVisitor;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

/**
 * A BCEL visitor implementation to collect class dependency information
 *
 * @author Conor MacNeill
 * @author <a href="mailto:hengels@innovidata.com">Holger Engels</a>
 */
public class DependencyVisitor extends EmptyVisitor {
    /** The collectd dependencies */
    private Hashtable dependencies = new Hashtable();
    /**
     * The current class's constant pool - used to determine class names
     * from class references.
     */
    private ConstantPool constantPool;

    /**
     * Get the dependencies collected by this visitor
     *
     * @return a Enumeration of classnames, being the classes upon which the
     *      visited classes depend.
     */
    public Enumeration getDependencies() {
        return dependencies.keys();
    }

    /** Clear the curretn set of collected dependencies. */
    public void clearDependencies() {
        dependencies.clear();
    }

    /**
     * Visit the constant pool of a class
     *
     * @param constantPool the constant pool of the class being visited.
     */
    public void visitConstantPool(ConstantPool constantPool) {
        this.constantPool = constantPool;
    }

    /**
     * Visit a class reference
     *
     * @param constantClass the constantClass entry for the class reference
     */
    public void visitConstantClass(ConstantClass constantClass) {
        String classname
             = constantClass.getConstantValue(constantPool).toString();
        addSlashClass(classname);
    }

    /**
     * Visit a field of the class.
     *
     * @param field the field being visited
     */
    public void visitField(Field field) {
        addClasses(field.getSignature());
    }

    /**
     * Visit a Java class
     *
     * @param javaClass the class being visited.
     */
    public void visitJavaClass(JavaClass javaClass) {
        addClass(javaClass.getClassName());
    }

    /**
     * Visit a method of the current class
     *
     * @param method the method being visited.
     */
    public void visitMethod(Method method) {
        String signature = method.getSignature();
        int pos = signature.indexOf(")");
        addClasses(signature.substring(1, pos));
        addClasses(signature.substring(pos + 1));
    }

    /**
     * Add a classname to the list of dependency classes
     *
     * @param classname the class to be added to the list of dependencies.
     */
    void addClass(String classname) {
        dependencies.put(classname, classname);
    }

    /**
     * Add all the classes from a descriptor string.
     *
     * @param string the descriptor string, being descriptors separated by
     *      ';' characters.
     */
    private void addClasses(String string) {
        StringTokenizer tokens = new StringTokenizer(string, ";");
        while (tokens.hasMoreTokens()) {
            String descriptor = tokens.nextToken();
            int pos = descriptor.indexOf('L');
            if (pos != -1) {
                addSlashClass(descriptor.substring(pos + 1));
            }
        }
    }

    /**
     * Adds a class name in slash format 
     * (for example org/apache/tools/ant/Main).
     *
     * @param classname the class name in slash format
     */
    private void addSlashClass(String classname) {
        addClass(classname.replace('/', '.'));
    }
}

