/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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
 * @author Steve Loughran
 * @since ant1.6
 * @ant.task category="dotnet"
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
