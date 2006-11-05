/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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
package org.apache.tools.ant.filters.util;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Vector;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.filters.BaseFilterReader;
import org.apache.tools.ant.filters.ChainableReader;
import org.apache.tools.ant.types.AntFilterReader;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.Parameterizable;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;

/**
 * Process a FilterReader chain.
 *
 */
public final class ChainReaderHelper {

    // default buffer size
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    // CheckStyle:VisibilityModifier OFF - bc
    /**
     * The primary reader to which the reader chain is to be attached.
     */
    public Reader primaryReader;

    /**
     * The size of the buffer to be used.
     */
    public int bufferSize = DEFAULT_BUFFER_SIZE;

    /**
     * Chain of filters
     */
    public Vector filterChains = new Vector();

    /** The Ant project */
    private Project project = null;

    // CheckStyle:VisibilityModifier ON

    /**
     * Sets the primary reader
     * @param rdr the reader object
     */
    public void setPrimaryReader(Reader rdr) {
        primaryReader = rdr;
    }

    /**
     * Set the project to work with
     * @param project the current project
     */
    public void setProject(final Project project) {
        this.project = project;
    }

    /**
     * Get the project
     *
     * @return the current project
     */
    public Project getProject() {
        return project;
    }

    /**
     * Sets the buffer size to be used.  Defaults to 8192,
     * if this method is not invoked.
     * @param size the buffer size to use
     */
    public void setBufferSize(int size) {
        bufferSize = size;
    }

    /**
     * Sets the collection of filter reader sets
     *
     * @param fchain the filter chains collection
     */
    public void setFilterChains(Vector fchain) {
        filterChains = fchain;
    }

    /**
     * Assemble the reader
     * @return the assembled reader
     * @exception BuildException if an error occurs
     */
    public Reader getAssembledReader() throws BuildException {
        if (primaryReader == null) {
            throw new BuildException("primaryReader must not be null.");
        }

        Reader instream = primaryReader;
        final int filterReadersCount = filterChains.size();
        final Vector finalFilters = new Vector();

        for (int i = 0; i < filterReadersCount; i++) {
            final FilterChain filterchain =
                (FilterChain) filterChains.elementAt(i);
            final Vector filterReaders = filterchain.getFilterReaders();
            final int readerCount = filterReaders.size();
            for (int j = 0; j < readerCount; j++) {
                finalFilters.addElement(filterReaders.elementAt(j));
            }
        }

        final int filtersCount = finalFilters.size();

        if (filtersCount > 0) {
            for (int i = 0; i < filtersCount; i++) {
                Object o = finalFilters.elementAt(i);

                if (o instanceof AntFilterReader) {
                    final AntFilterReader filter
                        = (AntFilterReader) finalFilters.elementAt(i);
                    final String className = filter.getClassName();
                    final Path classpath = filter.getClasspath();
                    final Project pro = filter.getProject();
                    if (className != null) {
                        try {
                            Class clazz = null;
                            if (classpath == null) {
                                clazz = Class.forName(className);
                            } else {
                                AntClassLoader al
                                    = pro.createClassLoader(classpath);
                                clazz = Class.forName(className, true, al);
                            }
                            if (clazz != null) {
                                if (!FilterReader.class.isAssignableFrom(clazz)) {
                                    throw new BuildException(className
                                        + " does not extend java.io.FilterReader");
                                }
                                final Constructor[] constructors =
                                    clazz.getConstructors();
                                int j = 0;
                                boolean consPresent = false;
                                for (; j < constructors.length; j++) {
                                    Class[] types = constructors[j]
                                                      .getParameterTypes();
                                    if (types.length == 1
                                        && types[0].isAssignableFrom(Reader.class)) {
                                        consPresent = true;
                                        break;
                                    }
                                }
                                if (!consPresent) {
                                    throw new BuildException(className
                                        + " does not define a public constructor"
                                        + " that takes in a Reader as its "
                                        + "single argument.");
                                }
                                final Reader[] rdr = {instream};
                                instream =
                                    (Reader) constructors[j].newInstance((Object[]) rdr);
                                setProjectOnObject(instream);
                                if (Parameterizable.class.isAssignableFrom(clazz)) {
                                    final Parameter[] params = filter.getParams();
                                    ((Parameterizable)
                                        instream).setParameters(params);
                                }
                            }
                        } catch (final ClassNotFoundException cnfe) {
                            throw new BuildException(cnfe);
                        } catch (final InstantiationException ie) {
                            throw new BuildException(ie);
                        } catch (final IllegalAccessException iae) {
                            throw new BuildException(iae);
                        } catch (final InvocationTargetException ite) {
                            throw new BuildException(ite);
                        }
                    }
                } else if (o instanceof ChainableReader) {
                    setProjectOnObject(o);
                    instream = ((ChainableReader) o).chain(instream);
                    setProjectOnObject(instream);
                }
            }
        }
        return instream;
    }

    /**
     * helper method to set the project on an object.
     * the reflection setProject does not work for anonymous/protected/private
     * classes, even if they have public methods.
     */
    private void setProjectOnObject(Object obj) {
        if (project == null) {
            return;
        }
        if (obj instanceof BaseFilterReader) {
            ((BaseFilterReader) obj).setProject(project);
            return;
        }
        project.setProjectReference(obj);
    }

    /**
     * Read data from the reader and return the
     * contents as a string.
     * @param rdr the reader object
     * @return the contents of the file as a string
     * @exception IOException if an error occurs
     */
    public String readFully(Reader rdr)
        throws IOException {
        return FileUtils.readFully(rdr, bufferSize);
    }
}
