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
import org.apache.tools.ant.types.Path;

import java.util.ArrayList;
import java.util.List;

public class Find extends Task {

    private String file;
    private String location;
    private List<Path> paths = new ArrayList<>();

    public void setFile(String file) {
        this.file = file;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void addPath(Path path) {
        paths.add(path);
    }

    protected void validate() {
        if (file == null) throw new BuildException("file not set");
        if (location == null) throw new BuildException("location not set");
        if (paths.size() < 1) throw new BuildException("path not set");
    }

    public void execute() {
        validate();
        String foundLocation = null;
        for (Path path : paths) {
            for (String includedFile : path.list()) {
                String filename = includedFile.replace('\\','/');
                filename = filename.substring(filename.lastIndexOf("/") + 1);
                if (foundLocation == null && file.equals(filename)) {
                    foundLocation = includedFiles[i];
                }
            }
        }
        if (foundLocation != null)
            getProject().setNewProperty(location, foundLocation);
    }

}
