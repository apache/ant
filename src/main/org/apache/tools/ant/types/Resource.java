/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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

/**
 * describes a File or a ZipEntry
 *
 * this class is meant to be used by classes needing to record path
 * and date/time information about a file, a zip entry or some similar
 * resource (URL, archive in a version control repository, ...)
 *
 * @author <a href="mailto:levylambert@tiscali-dsl.de">Antoine Levy-Lambert</a>
 * @since Ant 1.5.2
 */
public class Resource implements Cloneable {
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

}
