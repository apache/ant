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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;

/**
 * This is a helper class used by ScriptRunnerHelper to
 * create a ScriptRunner based on a classloader and on a language.
 */
public class ScriptRunnerCreator {
    private static final String AUTO = "auto";
    private static final String UTIL_OPT = MagicNames.ANT_CORE_PACKAGE + ".util.optional";

    private static final String BSF = "bsf";
    private static final String BSF_PACK = "org.apache.bsf";
    private static final String BSF_MANAGER = BSF_PACK + ".BSFManager";
    private static final String BSF_RUNNER = UTIL_OPT + ".ScriptRunner";

    private static final String JAVAX = "javax";
    private static final String JAVAX_MANAGER = "javax.script.ScriptEngineManager";
    private static final String JAVAX_RUNNER = UTIL_OPT + ".JavaxScriptRunner";

    private Project     project;
    private String      manager;
    private String      language;
    private ClassLoader scriptLoader = null;

    /**
     * Constructor for creator.
     * @param project the current project.
     */
    public ScriptRunnerCreator(Project project) {
        this.project = project;
    }

    /**
     * Create a ScriptRunner.
     * @param manager      the script manager ("auto" | "bsf" | "javax")
     * @param language     the language.
     * @param classLoader  the classloader to use
     * @return the created script runner.
     * @throws BuildException if unable to create the ScriptRunner.
     */
    public synchronized ScriptRunnerBase createRunner(
        String manager, String language, ClassLoader classLoader) {
        this.manager      = manager;
        this.language     = language;
        this.scriptLoader = classLoader;

        if (language == null) {
            throw new BuildException("script language must be specified");
        }
        if (!manager.equals(AUTO) && !manager.equals(JAVAX) && !manager.equals(BSF)) {
            throw new BuildException("Unsupported language prefix " + manager);
        }

        // Check for bsf first then javax
        // This version does not check if the scriptManager
        // supports the language.

        ScriptRunnerBase ret = null;
        ret = createRunner(BSF, BSF_MANAGER, BSF_RUNNER);
        if (ret == null) {
            ret = createRunner(JAVAX, JAVAX_MANAGER, JAVAX_RUNNER);
        }
        if (ret != null) {
            return ret;
        }
        if (JAVAX.equals(manager)) {
            throw new BuildException(
                    "Unable to load the script engine manager " + "(" + JAVAX_MANAGER + ")");
        }
        if (BSF.equals(manager)) {
            throw new BuildException(
                    "Unable to load the BSF script engine manager " + "(" + BSF_MANAGER + ")");
        }
        throw new BuildException("Unable to load a script engine manager "
                + "(" + BSF_MANAGER + " or " + JAVAX_MANAGER + ")");
    }

    /**
     * Create a script runner if the scriptManager matches the passed
     * in manager.
     * This checks if the script manager exists in the scriptLoader
     * classloader and if so it creates and returns the script runner.
     * @param checkManager check if the manager matches this value.
     * @param managerClass the name of the script manager class.
     * @param runnerClass   the name of ant's script runner for this manager.
     * @return the script runner class.
     * @throws BuildException if there is a problem creating the runner class.
     */
    private ScriptRunnerBase createRunner(
        String checkManager, String managerClass, String runnerClass) {
        ScriptRunnerBase runner = null;
        if (!manager.equals(AUTO) && !manager.equals(checkManager)) {
            return null;
        }
        if (managerClass.equals(BSF_MANAGER)) {
            if (scriptLoader.getResource(LoaderUtils.classNameToResource(managerClass)) == null) {
                return null;
            }
            new ScriptFixBSFPath().fixClassLoader(scriptLoader, language);
        } else {
            try {
                Class.forName(managerClass, true, scriptLoader);
            } catch (Exception ex) {
                return null;
            }
        }
        try {
            runner = (ScriptRunnerBase) Class.forName(
                    runnerClass, true, scriptLoader).getDeclaredConstructor().newInstance();
            runner.setProject(project);
        } catch (Exception ex) {
            throw ReflectUtil.toBuildException(ex);
        }
        runner.setLanguage(language);
        runner.setScriptClassLoader(scriptLoader);
        return runner;
    }
}
