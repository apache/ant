/*
 * Copyright  2000-2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.types;

import java.util.Properties;
import java.util.Stack;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.FileNameMapper;

/**
 * Element to define a FileNameMapper.
 *
 * @author Stefan Bodewig
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
                AntClassLoader al = getProject().createClassLoader(classpath);
                c = Class.forName(classname, true, al);
            }

            FileNameMapper m = (FileNameMapper) c.newInstance();
            final Project project = getProject();
            if (project != null) {
                project.setProjectReference(m);
            }
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
        if (!isChecked()) {
            Stack stk = new Stack();
            stk.push(this);
            dieOnCircularReference(stk, getProject());
        }

        Object o = getRefid().getReferencedObject(getProject());
        if (!(o instanceof Mapper)) {
            String msg = getRefid().getRefId() + " doesn\'t denote a mapper";
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
            implementations.put("unpackage",
                                "org.apache.tools.ant.util.UnPackageNameMapper");
        }

        public String[] getValues() {
            return new String[] {"identity", "flatten", "glob",
                                 "merge", "regexp", "package", "unpackage"};
        }

        public String getImplementation() {
            return implementations.getProperty(getValue());
        }
    }

}
