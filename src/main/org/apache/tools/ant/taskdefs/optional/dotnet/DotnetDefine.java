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
import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;

/**
 * definitions can be conditional. What .NET conditions can not be
 * is in any state other than defined and undefined; you cannot give
 * a definition a value.
 */
public class DotnetDefine {
    private String name;
    private String ifCond;
    private String unlessCond;


    /**
     * the name of a property which must be defined for
     * the definition to be set. Optional.
     * @param condition
     */
    public void setIf(String condition) {
        this.ifCond = condition;
    }

    /**
     * the name of a property which must be undefined for
     * the definition to be set. Optional.
     * @param condition
     */
    public void setUnless(String condition) {
        this.unlessCond = condition;
    }

    public String getName() {
        return name;
    }

    /**
     * the name of the definition. Required.
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * get the value of this definition. Will be null if a condition
     * was declared and not met
     * @param owner owning task
     * @return
     * @throws BuildException
     */
    public String getValue(Task owner) throws BuildException {
        if(name==null) {
            throw new BuildException("No name provided for the define element",
                owner.getLocation());
        }
        if(!isSet(owner)) {
            return null;
        }
        return name;
    }


    /**
     * logic taken from patternset
     * @param owner
     * @return true if the condition is valid
     */
    public boolean isSet(Task owner) {
        Project p=owner.getProject();
        if (ifCond != null && p.getProperty(ifCond) == null) {
            return false;
        } else if (unlessCond != null && p.getProperty(unlessCond) != null) {
            return false;
        }
        return true;
    }
}
