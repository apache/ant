/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.types;


import java.util.Stack;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Base class for those classes that can appear inside the build file
 * as stand alone data types.  
 *
 * <p>This class handles the common description attribute and provides
 * a default implementation for reference handling and checking for
 * circular references that is appropriate for types that can not be
 * nested inside elements of the same type (i.e. &lt;patternset&gt;
 * but not &lt;path&gt;).</p>
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 */
public abstract class DataType {
    /**
     * The descriptin the user has set.
     */
    protected String description = null;
    /**
     * Value to the refid attribute.
     */
    protected Reference ref = null;
    /**
     * Are we sure we don't hold circular references?
     *
     * <p>Subclasses are responsible for setting this value to false
     * if we'd need to investigate this condition (usually because a
     * child element has been added that is a subclass of
     * DataType).</p> 
     */
    protected boolean checked = true;
    
    /** 
     * Sets a description of the current data type. It will be useful
     * in commenting what we are doing.  
     */
    public void setDescription( String desc ) {
        description=desc;
    }

    /**
     * Return the description for the current data type.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Has the refid attribute of this element been set?
     */
    public boolean isReference() {
        return ref != null;
    }

    /**
     * Set the value of the refid attribute.
     *
     * <p>Subclasses may need to check whether any other attributes
     * have been set as well or child elements have been created and
     * thus override this method. if they do the must call
     * <code>super.setRefid</code>.</p> 
     */
    public void setRefid(Reference ref) {
        this.ref = ref;
        checked = false;
    }

    /**
     * Check to see whether any DataType we hold references to is
     * included in the Stack (which holds all DataType instances that
     * directly or indirectly reference this instance, including this
     * instance itself).
     *
     * <p>If one is included, throw a BuildException created by {@link
     * #circularReference circularReference}.</p>
     *
     * <p>This implementation is appropriate only for a DataType that
     * cannot hold other DataTypes as children.</p> 
     *
     * <p>The general contract of this method is that it shouldn't do
     * anything if {@link #checked <code>checked</code>} is true and
     * set it to true on exit.</p> 
     */
    protected void dieOnCircularReference(Stack stk, Project p) 
        throws BuildException {

        if (checked || !isReference()) {
            return;
        }
        Object o = ref.getReferencedObject(p);
        
        if (o instanceof DataType) {
            if (stk.contains(o)) {
                throw circularReference();
            } else {
                stk.push(o);
                ((DataType) o).dieOnCircularReference(stk, p);
                stk.pop();
            }
        }
        checked = true;
    }

    /**
     * Creates an exception that indicates that refid has to be the
     * only attribute if it is set.  
     */
    protected BuildException tooManyAttributes() {
        return new BuildException( "You must not specify more than one attribute" +
                                   " when using refid" );
    }

    /**
     * Creates an exception that indicates that this XML element must
     * not have child elements if the refid attribute is set.  
     */
    protected BuildException noChildrenAllowed() {
        return new BuildException("You must not specify nested elements when using refid");
    }

    /**
     * Creates an exception that indicates the user has generated a
     * loop of data types referencing each other.  
     */
    protected BuildException circularReference() {
        return new BuildException("This data type contains a circular reference.");
    }
}
