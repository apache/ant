/*
 * Copyright  2000-2004 The Apache Software Foundation
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


/**
 * Compile J# source down to a managed .NET application.
 * <p>
 * J# is not Java. But it is the language closest to Java in the .NET framework.
 * This task compiles jsharp source (.java files), and
 * generates a .NET managed exe or dll.
 * <p>
 *
 * @see <A=ref="http://msdn.microsoft.com/library/default.asp?url=/library/en-us/dv_vjsharp/html/vjoriMicrosoftVisualJ.asp">
 * Visual J++ online documentation</a>
 *
 * @since ant1.6
 * @ant.task category="dotnet" name="jsharpc"
 */
public class JSharp extends DotnetCompile {

    /**
     * hex base address
     */
    String baseAddress;

    /** /x option to disable J++ and J# lang extensions
     *
     */
    boolean pureJava = true;

    /**
     * whether to make package scoped stuff public or assembly scoped
     */
    boolean secureScoping = false;

    public JSharp() {
        setExecutable("vjc");
    }


    public void setBaseAddress(String baseAddress) {
        this.baseAddress = baseAddress;
    }

    /**
     * do we want pure java (default, true) or corrupted J#?
     * @param pureJava
     */
    public void setPureJava(boolean pureJava) {
        this.pureJava = pureJava;
    }

    /**
     * Make package scoped code visible to the current assembly only (default: false)
     * .NET does not have package scoping. Instead it has assembly, private and public.
     * By default, package content is public to all.
     * @param secureScoping
     */
    public void setSecureScoping(boolean secureScoping) {
        this.secureScoping = secureScoping;
    }

    /**
     * Get the delimiter that the compiler uses between references.
     * For example, c# will return ";"; VB.NET will return ","
     * @return The string delimiter for the reference string.
     */
    public String getReferenceDelimiter() {
        return ";";
    }

    /**
     * Get the extension of filenames to compile.
     * @return The string extension of files to compile.
     */
    public String getFileExtension() {
        return ".java";
    }

    /**
     * add jvc specific commands
     * @param command
     */
    protected void addCompilerSpecificOptions(NetCommand command) {
        if (pureJava) {
            command.addArgument("/x:all");
        }
        if (secureScoping) {
            command.addArgument("/securescoping");
        }
    }

    /**
     * from a resource, get the resource param
     * @param resource
     * @return a string containing the resource param, or a null string
     * to conditionally exclude a resource.
     */
    protected String createResourceParameter(DotnetResource resource) {
        return resource.getCSharpStyleParameter();
    }

    /**
     * validation code
     * @throws  org.apache.tools.ant.BuildException  if validation failed
     */
    protected void validate()
            throws BuildException {
        super.validate();
        if (getDestFile() == null) {
            throw new BuildException("DestFile was not specified");
        }
    }
}
