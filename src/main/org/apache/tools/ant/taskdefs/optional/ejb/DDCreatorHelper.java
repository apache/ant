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
package org.apache.tools.ant.taskdefs.optional.ejb;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import javax.ejb.deployment.DeploymentDescriptor;

/**
 * A helper class which performs the actual work of the ddcreator task.
 *
 * This class is run with a classpath which includes the weblogic tools and the home and remote
 * interface class files referenced in the deployment descriptors being built.
 *
 */
public final class DDCreatorHelper {
    /**
     * The root directory of the tree containing the textual deployment descriptors.
     */
    private File descriptorDirectory;

    /**
     * The directory where generated serialised deployment descriptors are written.
     */
    private File generatedFilesDirectory;

    // CheckStyle:VisibilityModifier OFF - bc
    /**
     * The descriptor text files for which a serialised descriptor is to be created.
     */
    String[] descriptors;
    // CheckStyle:VisibilityModifier ON

    /**
     * The main method.
     *
     * The main method creates an instance of the DDCreatorHelper, passing it the
     * args which it then processes.
     * @param args the arguments
     * @throws Exception on error
     */
    public static void main(String[] args) throws Exception {
        DDCreatorHelper helper = new DDCreatorHelper(args);
        helper.process();
    }

    /**
     * Initialise the helper with the command arguments.
     *
     */
    private DDCreatorHelper(String[] args) {
        int index = 0;
        descriptorDirectory = new File(args[index++]);
        generatedFilesDirectory = new File(args[index++]);

        descriptors = new String[args.length - index];
        for (int i = 0; index < args.length; ++i) {
            descriptors[i] = args[index++];
        }
    }

    /**
     * Do the actual work.
     *
     * The work proceeds by examining each descriptor given. If the serialised
     * file does not exist or is older than the text description, the weblogic
     * DDCreator tool is invoked directly to build the serialised descriptor.
     */
    private void process() throws Exception {
        for (int i = 0; i < descriptors.length; ++i) {
            String descriptorName = descriptors[i];
            File descriptorFile = new File(descriptorDirectory, descriptorName);

            int extIndex = descriptorName.lastIndexOf(".");
            String serName = null;
            if (extIndex != -1) {
                serName = descriptorName.substring(0, extIndex) + ".ser";
            } else {
                serName = descriptorName + ".ser";
            }
            File serFile = new File(generatedFilesDirectory, serName);

            // do we need to regenerate the file
            if (!serFile.exists() || serFile.lastModified() < descriptorFile.lastModified()
                || regenerateSerializedFile(serFile)) {

                String[] args = {"-noexit",
                                 "-d", serFile.getParent(),
                                 "-outputfile", serFile.getName(),
                                 descriptorFile.getPath()};
                try {
                    weblogic.ejb.utils.DDCreator.main(args);
                } catch (Exception e) {
                    // there was an exception - run with no exit to get proper error
                    String[] newArgs = {"-d", generatedFilesDirectory.getPath(),
                                 "-outputfile", serFile.getName(),
                                 descriptorFile.getPath()};
                    weblogic.ejb.utils.DDCreator.main(newArgs);
                }
            }
        }
    }

    /**
     * EJBC will fail if the serialized descriptor file does not match the bean classes.
     * You can test for this by trying to load the deployment descriptor.  If it fails,
     * the serialized file needs to be regenerated because the associated class files
     * don't match.
     */
    private boolean regenerateSerializedFile(File serFile) {
        try {

            FileInputStream fis = new FileInputStream(serFile);
            ObjectInputStream ois = new ObjectInputStream(fis);
            DeploymentDescriptor dd = (DeploymentDescriptor) ois.readObject();
            fis.close();

            // Since the descriptor read properly, everything should be o.k.
            return false;

        } catch (Exception e) {

            // Weblogic will throw an error if the deployment descriptor does
            // not match the class files.
            return true;

        }
    }
}
