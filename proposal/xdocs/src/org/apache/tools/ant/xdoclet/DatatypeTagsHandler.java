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
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.Type;

import xdoclet.XDocletException;
import xdoclet.XDocletTagSupport;
import xdoclet.tags.AbstractProgramElementTagsHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Custom tag handler for XDoclet templates for Ant-specific processing.
 *
 * @author     Erik Hatcher
 * @created    February 17, 2002
 *
 * @todo clean up logic so that all setters are gathered first (even
 * superclass) and sorted along wih them
 * @todo need to create better logic for finding proper setters
 * @todo add ifIsAntTask, among other convenience tags
 */
public class DatatypeTagsHandler extends XDocletTagSupport {

    /**
     * Iterates over all Ant datatypes
     */
    public void forAllDatatypes(String template, Properties attributes) throws XDocletException {
        ClassDoc[] classes = AbstractProgramElementTagsHandler.getAllClasses();
        ClassDoc cur_class = null;

        for (int i = 0; i < classes.length; i++) {
            cur_class = classes[i];
            setCurrentClass(cur_class);

            if (DatatypeSubTask.isAntDatatype(cur_class)) {
                generate(template);
            }
        }
    }

    /**
     * Provides the datatype name
     */
    public String typeName() throws XDocletException {
        return getDatatypeName(getCurrentClass());
    }

    public static final String getDatatypeName(ClassDoc clazz) throws XDocletException {
        // sheesh!  There should be a friendlier method than this!
        String tagValue = getTagValue(clazz, "ant:datatype", "name", -1,
                null, null, null, null,
                null, false, XDocletTagSupport.FOR_CLASS, false);

        if (tagValue == null) {
            tagValue = clazz.name();

            tagValue = tagValue.toLowerCase();
        }
        return tagValue;
    }
}

