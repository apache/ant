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
package org.apache.ant.core.model;

import java.util.*;
import org.apache.ant.core.support.*;

/**
 * A BuildElement is an element of a build file and has a location
 * within that file.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 */ 
public class BuildElement {
    /**
     * The aspects defined for this element.
     */
    private Map aspects;

    /**
     * The location of this element
     */
    private Location location;
    
    /**
     * A comment associated with this element, if any
     *
     */
    private String comment;
    
    /**
     * Create a build element giving its location.
     *
     * @param location identifies where this element is defined
     */
    public BuildElement(Location location) {
        this.location = location;
    }
    
    /**
     * Get the location of the source where this element is defined
     *
     * @return the element's location
     */
    public Location getLocation() {
        return location;
    }
    
    /**
     * Set a comment associated with this element
     *
     * @param comment the comment to be associated with this element.
     */
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    /**
     * Get the comment associated with this element.
     *
     * @return the element's comment which may be null.
     */
    public String getComment() {
        return comment;
    }
    
    /**
     * Set the aspects of this element
     *
     * @param aspects a Map of apects that relate to this build element.
     */
    public void setAspects(Map aspects) {
        this.aspects = aspects;
    }
}

