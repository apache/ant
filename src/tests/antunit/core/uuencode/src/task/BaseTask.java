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
package task;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.FileUtils;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * Base class for the uuencode/decode test tasks.
 */
abstract public class BaseTask extends Task {
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    private File inFile;
    private File outFile;

    public void setInFile(File inFile) {
        this.inFile = inFile;
    }
    protected File getInFile() {
        return inFile;
    }
    public void setOutFile(File outFile) {
        this.outFile = outFile;
    }
    protected File getOutFile() {
        return outFile;
    }
    public void execute() {
        assertAttribute(inFile, "inFile");
        assertAttribute(outFile, "outFile");
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = new BufferedInputStream(
                Files.newInputStream(getInFile().toPath()));
            outputStream = Files.newOutputStream(getOutFile().toPath());
            doit(inputStream, outputStream);
        } catch (Exception ex) {
            throw new BuildException(ex);
        } finally {
            FILE_UTILS.close(inputStream);
            FILE_UTILS.close(outputStream);
        }
    }

    abstract protected void doit(
        InputStream is, OutputStream os) throws Exception;

    private void assertAttribute(File file, String attributeName) {
        if (file == null) {
            throw new BuildException("Required attribute " + attributeName
                                     + " not set");
        }
    }
}
