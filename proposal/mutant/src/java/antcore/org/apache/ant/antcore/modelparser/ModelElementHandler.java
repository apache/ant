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
package org.apache.ant.antcore.modelparser;
import org.apache.ant.antcore.xml.ElementHandler;

import org.apache.ant.common.model.ModelElement;
import java.util.Iterator;
import org.apache.ant.common.util.AttributeCollection;

/**
 *  A BuildElementHandler parses the task elements of a build. Task elements
 *  include tasks themselves plus all their nested elements to any depth.
 *
 * @author  Conor MacNeill
 * @created  9 January 2002
 */
public abstract class ModelElementHandler extends ElementHandler {
    /**  The model element being parsed by this handler. */
    private ModelElement modelElement;


    /**
     *  Set the model element being handled by this handler
     *
     * @param  modelElement The new modelElement value
     */
    protected void setModelElement(ModelElement modelElement) {
        this.modelElement = modelElement;
    }


    /**  Set the end location of the element.  */
    protected void finish() {
        if (modelElement != null) {
            modelElement.setEndLocation(getLocation());
        }
    }

    /**
     * Add all attributes which belong to namespaces rather than the default
     * namespace for the build files.
     */
    protected void addNamespaceAttributes() {
        for (Iterator i = getNamespaces(); i.hasNext();) {
            String uri = (String) i.next();
            AttributeCollection namespaceValues = getNamespaceAttributes(uri);
            modelElement.addNamespaceAttributes(uri, namespaceValues);
        }
    }

}

