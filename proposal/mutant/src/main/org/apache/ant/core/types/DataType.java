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
package org.apache.ant.core.types;

import org.apache.ant.core.execution.*;
import java.io.*;

/**
 * A DataType is an element which can register a named value in the
 * ExecutionFrame's context. 
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 */
public abstract class DataType extends AbstractTask {
    private String reference = null;
    private Object referencedObject = null;

    public void execute() throws ExecutionException {
    }

    /**
     * Creates an exception that indicates that refid has to be the
     * only attribute if it is set.  
     */
    protected ExecutionException tooManyAttributes() {
        return new ExecutionException("You must not specify more than one attribute" +
                                      " when using refid" );
    }

    /**
     * Creates an exception that indicates that this XML element must
     * not have child elements if the refid attribute is set.  
     */
    protected ExecutionException noChildrenAllowed() {
        return new ExecutionException("You must not specify nested elements when using refid");
    }

    /**
     * Creates an exception that indicates the user has generated a
     * loop of data types referencing each other.  
     */
    protected ExecutionException circularReference() {
        return new ExecutionException("This data type contains a circular reference.");
    }
    
    /**
     * Makes this instance in effect a reference to another DataType
     * instance.
     */
    public void setRefid(String reference) throws ExecutionException {
        this.reference = reference;
        Object referencedObject = getTaskContext().getDataValue(reference);

        if (referencedObject == null) {
            throw new ExecutionException("Unable to locate the reference specified by refid '" +
                                         getReference() + "'");
        }
        
        if (!this.getClass().isAssignableFrom(referencedObject.getClass())) {
            throw new ExecutionException("The object referenced by refid '" +
                                         getReference() + "' is not compatible with this element ");
        }
    }

    /**
     * Has the refid attribute of this element been set?
     */
    public boolean isReference() {
        return referencedObject != null;
    }


    protected Object getReferencedObject() throws ExecutionException {
        if (!isReference()) {
            throw new ExecutionException("You cannot get a referenced value from a data type " + 
                                         "which does not have the refid attribute");
        }
        
        return referencedObject;
    }
    
    protected String getReference() {
        return reference;
    }
}
