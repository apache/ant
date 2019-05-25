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
import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.DirectoryScanner;

import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class Find extends Task {

    private String file;
    private String location;
    private List<FileSet> filesets = new ArrayList<>();

    public void setFile(String file) {
        this.file = file;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void addFileset(FileSet fileset) {
        filesets.add(fileset);
    }

    protected void validate() {
        if (file == null) throw new BuildException("file not set");
        if (location == null) throw new BuildException("location not set");
        if (filesets.size() < 1) throw new BuildException("fileset not set");
    }

    public void execute2() {
        validate();
        String foundLocation = null;
        for (FileSet fs : filesets) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            File base  = ds.getBasedir();
            for (String includedFile : ds.getIncludedFiles()) {
                String filename = includedFile.replace('\\','/');
                filename = filename.substring(filename.lastIndexOf("/") + 1);
                if (foundLocation == null && file.equals(filename)) {
                    File found = new File(base, includedFile);
                    foundLocation = found.getAbsolutePath();
                }
            }
        }
        if (foundLocation != null)
            getProject().setNewProperty(location, foundLocation);
    }

}
