/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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
package org.apache.ant.xdoclet;

import java.util.Collection;
import java.util.Iterator;
import xjavadoc.XClass;
import xjavadoc.XJavaDoc;
import xjavadoc.XMethod;

import xdoclet.TemplateSubTask;
import xdoclet.XDocletException;
import xdoclet.util.TypeConversionUtil;

/**
 * @created   January 5, 2003
 */
public abstract class AntSubTask extends TemplateSubTask
{

    /**
     * Checks many factors to determine if the class is indeed an Ant task or not.
     *
     * @param clazz
     * @return
     * @exception XDocletException
     * @todo                        perhaps make deprecation switch configurable
     */
    public final static boolean isAntTask(XClass clazz) throws XDocletException
    {
        if (clazz.isAbstract()) {
            return false;
        }

        // no inner classes (for now - but is this possible? desired?)
        if (clazz.isInner()) {
            return false;
        }

        String ignoreValue = clazz.getDoc().getTagAttributeValue("ant.task", "ignore");
        boolean ignore = TypeConversionUtil.stringToBoolean(ignoreValue, false);

        if (ignore) {
            return false;
        }

        /*
         * Tag[] tags = clazz.tags();
         * for (int i = 0; i < tags.length; i++) {
         * if ("@deprecated".equals(tags[i].name())) {
         * return false;
         * }
         * }
         */
        if (hasExecuteMethod(clazz)) {
            return true;
        }

        return false;
    }

    /**
     * Check for class implementing an execute() method. Recursive calls are made to superclasses.
     *
     * @param clazz
     * @return
     */
    private static boolean hasExecuteMethod(XClass clazz)
    {
        if (clazz == null) {
            return false;
        }

        // It ain't a task if we've climbed back to Task itself.
        // Also ignore other special Ant classes
        if ("org.apache.tools.ant.Task".equals(clazz.getQualifiedName()) ||
            "org.apache.tools.ant.Target".equals(clazz.getQualifiedName()) ||
            "org.apache.tools.ant.TaskAdapter".equals(clazz.getQualifiedName()) ||
            "org.apache.tools.ant.UnknownElement".equals(clazz.getQualifiedName())) {
            return false;
        }

        // need to check that only runtime exceptions are thrown?
        Collection methods = clazz.getMethods(true);
        Iterator iter = methods.iterator();

        while (iter.hasNext()) {
            XMethod method = (XMethod) iter.next();

            if ("execute".equals(method.getName())) {
                if (method.getParameters().size() == 0) {
                    if (method.getReturnType().getType().getName().equals("void")) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    protected void startProcess() throws XDocletException
    {
        Collection classes = XJavaDoc.getInstance().getSourceClasses(false, processInnerClasses());

        super.startProcess();
    }

    /**
     * Returns true if the class is an Ant task. This causes the task to be processed by the XDoclet template task.
     *
     * @param clazz
     * @return
     * @exception XDocletException
     */
    protected boolean matchesGenerationRules(XClass clazz) throws XDocletException
    {
        boolean match = isAntTask(clazz);

        return match;
    }
}
