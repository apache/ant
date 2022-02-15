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

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;

/**
 * This is a helper class used by ScriptRunnerHelper to
 * create a ScriptRunner based on a classloader and on a language.
 */
public class ScriptRunnerCreator {
    private static class ScriptRunnerFactory {
        final String managerClass;
        final String runnerClass;
        
        ScriptRunnerFactory(String managerClass, String runnerClass) {
            this.managerClass = managerClass;
            this.runnerClass = runnerClass;
        }

        boolean validateManager(Project project, String language, ClassLoader scriptLoader) {
            try {
                Class.forName(managerClass, true, scriptLoader);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }

        ScriptRunnerBase getRunner(Project project, String language, ClassLoader scriptLoader) {
            if (!validateManager(project, language, scriptLoader)) {
                return null;
            }
            final ScriptRunnerBase runner;
            try {
                runner = Class.forName(runnerClass, true, scriptLoader)
                    .asSubclass(ScriptRunnerBase.class).getDeclaredConstructor().newInstance();

                runner.setProject(project);
            } catch (Exception ex) {
                throw ReflectUtil.toBuildException(ex);
            }
            runner.setLanguage(language);
            runner.setScriptClassLoader(scriptLoader);
            return runner;
        }
    }

    private static final Map<ScriptManager, ScriptRunnerFactory> RUNNER_FACTORIES;

    private static final String UTIL_OPT = MagicNames.ANT_CORE_PACKAGE + ".util.optional";

    private static final String BSF_PACK = "org.apache.bsf";
    private static final String BSF_MANAGER = BSF_PACK + ".BSFManager";
    private static final String BSF_RUNNER = UTIL_OPT + ".ScriptRunner";

    private static final String JAVAX_MANAGER = "javax.script.ScriptEngineManager";
    private static final String JAVAX_RUNNER = UTIL_OPT + ".JavaxScriptRunner";

    static {
        final Map<ScriptManager, ScriptRunnerFactory> m = new EnumMap<>(ScriptManager.class);
        
        m.put(ScriptManager.bsf, new ScriptRunnerFactory(BSF_MANAGER, BSF_RUNNER) {
            @Override
            boolean validateManager(Project project, String language, ClassLoader scriptLoader) {
                if (scriptLoader.getResource(LoaderUtils.classNameToResource(BSF_MANAGER)) == null) {
                    return false;
                }
                new ScriptFixBSFPath().fixClassLoader(scriptLoader, language);
                return true;
            }
        });

        m.put(ScriptManager.javax, new ScriptRunnerFactory(JAVAX_MANAGER, JAVAX_RUNNER));

        RUNNER_FACTORIES = Collections.unmodifiableMap(m);
    }

    private Project project;

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
     * @deprecated Use {@link #createRunner(ScriptManager,String,ClassLoader)} instead
     */
    @Deprecated
    public synchronized ScriptRunnerBase createRunner(String manager, String language,
        ClassLoader classLoader) {
        return createRunner(ScriptManager.valueOf(manager), language, classLoader);
    }

    /**
     * Create a ScriptRunner.
     * @param manager      the {@link ScriptManager}
     * @param language     the language.
     * @param classLoader  the classloader to use
     * @return the created script runner.
     * @throws BuildException if unable to create the ScriptRunner.
     */
    public synchronized ScriptRunnerBase createRunner(ScriptManager manager, String language,
        ClassLoader classLoader) {

        if (language == null) {
            throw new BuildException("script language must be specified");
        }
        if (manager == null) {
            throw new BuildException("Unsupported language prefix " + manager);
        }

        // Check for bsf first then javax
        // This version does not check if the scriptManager
        // supports the language.

        final Set<ScriptManager> managers;
        if (manager == ScriptManager.auto) {
            managers = EnumSet.complementOf(EnumSet.of(ScriptManager.auto));
        } else {
            managers = EnumSet.of(manager);
        }
        return managers.stream().map(RUNNER_FACTORIES::get)
            .map(f -> f.getRunner(project, language, classLoader)).findFirst()
            .orElseThrow(() -> new BuildException(
                managers.stream().map(RUNNER_FACTORIES::get).map(f -> f.managerClass).collect(
                    Collectors.joining("|", "Unable to load script engine manager (", ")"))));
    }
}
