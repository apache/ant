/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.types;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.FileNameMapper;

import java.util.Properties;
import java.util.Stack;

/**
 * Element to define a FileNameMapper.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 */
public class Mapper extends DataType implements Cloneable {

    protected MapperType type = null;

    public Mapper(Project p) {
        setProject(p);
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

    protected String classname = null;

    /**
     * Set the class name of the FileNameMapper to use.
     */
    public void setClassname(String classname) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.classname = classname;
    }

    protected Path classpath = null;

    /**
     * Set the classpath to load the FileNameMapper through (attribute).
     */
    public void setClasspath(Path classpath) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        if (this.classpath == null) {
            this.classpath = classpath;
        } else {
            this.classpath.append(classpath);
        }
    }

    /**
     * Set the classpath to load the FileNameMapper through (nested element).
     */
    public Path createClasspath() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (this.classpath == null) {
            this.classpath = new Path(getProject());
        }
        return this.classpath.createPath();
    }

    /**
     * Set the classpath to load the FileNameMapper through via
     * reference (attribute).
     */
    public void setClasspathRef(Reference r) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        createClasspath().setRefid(r);
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
        
        if (type == null && classname == null) {
            throw new BuildException("one of the attributes type or classname is required");
        }

        if (type != null && classname != null) {
            throw new BuildException("must not specify both type and classname attribute");
        }

        try {
            if (type != null) {
                classname = type.getImplementation();
            }

            Class c = null;
            if (classpath == null) {
                c = Class.forName(classname);
            } else {
                AntClassLoader al = new AntClassLoader(getProject(), 
                                                       classpath);
                c = al.loadClass(classname);
                AntClassLoader.initializeClass(c);
            }
            
            FileNameMapper m = (FileNameMapper) c.newInstance();
            m.setFrom(from);
            m.setTo(to);
            return m;
        } catch (BuildException be) {
            throw be;
        } catch (Throwable t) {
            throw new BuildException(t);
        } finally {
            if (type != null) {
                classname = null;
            }
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
            dieOnCircularReference(stk, getProject());
        }
        
        Object o = ref.getReferencedObject(getProject());
        if (!(o instanceof Mapper)) {
            String msg = ref.getRefId() + " doesn\'t denote a mapper";
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
            implementations.put("regexp", 
                                "org.apache.tools.ant.util.RegexpPatternMapper");
            implementations.put("package", 
                                "org.apache.tools.ant.util.PackageNameMapper");
        }

        public String[] getValues() {
            return new String[] {"identity", "flatten", "glob", 
                                 "merge", "regexp", "package"};
        }

        public String getImplementation() {
            return implementations.getProperty(getValue());
        }
    }

}
