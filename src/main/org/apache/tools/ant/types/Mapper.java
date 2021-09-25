/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.CompositeMapper;
import org.apache.tools.ant.util.ContainerMapper;
import org.apache.tools.ant.util.FileNameMapper;

/**
 * Element to define a FileNameMapper.
 *
 */
public class Mapper extends DataType {
    // CheckStyle:VisibilityModifier OFF - bc

    protected MapperType type = null;
    protected String classname = null;
    protected Path classpath = null;
    protected String from = null;
    protected String to = null;

    // CheckStyle:VisibilityModifier ON

    private ContainerMapper container = null;

    /**
     * Construct a new <code>Mapper</code> element.
     * @param p   the owning Ant <code>Project</code>.
     */
    public Mapper(Project p) {
        setProject(p);
    }

    /**
     * Set the type of <code>FileNameMapper</code> to use.
     * @param type   the <code>MapperType</code> enumerated attribute.
     */
    public void setType(MapperType type) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.type = type;
    }

    /**
     * Cannot mix add and addconfigured in same type, so
     * provide this to override the add method.
     * @param fileNameMapper   the <code>FileNameMapper</code> to add.
     */
    public void addConfigured(FileNameMapper fileNameMapper) {
        add(fileNameMapper);
    }

    /**
     * Add a nested <code>FileNameMapper</code>.
     * @param fileNameMapper   the <code>FileNameMapper</code> to add.
     */
    public void add(FileNameMapper fileNameMapper) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (container == null) {
            if (type == null && classname == null) {
                container = new CompositeMapper();
            } else {
                FileNameMapper m = getImplementation();
                if (m instanceof ContainerMapper) {
                    container = (ContainerMapper) m;
                } else {
                    throw new BuildException(m
                        + " mapper implementation does not support nested mappers!");
                }
            }
        }
        container.add(fileNameMapper);
        setChecked(false);
    }

    /**
     * Add a Mapper
     * @param mapper the mapper to add
     */
    public void addConfiguredMapper(Mapper mapper) {
        add(mapper.getImplementation());
    }

    /**
     * Set the class name of the FileNameMapper to use.
     * @param classname the name of the class
     */
    public void setClassname(String classname) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.classname = classname;
    }

    /**
     * Set the classpath to load the FileNameMapper through (attribute).
     * @param classpath the classpath
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
     * @return a path object to be configured
     */
    public Path createClasspath() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (this.classpath == null) {
            this.classpath = new Path(getProject());
        }
        setChecked(false);
        return this.classpath.createPath();
    }

    /**
     * Set the classpath to load the FileNameMapper through via
     * reference (attribute).
     * @param ref the reference to the FileNameMapper
     */
    public void setClasspathRef(Reference ref) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        createClasspath().setRefid(ref);
    }

    /**
     * Set the argument to FileNameMapper.setFrom
     * @param from the from attribute to pass to the FileNameMapper
     */
    public void setFrom(String from) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.from = from;
    }

    /**
     * Set the argument to FileNameMapper.setTo
     * @param to the to attribute to pass to the FileNameMapper
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
     * @param r the reference to another mapper
     * @throws BuildException if other attributes are set
     */
    @Override
    public void setRefid(Reference r) throws BuildException {
        if (type != null || from != null || to != null) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }

    /**
     * Returns a fully configured FileNameMapper implementation.
     * @return a FileNameMapper object to be configured
     * @throws BuildException on error
     */
    public FileNameMapper getImplementation() throws BuildException {
        if (isReference()) {
            dieOnCircularReference();
            Reference r = getRefid();
            Object o = r.getReferencedObject(getProject());
            if (o instanceof FileNameMapper) {
                return (FileNameMapper) o;
            }
            if (o instanceof Mapper) {
                return ((Mapper) o).getImplementation();
            }
            String od = o == null ? "null" : o.getClass().getName();
            throw new BuildException(od + " at reference '"
                + r.getRefId() + "' is not a valid mapper reference.");
        }

        if (type == null && classname == null && container == null) {
            throw new BuildException(
                    "nested mapper or one of the attributes type or classname is required");
        }

        if (container != null) {
            return container;
        }

        if (type != null && classname != null) {
            throw new BuildException(
                "must not specify both type and classname attribute");
        }

        try {
            FileNameMapper m = getImplementationClass().getDeclaredConstructor().newInstance();
            final Project p = getProject();
            if (p != null) {
                p.setProjectReference(m);
            }
            m.setFrom(from);
            m.setTo(to);

            return m;
        } catch (BuildException be) {
            throw be;
        } catch (Throwable t) {
            throw new BuildException(t);
        }
    }

     /**
     * Gets the Class object associated with the mapper implementation.
     * @return <code>Class</code>.
     * @throws ClassNotFoundException if the class cannot be found
     */
    protected Class<? extends FileNameMapper> getImplementationClass() throws ClassNotFoundException {

        String cName = this.classname;
        if (type != null) {
            cName = type.getImplementation();
        }

        ClassLoader loader = (classpath == null)
            ? getClass().getClassLoader()
            // Memory leak in line below
            : getProject().createClassLoader(classpath);

        return Class.forName(cName, true, loader).asSubclass(FileNameMapper.class);
    }

    /**
     * Performs the check for circular references and returns the
     * referenced Mapper.
     * @deprecated since Ant 1.7.1 because a mapper might ref a
     *             FileNameMapper implementation directly.
     * @return the referenced Mapper
     */
    @Deprecated
    protected Mapper getRef() {
        return getCheckedRef(Mapper.class);
    }

    /**
     * Class as Argument to FileNameMapper.setType.
     */
    public static class MapperType extends EnumeratedAttribute {
        private Properties implementations;

        /** Constructor for the MapperType enumeration */
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

        /**
         * @return the filenamemapper names
         */
        @Override
        public String[] getValues() {
            return new String[] {"identity", "flatten", "glob",
                                 "merge", "regexp", "package", "unpackage"};
        }

        /**
         * @return the classname for the filenamemapper name
         */
        public String getImplementation() {
            return implementations.getProperty(getValue());
        }
    }

}
