/*
 * Copyright  2003-2004 The Apache Software Foundation
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

/**
 * describes a File or a ZipEntry
 *
 * this class is meant to be used by classes needing to record path
 * and date/time information about a file, a zip entry or some similar
 * resource (URL, archive in a version control repository, ...)
 *
 * @since Ant 1.5.2
 */
public class Resource implements Cloneable, Comparable {
    private String name = null;
    private boolean exists = true;
    private long lastmodified = 0;
    private boolean directory = false;

    /**
     * default constructor
     */
    public Resource() {
    }

    /**
     * only sets the name.
     *
     * <p>This is a dummy, used for not existing resources.</p>
     *
     * @param name relative path of the resource.  Expects
     * &quot;/&quot; to be used as the directory separator.
     */
    public Resource(String name) {
        this(name, false, 0, false);
    }

    /**
     * sets the name, lastmodified flag, and exists flag
     *
     * @param name relative path of the resource.  Expects
     * &quot;/&quot; to be used as the directory separator.
     */
    public Resource(String name, boolean exists, long lastmodified) {
        this(name, exists, lastmodified, false);
    }

    /**
     * @param name relative path of the resource.  Expects
     * &quot;/&quot; to be used as the directory separator.
     */
    public Resource(String name, boolean exists, long lastmodified,
                    boolean directory) {
        this.name = name;
        this.exists = exists;
        this.lastmodified = lastmodified;
        this.directory = directory;
    }

    /**
     * name attribute will contain the path of a file relative to the
     * root directory of its fileset or the recorded path of a zip
     * entry.
     *
     * <p>example for a file with fullpath /var/opt/adm/resource.txt
     * in a file set with root dir /var/opt it will be
     * adm/resource.txt.</p>
     *
     * <p>&quot;/&quot; will be used as the directory separator.</p>
     */
    public String getName() {
        return name;
    }

    /**
     * @param name relative path of the resource.  Expects
     * &quot;/&quot; to be used as the directory separator.
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * the exists attribute tells whether a file exists
     */
    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    /**
     * tells the modification time in milliseconds since 01.01.1970 of
     *
     * @return 0 if the resource does not exist to mirror the behavior
     * of {@link java.io.File File}.
     */
    public long getLastModified() {
        return !exists || lastmodified < 0 ? 0 : lastmodified;
    }

    public void setLastModified(long lastmodified) {
        this.lastmodified = lastmodified;
    }
    /**
     * tells if the resource is a directory
     * @return boolean flag indicating if the resource is a directory
     */
    public boolean isDirectory() {
        return directory;
    }

    public void setDirectory(boolean directory) {
        this.directory = directory;
    }

    /**
     * @return copy of this
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new Error("CloneNotSupportedException for a "
                            + "Clonable Resource caught?");
        }
    }

    /**
     * delegates to a comparison of names.
     *
     * @since Ant 1.6
     */
    public int compareTo(Object other) {
        if (!(other instanceof Resource)) {
            throw new IllegalArgumentException("Can only be compared with "
                                               + "Resources");
        }
        Resource r = (Resource) other;
        return getName().compareTo(r.getName());
    }
}
