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
package org.apache.tools.ant.types.resources;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;

/**
 * ResourceCollection representing the union of multiple nested ResourceCollections.
 * @since Ant 1.7
 */
public class Union extends BaseResourceCollectionContainer {

    /**
     * Static convenience method to union an arbitrary set of Resources.
     * @param rc a ResourceCollection.
     * @return a Union.
     */
    public static Union getInstance(ResourceCollection rc) {
        return rc instanceof Union ? (Union) rc : new Union(rc);
    }

    /**
     * Default constructor.
     */
    public Union() {
    }

    /**
     * Create a new Union.
     * @param project owning Project
     */
    public Union(Project project) {
        super(project);
    }

    /**
     * Convenience constructor.
     * @param rc the ResourceCollection to add.
     */
    public Union(ResourceCollection rc) {
        this(Project.getProject(rc), rc);
    }

    /**
     * Convenience constructor.
     * @param project owning Project
     * @param rc the ResourceCollection to add.
     */
    public Union(Project project, ResourceCollection rc) {
        super(project);
        add(rc);
    }

    /**
     * Returns all Resources in String format. Provided for
     * convenience in implementing Path.
     * @return String array of Resources.
     */
    public String[] list() {
        if (isReference()) {
            return getRef().list();
        }
        return streamResources().map(Object::toString).toArray(String[]::new);
    }

    /**
     * Convenience method.
     * @return Resource[]
     */
    public Resource[] listResources() {
        if (isReference()) {
            return getRef().listResources();
        }
        return streamResources().toArray(Resource[]::new);
    }

    /**
     * Unify the contained Resources.
     * @return a Collection of Resources.
     */
    @Override
    protected Collection<Resource> getCollection() {
        return getAllResources();
    }

    /**
     * Unify the contained Resources.
     * @param <T> resource type
     * @param asString indicates whether the resulting Collection
     *        should contain Strings instead of Resources.
     * @return a Collection of Resources.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    protected <T> Collection<T> getCollection(boolean asString) { // TODO untypable
        return asString ? (Collection<T>) getAllToStrings()
            : (Collection<T>) getAllResources();
    }

    /**
     * Get a collection of strings representing the unified resource set (strings may duplicate).
     * @return Collection&lt;String&gt;
     */
    protected Collection<String> getAllToStrings() {
        return streamResources(Object::toString)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Get the unified set of contained Resources.
     * @return Set&lt;Resource&gt;
     */
    protected Set<Resource> getAllResources() {
        return streamResources()
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Union getRef() {
        return getCheckedRef(Union.class);
    }

    private Stream<? extends Resource> streamResources() {
        return streamResources(Function.identity());
    }

    private <T> Stream<? extends T> streamResources(
        Function<? super Resource, ? extends T> mapper) {
        return getResourceCollections().stream()
            .flatMap(ResourceCollection::stream).map(mapper).distinct();
    }
}
