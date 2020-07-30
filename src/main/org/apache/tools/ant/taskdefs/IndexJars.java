package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileNameMapper;

public class IndexJars extends Path {

    private Mapper mapperElement;

    public IndexJars(Project project) {
        super(project);
    }

    public void add(FileNameMapper fileNameMapper) {
        createMapper().add(fileNameMapper);
    }

    public Mapper createMapper() throws BuildException {
        if (mapperElement != null) {
            throw new BuildException("Cannot define more than one mapper",
                    getLocation());
        }
        mapperElement = new Mapper(getProject());
        return mapperElement;
    }

    FileNameMapper getMapper() {
        if (mapperElement == null) {
            return null;
        }
        return mapperElement.getImplementation();
    }

}
