/*
*  The Apache Software License, Version 1.1
*
*  Copyright (c) 2003 The Apache Software Foundation.  All rights
*  reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions
*  are met:
*
*  1. Redistributions of source code must retain the above copyright
*  notice, this list of conditions and the following disclaimer.
*
*  2. Redistributions in binary form must reproduce the above copyright
*  notice, this list of conditions and the following disclaimer in
*  the documentation and/or other materials provided with the
*  distribution.
*
*  3. The end-user documentation included with the redistribution, if
*  any, must include the following acknowlegement:
*  "This product includes software developed by the
*  Apache Software Foundation (http://www.apache.org/)."
*  Alternately, this acknowlegement may appear in the software itself,
*  if and wherever such third-party acknowlegements normally appear.
*
*  4. The names "The Jakarta Project", "Ant", and "Apache Software
*  Foundation" must not be used to endorse or promote products derived
*  from this software without prior written permission. For written
*  permission, please contact apache@apache.org.
*
*  5. Products derived from this software may not be called "Apache"
*  nor may "Apache" appear in their names without prior written
*  permission of the Apache Group.
*
*  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
*  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
*  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
*  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
*  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
*  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
*  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
*  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
*  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
*  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
*  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
*  SUCH DAMAGE.
*  ====================================================================
*
*  This software consists of voluntary contributions made by many
*  individuals on behalf of the Apache Software Foundation.  For more
*  information on the Apache Software Foundation, please see
*  <http://www.apache.org/>.
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
