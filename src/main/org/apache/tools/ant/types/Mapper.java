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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.*;

import java.util.Properties;
import java.util.Stack;

/**
 * Element to define a FileNameMapper.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 */
public class Mapper extends DataType {

    protected Project p;

    protected MapperType type = null;

    public Mapper(Project p) {
        this.p = p;
    }

    /**
     * Set the type of FileNameMapper to use.
     */
    public void setType(MapperType type) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.type = type;
    }

    protected String from = null;

    /**
     * Set the argument to FileNameMapper.setFrom
     */
    public void setFrom(String from) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.from = from;
    }

    protected String to = null;

    /**
     * Set the argument to FileNameMapper.setTo
     */
    public void setTo(String to) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.to = to;
    }

    /**
     * Make this Mapper instance a reference to another Mapper.
     *
     * <p>You must not set any other attribute if you make it a
     * reference.</p>
     */
    public void setRefid(Reference r) throws BuildException {
        if (type != null || from != null || to != null) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }

    /**
     * Returns a fully configured FileNameMapper implementation.
     */
    public FileNameMapper getImplementation() throws BuildException {
        if (isReference()) {
            return getRef().getImplementation();
        }
        
        if (type == null) {
            throw new BuildException("type attribute is required");
        }

        try {
            Class c = Class.forName(type.getImplementation());
            FileNameMapper m = (FileNameMapper) c.newInstance();
            m.setFrom(from);
            m.setTo(to);
            return m;
        } catch (Throwable t) {
            throw new BuildException(t);
        }
    }
        
    /**
     * Performs the check for circular references and returns the
     * referenced Mapper.  
     */
    protected Mapper getRef() {
        if (!checked) {
            Stack stk = new Stack();
            stk.push(this);
            dieOnCircularReference(stk, p);
        }
        
        Object o = ref.getReferencedObject(p);
        if (!(o instanceof Mapper)) {
            String msg = ref.getRefId()+" doesn\'t denote a mapper";
            throw new BuildException(msg);
        } else {
            return (Mapper) o;
        }
    }

    /**
     * Class as Argument to FileNameMapper.setType.
     */
    public static class MapperType extends EnumeratedAttribute {
        private Properties implementations;

        public MapperType() {
            implementations = new Properties();
            implementations.put("identity", 
                                "org.apache.tools.ant.util.IdentityMapper");
            implementations.put("flatten", 
                                "org.apache.tools.ant.util.FlatFileNameMapper");
            implementations.put("glob", 
                                "org.apache.tools.ant.util.GlobPatternMapper");
            implementations.put("merge", 
                                "org.apache.tools.ant.util.MergingMapper");
        }

        public String[] getValues() {
            return new String[] {"identity", "flatten", "glob", "merge"};
        }

        public String getImplementation() {
            return implementations.getProperty(getValue());
        }
    }
}
