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
package org.apache.tools.ant.taskdefs.optional.native2ascii;

import java.lang.reflect.Method;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.taskdefs.optional.Native2Ascii;
import org.apache.tools.ant.types.Commandline;

/**
 * Adapter to sun.tools.native2ascii.Main.
 *
 * @since Ant 1.6.3
 */
public final class SunNative2Ascii extends DefaultNative2Ascii {

    /**
     * Identifies this adapter.
     */
    public static final String IMPLEMENTATION_NAME = "sun";

    private static final String SUN_TOOLS_NATIVE2ASCII_MAIN = "sun.tools.native2ascii.Main";

    /** {@inheritDoc} */
    @Override
    protected void setup(Commandline cmd, Native2Ascii args)
        throws BuildException {
        if (args.getReverse()) {
            cmd.createArgument().setValue("-reverse");
        }
        super.setup(cmd, args);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean run(Commandline cmd, ProjectComponent log)
        throws BuildException {
        try {
            Class<?> n2aMain = Class.forName(SUN_TOOLS_NATIVE2ASCII_MAIN);
            Method convert = n2aMain.getMethod("convert", String[].class);
            return Boolean.TRUE.equals(convert.invoke(n2aMain.getDeclaredConstructor().newInstance(),
                (Object) cmd.getArguments()));
        } catch (BuildException ex) {
            //rethrow
            throw ex;
        } catch (NoSuchMethodException ex) {
            throw new BuildException("Could not find convert() method in %s",
                SUN_TOOLS_NATIVE2ASCII_MAIN);
        } catch (Exception ex) {
            //wrap
           throw new BuildException("Error starting Sun's native2ascii: ", ex);
        }
    }
}
