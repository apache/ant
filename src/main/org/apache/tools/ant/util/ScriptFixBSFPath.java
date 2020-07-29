/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.MagicNames;


/**
 * A class to modify a classloader to
 * support BSF language support.
 */
public class ScriptFixBSFPath {
    private static final String UTIL_OPTIONAL_PACKAGE
        = MagicNames.ANT_CORE_PACKAGE + ".util.optional";

    private static final String BSF_PACKAGE = "org.apache.bsf";
    private static final String BSF_MANAGER = BSF_PACKAGE + ".BSFManager";
    private static final String BSF_SCRIPT_RUNNER
        = UTIL_OPTIONAL_PACKAGE + ".ScriptRunner";

    /**
     * The following are languages that have
     * scripting engines embedded in bsf.jar.
     * The array is converted to a map of
     * languagename->classname.
     */
    private static final String[] BSF_LANGUAGES =
        new String[] {
            "js",         "org.mozilla.javascript.Scriptable",
            "javascript", "org.mozilla.javascript.Scriptable",
            "jacl",       "tcl.lang.Interp",
            "netrexx",    "netrexx.lang.Rexx",
            "nrx",        "netrexx.lang.Rexx",
            "jython",     "org.python.core.Py",
            "py",         "org.python.core.Py",
            "xslt",       "org.apache.xpath.objects.XObject"};

    /** A map of languages for which the engine in located in bsf */
    private static final Map<String, String> BSF_LANGUAGE_MAP = new HashMap<>();
    static {
        for (int i = 0; i < BSF_LANGUAGES.length; i += 2) {
            BSF_LANGUAGE_MAP.put(BSF_LANGUAGES[i], BSF_LANGUAGES[i + 1]);
        }
    }

    private File getClassSource(ClassLoader loader, String className) {
        return LoaderUtils.getResourceSource(
            loader,
            LoaderUtils.classNameToResource(className));
    }

    private File getClassSource(String className) {
        return getClassSource(getClass().getClassLoader(), className);
    }

    /**
     * Check if need to mess about with the classloader.
     * The class loader will need to be modified for two
     * reasons:
     * <ol>
     *  <li>language is at a higher level than bsf for engines in bsf,
     *      move bsf.
     *  </li>
     *  <li>bsf is at a higher level than oata.util.optional.ScriptRunner
     *  </li>
     * </ol>
     *
     * Assume a simple model for the loader:
     *  thisloader&lt;-customloader
     *  or
     *  thisloader
     *
     * @param loader the classloader to fix.
     * @param language the language to use.
     */
    public void fixClassLoader(ClassLoader loader, String language) {
        if (loader == getClass().getClassLoader()
            || !(loader instanceof AntClassLoader)) {
            return;
        }
        ClassLoader myLoader = getClass().getClassLoader();
        AntClassLoader fixLoader = (AntClassLoader) loader;

        // Check for location of bsf in this classloader
        File bsfSource = getClassSource(BSF_MANAGER);

        // If bsf is not in the classloader for this, need to move
        // runner.
        boolean needMoveRunner = (bsfSource == null);

        // Check for location of language
        String languageClassName = BSF_LANGUAGE_MAP.get(language);

        // Check if need to need to move bsf
        boolean needMoveBsf =
            bsfSource != null
            && languageClassName != null
            && !LoaderUtils.classExists(myLoader, languageClassName)
            && LoaderUtils.classExists(loader, languageClassName);

        // Update need to move runner
        needMoveRunner = needMoveRunner || needMoveBsf;

        // Check if bsf in place
        if (bsfSource == null) {
            bsfSource = getClassSource(loader, BSF_MANAGER);
        }

        if (bsfSource == null) {
            throw new BuildException(
                "Unable to find BSF classes for scripting");
        }

        if (needMoveBsf) {
            fixLoader.addPathComponent(bsfSource);
            fixLoader.addLoaderPackageRoot(BSF_PACKAGE);
        }

        if (needMoveRunner) {
            fixLoader.addPathComponent(
                LoaderUtils.getResourceSource(
                    fixLoader,
                    LoaderUtils.classNameToResource(BSF_SCRIPT_RUNNER)));
            fixLoader.addLoaderPackageRoot(UTIL_OPTIONAL_PACKAGE);
        }
    }
}
