/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.xdoclet;

import com.sun.javadoc.ClassDoc;
import xdoclet.TemplateSubTask;
import xdoclet.XDocletException;
import xdoclet.XDocletTagSupport;
import xdoclet.tags.TypeTagsHandler;
import xdoclet.util.TypeConversionUtil;

import java.io.File;
import java.text.MessageFormat;

/**
 * Custom XDoclet subtask to handle Ant datatypes
 */
public class DatatypeSubTask extends TemplateSubTask {
    public final static String SUBTASK_NAME = "datatypes";

    public String getSubTaskName() {
        return SUBTASK_NAME;
    }

    /**
     * Returns true if the class is an Ant task. This causes the task to be processed
     * by the XDoclet template task.
     */
    protected boolean matchesGenerationRules(ClassDoc clazz) throws XDocletException {
        return isAntDatatype(clazz);
    }

    /**
     * @todo a datatype doesn't have to extend Datatype, right?  so perhaps should
     *       another condition to flag a class with @ant.datatype name="..."
     */
    public static final boolean isAntDatatype(ClassDoc clazz) throws XDocletException {
        if (clazz.isAbstract()) {
            return false;
        }

        // no inner classes
        if (clazz.containingClass() != null) {
            return false;
        }

        String ignoreValue = XDocletTagSupport.getClassTagValue(clazz, "ant:datatype", "ignore", 0, null, "false", false, false);
        boolean ignore = TypeConversionUtil.stringToBoolean(ignoreValue, true);

        if (ignore) {
            return false;
        }

        return TypeTagsHandler.isOfType(clazz,
                "org.apache.tools.ant.types.DataType",
                TypeTagsHandler.TYPE_HIERARCHY);
    }

    /**
     * Custom file naming. Use the task name for the file name rather than the
     * default class name.
     *
     * @todo fix hardcoded path name
     */
    protected String getGeneratedFileName(ClassDoc clazz) throws XDocletException {
        String typeName = DatatypeTagsHandler.getDatatypeName(clazz);
        return typeName + ".xml";
    }

}
