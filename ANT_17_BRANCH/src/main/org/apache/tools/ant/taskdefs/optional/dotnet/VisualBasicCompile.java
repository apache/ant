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


/**
 * This task compiles Visual Basic.NET source into executables or modules.
 * The task requires vbc.exe on the execute path, unless it or an equivalent
 * program is specified in the <tt>executable</tt> parameter
 *
 * <p>
 * All parameters are optional: &lt;vbc/&gt; should suffice to produce a debug
 * build of all *.vb files.
 *
 * <p>

 * The task is a directory based task, so attributes like
 * <tt>includes=&quot;**\/*.vb&quot;</tt> and
 * <tt>excludes=&quot;broken.vb&quot;</tt> can be used to control
 * the files pulled in. By default,
 * all *.vb files from the project folder down are included in the command.
 * When this happens the destFile -if not specified-
 * is taken as the first file in the list, which may be somewhat hard to control.
   Specifying the output file with <tt>destfile</tt> is prudent.
 </p>
 <p>
 * Also, dependency checking only works if destfile is set.
 *
 * <p>For historical reasons the pattern
 * <code>**</code><code>/*.vb</code> is preset as includes list and
 * you can not override it with an explicit includes attribute.  Use
 * nested <code>&lt;src&gt;</code> elements instead of the basedir
 * attribute if you need more control.</p>
 *
 * As with &lt;csc&gt; nested <tt>src</tt> filesets of source,
 * reference filesets, definitions and resources can be provided.
 *
 * <p>
 * Example
 * </p>
 * <pre>&lt;vbc
 *   optimize=&quot;true&quot;
 *   debug=&quot;false&quot;
 *   warnLevel=&quot;4&quot;
 *   targetType=&quot;exe&quot;
 *   definitions=&quot;RELEASE&quot;
 *   excludes=&quot;src/unicode_class.vb&quot;
 *   mainClass = &quot;MainApp&quot;
 *   destFile=&quot;NetApp.exe&quot;
 *   optionExplicit=&quot;true&quot;
 *   optionCompare=&quot;text&quot;
 *   references="System.Xml,System.Web.Xml"
 *   &gt;
 *          &lt;reference file="${testCSC.dll}" /&gt;
 *          &lt;define name="RELEASE" /&gt;
 *          &lt;define name="DEBUG" if="debug.property"/&gt;
 *          &lt;define name="def3" unless="def2.property"/&gt;
 *   &lt;/vbc&gt;
 </pre>
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
        clear();
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
        setExecutable("vbc");
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
     * For the option string for optionStrict.
     * @return The parameter string.
     */
    public String getOptionStrictParameter() {
        return "/optionstrict" + (optionStrict ? "+" : "-");
    }


    /**
     * Specifies the root namespace for all type declarations.
     * @param rootNamespace a root namespace.
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
     * @param command the command to set arguements on.
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

    /** {@inheritDoc} */
    protected void createResourceParameter(NetCommand command, DotnetResource resource) {
        resource.getParameters(getProject(), command, false);
    }

    /**
     * validation code
     * @throws  BuildException  if validation failed
     */
    protected void validate()
            throws BuildException {
        super.validate();
        if (getDestFile() == null) {
            throw new BuildException("DestFile was not specified");
        }
    }
}
