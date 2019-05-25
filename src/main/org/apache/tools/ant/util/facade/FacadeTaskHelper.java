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

package org.apache.tools.ant.util.facade;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;

/**
 * Helper class for facade implementations - encapsulates treatment of
 * explicit implementation choices, magic properties and
 * implementation specific command line arguments.
 *
 *
 * @since Ant 1.5
 */
public class FacadeTaskHelper {

    /**
     * Command line arguments.
     */
    private List<ImplementationSpecificArgument> args = new ArrayList<>();

    /**
     * The explicitly chosen implementation.
     */
    private String userChoice;

    /**
     * The magic property to consult.
     */
    private String magicValue;

    /**
     * The default value.
     */
    private String defaultValue;

    /**
     * User specified path used as classpath when loading the implementation.
     */
    private Path implementationClasspath;

    /**
     * @param defaultValue The default value for the implementation.
     * Must not be null.
     */
    public FacadeTaskHelper(String defaultValue) {
        this(defaultValue, null);
    }

    /**
     * @param defaultValue The default value for the implementation.
     * Must not be null.
     * @param magicValue the value of a magic property that may hold a user.
     * choice.  May be null.
     */
    public FacadeTaskHelper(String defaultValue, String magicValue) {
        this.defaultValue = defaultValue;
        this.magicValue = magicValue;
    }

    /**
     * Used to set the value of the magic property.
     * @param magicValue the value of a magic property that may hold a user.
     */
    public void setMagicValue(String magicValue) {
        this.magicValue = magicValue;
    }

    /**
     * Used for explicit user choices.
     * @param userChoice the explicitly chosen implementation.
     */
    public void setImplementation(String userChoice) {
        this.userChoice = userChoice;
    }

    /**
     * Retrieves the implementation.
     * @return the implementation.
     */
    public String getImplementation() {
        return userChoice != null ? userChoice
                                  : (magicValue != null ? magicValue
                                                        : defaultValue);
    }

    /**
     * Retrieves the explicit user choice.
     * @return the explicit user choice.
     */
    public String getExplicitChoice() {
        return userChoice;
    }

    /**
     * Command line argument.
     * @param arg an argument to add.
     */
    public void addImplementationArgument(ImplementationSpecificArgument arg) {
        args.add(arg);
    }

    /**
     * Retrieves the command line arguments enabled for the current
     * facade implementation.
     * @return an array of command line arguments.
     */
    public String[] getArgs() {
        String implementation = getImplementation();
        return args.stream().map(arg -> arg.getParts(implementation))
            .filter(Objects::nonNull).flatMap(Stream::of)
            .toArray(String[]::new);
    }

    /**
     * Tests whether the implementation has been chosen by the user
     * (either via a magic property or explicitly.
     * @return true if magic or user choice has be set.
     * @since Ant 1.5.2
     */
    public boolean hasBeenSet() {
        return userChoice != null || magicValue != null;
    }

    /**
     * The classpath to use when loading the implementation.
     *
     * @param project the current project
     * @return a Path instance that may be appended to
     * @since Ant 1.8.0
     */
    public Path getImplementationClasspath(Project project) {
        if (implementationClasspath == null) {
            implementationClasspath = new Path(project);
        }
        return implementationClasspath;
    }
}
