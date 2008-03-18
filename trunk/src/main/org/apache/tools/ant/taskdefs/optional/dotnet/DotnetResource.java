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
package org.apache.tools.ant.taskdefs.optional.dotnet;

import org.apache.tools.ant.BuildException;

import java.io.File;
import java.util.ArrayList;
import org.apache.tools.ant.types.FileSet;
import java.util.Iterator;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.DirectoryScanner;

/**
 * Used by {@link DotnetCompile} to name resources.
 * Could be upgraded to a datatype in the distant future.
 * A resource maps to /res:file,name
 */
public class DotnetResource {

    /**
     * name of resource
     */
    private File file;

    /**
     * embed (default) or link the resource
     */
    private boolean embed = true;

    /**
     * this is used in VBC and JSC
     */
    private Boolean isPublic = null;

    /**
     * name of the object
     */
    private String name = null;

    /**
     * A list of filesets with resources.
     */
    private ArrayList fileSets = new ArrayList();

    /**
     * a namespace to be used with <filesets>
     */
    private String namespace = null;

    /**
     * Return the embed attribute.
     * @return the embed value.
     */
    public boolean isEmbed() {
        return embed;
    }

    /**
     * embed the resource in the assembly (default, true) or just link to it.
     *
     * @param embed a <code>boolean</code> value.
     */
    public void setEmbed(boolean embed) {
        this.embed = embed;
    }

    /**
     * The file resource.
     * @return the file resource.
     */
    public File getFile() {
        return file;
    }

    /**
     * name the resource
     *
     * @param file the file.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Get the public attribute.
     * @return the public attribute.
     */
    public Boolean getPublic() {
        return isPublic;
    }

    /**
     * VB and J# only: is a resource public or not?
     *
     * @param aPublic a <code>boolean</code> value.
     */
    public void setPublic(Boolean aPublic) {
        isPublic = aPublic;
    }

    /**
     * The name of the resource.
     * @return the name of the resource.
     */
    public String getName() {
        return name;
    }

    /**
     * should the resource have a name?
     *
     * @param name the name of the resource.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Filesets root namespace. The value always ends with '.' .
     *
     * @return String namespace name
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Sets filesets root namespace.
     *
     * @param namespace
     *            String root namespace
     */
    public void setNamespace(String namespace) {
        if (namespace == null) {
            this.namespace = null;
        } else {
            this.namespace = (namespace.length() == 0 || namespace.endsWith(".") ? namespace
                    : namespace + '.');
        }
    }

    private void checkParameters() {
        if (hasFilesets()) {
            if (getName() != null) {
                throw new BuildException(
                        "Cannot use <resource name=\"...\"> attribute with filesets");
            }
            if (getFile() != null) {
                throw new BuildException(
                        "Cannot use <resource file=\"...\"> attribute with filesets");
            }
        } else {
            if (getNamespace() != null) {
                throw new BuildException(
                        "Cannot use <resource namespace=\"...\"> attribute without filesets");
            }
        }
    }

    /**
     * build the C# style parameter (which has no public/private option)
     * @param p the current project.
     * @param command the command.
     * @param csharpStyle a <code>boolean</code> attribute.
     */
    public void getParameters(Project p, NetCommand command, boolean csharpStyle) {
        checkParameters();
        if (hasFilesets()) {
            for (Iterator listIter = fileSets.iterator(); listIter.hasNext();) {
                FileSet fs = (FileSet) listIter.next();
                String baseDirectory = fs.getDir(p).toString();
                String namespace = getNamespace(); // ends with '.' or null
                DirectoryScanner ds = fs.getDirectoryScanner(p);
                String[] files = ds.getIncludedFiles();
                for (int i = 0; i < files.length; i++) {
                    String file = files[i];
                    command.addArgument(getParameter(baseDirectory + File.separatorChar + file,
                            (namespace == null ? null : namespace
                                    + file.replace(File.separatorChar, '.')), csharpStyle));
                }
            }
        } else {
            command.addArgument(getParameter(getFile().toString(), getName(), csharpStyle));
        }
    }

    private String getParameter(String fileName, String name, boolean csharpStyle) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(isEmbed() ? "/resource" : "/linkresource");
        buffer.append(':');
        buffer.append(fileName);
        if (name != null) {
            buffer.append(',');
            buffer.append(name);
            if (csharpStyle) {
                if (getPublic() != null) {
                    throw new BuildException("This compiler does not support the "
                            + "public/private option.");
                } else {
                    if (getPublic() != null) {
                        buffer.append(',');
                        buffer.append(getPublic().booleanValue() ? "public" : "private");

                    }
                }
            } else if (getPublic() != null) {
                throw new BuildException("You cannot have a public or private "
                        + "option without naming the resource");
            }
        }
        return buffer.toString();
    }

    /**
     * Adds a resource file set.
     *
     * @param fileset
     *            FileSet
     */
    public void addFileset(FileSet fileset) {
        fileSets.add(fileset);
    }

    /**
     * Checks that <resource> node has embedded <filesets>
     *
     * @return boolean
     */
    public boolean hasFilesets() {
        return fileSets.size() > 0;
    }
}
