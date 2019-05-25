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
package org.apache.tools.ant.filters.util;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.filters.BaseFilterReader;
import org.apache.tools.ant.filters.ChainableReader;
import org.apache.tools.ant.types.AntFilterReader;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.types.Parameterizable;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;

/**
 * Process a FilterReader chain.
 *
 */
public final class ChainReaderHelper {
    /**
     * Created type.
     */
    public class ChainReader extends FilterReader {

        private List<AntClassLoader> cleanupLoaders;

        private ChainReader(Reader in, List<AntClassLoader> cleanupLoaders) {
            super(in);
            this.cleanupLoaders = cleanupLoaders;
        }

        public String readFully() throws IOException {
            return ChainReaderHelper.this.readFully(this);
        }

        @Override
        public void close() throws IOException {
            cleanUpClassLoaders(cleanupLoaders);
            super.close();
        }

        @Override
        protected void finalize() throws Throwable {
            try {
                close();
            } finally {
                super.finalize();
            }
        }
    }

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
    public Vector<FilterChain> filterChains = new Vector<>();

    /** The Ant project */
    private Project project = null;

    // CheckStyle:VisibilityModifier ON

    /**
     * Default constructor.
     */
    public ChainReaderHelper() {
    }

    /**
     * Convenience constructor.
     * @param project ditto
     * @param primaryReader ditto
     * @param filterChains ditto
     */
    public ChainReaderHelper(Project project, Reader primaryReader,
        Iterable<FilterChain> filterChains) {
        withProject(project).withPrimaryReader(primaryReader)
            .withFilterChains(filterChains);
    }

    /**
     * Sets the primary {@link Reader}
     * @param rdr the reader object
     */
    public void setPrimaryReader(Reader rdr) {
        primaryReader = rdr;
    }

    /**
     * Fluent primary {@link Reader} mutator.
     * @param rdr Reader
     * @return {@code this}
     */
    public ChainReaderHelper withPrimaryReader(Reader rdr) {
        setPrimaryReader(rdr);
        return this;
    }

    /**
     * Set the project to work with
     * @param project the current project
     */
    public void setProject(final Project project) {
        this.project = project;
    }

    /**
     * Fluent {@link Project} mutator.
     * @param project ditto
     * @return {@code this}
     */
    public ChainReaderHelper withProject(Project project) {
        setProject(project);
        return this;
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
     * Fluent buffer size mutator.
     * @param size ditto
     * @return {@code this}
     */
    public ChainReaderHelper withBufferSize(int size) {
        setBufferSize(size);
        return this;
    }

    /**
     * Sets the collection of filter reader sets
     *
     * @param fchain the filter chains collection
     */
    public void setFilterChains(Vector<FilterChain> fchain) {
        filterChains = fchain;
    }

    /**
     * Fluent {@code filterChains} mutator.
     * @param filterChains ditto
     * @return {@code this}
     */
    public ChainReaderHelper withFilterChains(Iterable<FilterChain> filterChains) {
        final Vector<FilterChain> fcs;
        if (filterChains instanceof Vector<?>) {
            fcs = (Vector<FilterChain>) filterChains;
        } else {
            fcs = new Vector<>();
            filterChains.forEach(fcs::add);
        }
        setFilterChains(fcs);
        return this;
    }

    /**
     * Fluent mechanism to apply some {@link Consumer}.
     * @param consumer ditto
     * @return {@code this}
     */
    public ChainReaderHelper with(Consumer<ChainReaderHelper> consumer) {
        consumer.accept(this);
        return this;
    }

    /**
     * Assemble the reader
     * @return the assembled reader
     * @exception BuildException if an error occurs
     */
    public ChainReader getAssembledReader() throws BuildException {
        if (primaryReader == null) {
            throw new BuildException("primaryReader must not be null.");
        }

        Reader instream = primaryReader;
        final List<AntClassLoader> classLoadersToCleanUp = new ArrayList<>();

        final List<Object> finalFilters =
            filterChains.stream().map(FilterChain::getFilterReaders)
                .flatMap(Collection::stream).collect(Collectors.toList());

        if (!finalFilters.isEmpty()) {
            boolean success = false;
            try {
                for (Object o : finalFilters) {
                    if (o instanceof AntFilterReader) {
                        instream =
                            expandReader((AntFilterReader) o,
                                         instream, classLoadersToCleanUp);
                    } else if (o instanceof ChainableReader) {
                        setProjectOnObject(o);
                        instream = ((ChainableReader) o).chain(instream);
                        setProjectOnObject(instream);
                    }
                }
                success = true;
            } finally {
                if (!success && !classLoadersToCleanUp.isEmpty()) {
                    cleanUpClassLoaders(classLoadersToCleanUp);
                }
            }
        }
        return new ChainReader(instream, classLoadersToCleanUp);
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
     * Deregisters Classloaders from the project so GC can remove them later.
     */
    private static void cleanUpClassLoaders(List<AntClassLoader> loaders) {
        loaders.forEach(AntClassLoader::cleanup);
    }

    /**
     * Read data from the reader and return the
     * contents as a string.
     * @param rdr the reader object
     * @return the contents of the file as a string
     * @exception IOException if an error occurs
     */
    public String readFully(Reader rdr) throws IOException {
        return FileUtils.readFully(rdr, bufferSize);
    }

    /**
     * Creates and parameterizes a new FilterReader from a
     * &lt;filterreader&gt; element.
     *
     * @since Ant 1.8.0
     */
    private Reader expandReader(final AntFilterReader filter,
                                final Reader ancestor,
                                final List<AntClassLoader> classLoadersToCleanUp) {
        final String className = filter.getClassName();
        final Path classpath = filter.getClasspath();
        if (className != null) {
            try {
                Class<? extends FilterReader> clazz;
                try {
                    if (classpath == null) {
                        clazz = Class.forName(className)
                            .asSubclass(FilterReader.class);
                    } else {
                        AntClassLoader al =
                            filter.getProject().createClassLoader(classpath);
                        classLoadersToCleanUp.add(al);
                        clazz = Class.forName(className, true, al)
                            .asSubclass(FilterReader.class);
                    }
                } catch (ClassCastException ex) {
                    throw new BuildException("%s does not extend %s", className,
                        FilterReader.class.getName());
                }
                Optional<Constructor<?>> ctor =
                    Stream.of(clazz.getConstructors())
                        .filter(c -> c.getParameterCount() == 1
                            && c.getParameterTypes()[0]
                                .isAssignableFrom(Reader.class))
                        .findFirst();

                Object instream = ctor
                    .orElseThrow(() -> new BuildException(
                        "%s does not define a public constructor that takes in a %s as its single argument.",
                        className, Reader.class.getSimpleName()))
                    .newInstance(ancestor);

                setProjectOnObject(instream);
                if (Parameterizable.class.isAssignableFrom(clazz)) {
                    ((Parameterizable) instream).setParameters(filter.getParams());
                }
                return (Reader) instream;
            } catch (ClassNotFoundException | InstantiationException
                    | IllegalAccessException | InvocationTargetException ex) {
                throw new BuildException(ex);
            }
        }
        // Ant 1.7.1 and earlier ignore <filterreader> without a
        // classname attribute, not sure this is a good idea -
        // backwards compatibility makes it hard to change, though.
        return ancestor;
    }
}
