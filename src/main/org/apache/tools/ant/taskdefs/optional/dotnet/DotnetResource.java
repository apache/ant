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
package org.apache.tools.ant.taskdefs.optional.dotnet;

import org.apache.tools.ant.BuildException;

import java.io.File;

/**
 * class used by DotnetCompile to name resources, could be upgraded to a datatype
 * in the distant future.
 * a resource maps to /res:file,name
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

    public boolean isEmbed() {
        return embed;
    }

    /**
     * embed the resource in the assembly (default, true) or just link to it.
     * @param embed
     */
    public void setEmbed(boolean embed) {
        this.embed = embed;
    }

    public File getFile() {
        return file;
    }

    /**
     * name the resource
     * @param file
     */
    public void setFile(File file) {
        this.file = file;
    }

    public Boolean getPublic() {
        return isPublic;
    }

    /**
     * VB and J# only: is a resource public or not?
     * @param aPublic
     */
    public void setPublic(Boolean aPublic) {
        isPublic = aPublic;
    }

    public String getName() {
        return name;
    }

    /**
     * should the resource have a name?
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * build the C# style parameter (which has no public/private option)
     * @return
     */
    public String getCSharpStyleParameter() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(isEmbed() ? "/resource" : "/linkresource");
        buffer.append(':');
        buffer.append(getFile().toString());
        if (getName() != null) {
            buffer.append(',');
            buffer.append(getName());
        }
        if (getPublic() != null) {
            throw new BuildException("This compiler does not support the "
                    + "public/private option.");
        }
        return buffer.toString();
    }

    /**
     * get the style of param used by VB and javascript
     * @return
     */
    public String getVbStyleParameter() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(isEmbed() ? "/resource" : "/linkresource");
        buffer.append(':');
        buffer.append(getFile().toString());
        if (getName() != null) {
            buffer.append(',');
            buffer.append(getName());
            if (getPublic() != null) {
                buffer.append(',');
                buffer.append(getPublic().booleanValue()
                        ? "public" : "private");

            }
        } else if (getPublic() != null) {
            throw new BuildException("You cannot have a public or private "
                    + "option without naming the resource");
        }
        return buffer.toString();
    }
}
