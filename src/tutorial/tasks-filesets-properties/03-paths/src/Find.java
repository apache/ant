import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.DirectoryScanner;

import java.util.Vector;
import java.util.Iterator;
import java.io.File;

public class Find extends Task {

    private String file;
    private String location;
    private Vector paths = new Vector();

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
        if (file==null) throw new BuildException("file not set");
        if (location==null) throw new BuildException("location not set");
        if (paths.size()<1) throw new BuildException("path not set");
    }

    public void execute() {
        validate();
        String foundLocation = null;
        for(Iterator itPaths = paths.iterator(); itPaths.hasNext(); ) {
            Path path = (Path)itPaths.next();
            String[] includedFiles = path.list();
            for(int i=0; i<includedFiles.length; i++) {
                String filename = includedFiles[i].replace('\\','/');
                filename = filename.substring(filename.lastIndexOf("/")+1);
                if (foundLocation==null && file.equals(filename)) {
                    foundLocation = includedFiles[i];
                }
            }
        }
        if (foundLocation!=null)
            getProject().setNewProperty(location, foundLocation);
    }

}
