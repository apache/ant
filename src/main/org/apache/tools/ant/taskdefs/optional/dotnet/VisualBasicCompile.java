/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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
 * This task compiles Visual Basic.NET source into executables or modules.
 * The task will only work on win2K until other platforms support vbc.exe or
 * an equivalent. VBC.exe must be on the execute path, too.
 *
 * <p>
 * All parameters are optional: &lt;vbc/&gt; should suffice to produce a debug
 * build of all *.vb files.
 *
 * @author Brian Felder bfelder@providence.org
 * @author Steve Loughran
 * @ant.task    name="vbc" category="dotnet"
 */

public class VisualBasicCompile extends DotnetCompile {

    /**
     * Compiler option to remove integer checks. Default: false.
     */
    private boolean removeIntChecks = false;

    /**
     * Require explicit declaration of variables? Default: false.
     */
    private boolean optionExplicit = false;

    /**
     * Enforce strict language semantics? Default: false.
     */
    private boolean optionStrict = false;

    /**
     * Whether to compare strings as "text" or "binary". Default: "binary".
     */
    private String optionCompare;

    /**
     * Root namespace for all type declarations.
     */
    private String rootNamespace;

    /**
     * Declare global imports fornamespaces in referenced metadata files.
     */
    private String imports;

    /**
     * Constructor for VisualBasicCompile.
     */
    public VisualBasicCompile() {
        super();
    }

    /**
     *  reset all contents.
     */
    public void clear() {
        super.clear();
        imports = null;
        rootNamespace = null;
        optionCompare = null;
        optionExplicit = false;
        optionStrict = false;
        removeIntChecks = false;
    }

    /**
     *  get the argument or null for no argument needed
     *  This is overridden from DotnetCompile.java because VBC uses
     *  "/win32resource:" rather than "/win32res:"
     *
     *@return    The Win32Res Parameter to CSC
     */
    protected String getWin32ResParameter() {
        if (getWin32Res() != null) {
            return "/win32resource:" + getWin32Res().toString();
        } else {
            return null;
        }
    }

    /**
     * Whether to remove integer checks. Default false.
     * @param  flag  on/off flag
     */
    public void setRemoveIntChecks(boolean flag) {
        removeIntChecks = flag;
    }

    /**
     * Get the flag for removing integer checks.
     * @return    true if flag is turned on
     */
    public boolean getRemoveIntChecks() {
        return removeIntChecks;
    }

    /**
     * Form the option string for removeIntChecks.
     * @return The parameter string.
     */
    public String getRemoveIntChecksParameter() {
        return "/removeintchecks" + (removeIntChecks ? "+" : "-");
    }

    /**
     * Whether to require explicit declaration of variables.
     * @param  flag  on/off flag
     */
    public void setOptionExplicit(boolean flag) {
        optionExplicit = flag;
    }

    /**
     * Get the flag for whether to require explicit declaration of variables.
     *@return    true if flag is turned on
     */
    public boolean getOptionExplicit() {
        return optionExplicit;
    }

    /**
     * Form the option string for optionExplicit..
     * @return The parameter string.
     */
    public String getOptionExplicitParameter() {
        return "/optionexplicit" + (optionExplicit ? "+" : "-");
    }

    /**
     * Enforce strict language semantics.
     * @param  flag  on/off flag
     */
    public void setOptionStrict(boolean flag) {
        optionStrict = flag;
    }

    /**
     * Get the flag for whether to enforce strict language semantics.
     * @return    true if flag is turned on
     */
    public boolean getOptionStrict() {
        return optionStrict;
    }

    /**
     * Forn the option string for optionStrict.
     * @return The parameter string.
     */
    public String getOptionStrictParameter() {
        return "/optionstrict" + (optionStrict ? "+" : "-");
    }


    /**
     * Specifies the root namespace for all type declarations.
     * @param  a root namespace.
     */
    public void setRootNamespace(String rootNamespace) {
        this.rootNamespace = rootNamespace;
    }


    /**
     * Get the root namespace.
     * @return  the root namespace.
     */
    public String getRootNamespace() {
        return this.rootNamespace;
    }


    /**
     * Form the option string for rootNamespace.
     * @return  the root namespace option string.
     */
    protected String getRootNamespaceParameter() {
        if (rootNamespace != null && rootNamespace.length() != 0) {
            return "/rootnamespace:" + rootNamespace;
        } else {
            return null;
        }
    }


    /**
     * Declare global imports for namespaces in referenced metadata files.
     * @param imports the imports string
     */
    public void setImports(String imports) {
        this.imports = imports;
    }


    /**
     * Get global imports for namespaces in referenced metadata files.
     * @return  the imports string.
     */
    public String getImports() {
        return this.imports;
    }


    /**
     * Format the option for imports.
     * @return  the formatted import option.
     */
    protected String getImportsParameter() {
        if (imports != null && imports.length() != 0) {
            return "/imports:" + imports;
        } else {
            return null;
        }
    }


    /**
     * Specify binary- or text-style string comparisons. Defaults
     * to "binary"
     * @param optionCompare the option compare style. "text" | "binary".
     */
    public void setOptionCompare(String optionCompare) {
        if ("text".equalsIgnoreCase(optionCompare)) {
            this.optionCompare = "text";
        } else {
            this.optionCompare = "binary";
        }
    }


    /**
     * "binary" or "text" for the string-comparison style.
     * @return  the option compare style.
     */
    public String getOptionCompare() {
        return this.optionCompare;
    }

    /**
     * Format the option for string comparison style.
     * @return  The formatted option.
     */
    protected String getOptionCompareParameter() {
        if (optionCompare != null && "text".equalsIgnoreCase(optionCompare)) {
            return "/optioncompare:text";
        } else {
            return "/optioncompare:binary";
        }
    }

    /**
     * implement VBC commands
     * @param command
     */
    protected void addCompilerSpecificOptions(NetCommand command) {
        command.addArgument(getRemoveIntChecksParameter());
        command.addArgument(getImportsParameter());
        command.addArgument(getOptionExplicitParameter());
        command.addArgument(getOptionStrictParameter());
        command.addArgument(getRootNamespaceParameter());
        command.addArgument(getOptionCompareParameter());
    }

    /**
     * Get the delimiter that the compiler uses between references.
     * For example, c# will return ";"; VB.NET will return ","
     * @return The string delimiter for the reference string.
     */
    public String getReferenceDelimiter() {
        return ",";
    }



    /**
     * Get the extension of filenames to compile.
     * @return The string extension of files to compile.
     */
    public String getFileExtension() {
        return "vb";
    }

    /**
     * from a resource, get the resource param
     * @param resource
     * @return a string containing the resource param, or a null string
     * to conditionally exclude a resource.
     */
    protected String createResourceParameter(DotnetResource resource) {
        return resource.getVbStyleParameter();
    }

    /**
     * Get the name of the compiler executable.
     * @return The name of the compiler executable.
     */
    public String getCompilerExeName() {
        return "vbc";
    }

    /**
     * validation code
     * @throws  BuildException  if validation failed
     */
    protected void validate()
            throws BuildException {
        super.validate();
        if(getDestFile()==null) {
            throw new BuildException("DestFile was not specified");
        }
    }
}
