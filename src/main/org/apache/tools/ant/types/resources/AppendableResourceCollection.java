package org.apache.tools.ant.types.resources;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.ResourceCollection;

/**
 * Interface describing a collection of Resources, to which elements can be
 * appended.
 *
 * @since Ant 1.10.10
 */
public interface AppendableResourceCollection extends ResourceCollection {
    /**
     Add a ResourceCollection to the container.
     
     @param c the ResourceCollection to add.
     @throws BuildException on error.
     @since Ant 1.10.10
     */
    void add(ResourceCollection c) throws BuildException;
}
