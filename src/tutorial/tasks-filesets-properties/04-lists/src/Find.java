import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.DirectoryScanner;

import java.util.Vector;
import java.util.Iterator;
import java.io.File;

public class Find extends Task {

    // =====  internal attributes  =====

    private Vector foundFiles = new Vector();

    // =====  attribute support  =====

    private String file;
    private String location;
    private Vector paths = new Vector();
    private String delimiter = null;

    public void setFile(String file) {
        this.file = file;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void addPath(Path path) {
        paths.add(path);
    }

    public void setDelimiter(String delim) {
        delimiter = delim;
    }

    // =====  the tasks work  =====

    protected void validate() {
        if (file==null) throw new BuildException("file not set");
        if (location==null) throw new BuildException("location not set");
        if (paths.size()<1) throw new BuildException("path not set");
    }

    public void execute() {
        validate();
        for(Iterator itPaths = paths.iterator(); itPaths.hasNext(); ) {
            Path path = (Path)itPaths.next();
            String[] includedFiles = path.list();
            for(int i=0; i<includedFiles.length; i++) {
                String filename = includedFiles[i].replace('\\','/');
                filename = filename.substring(filename.lastIndexOf("/")+1);
                if (file.equals(filename) && !foundFiles.contains(includedFiles[i])) {
                    foundFiles.add(includedFiles[i]);
                }
            }
        }
        String rv = null;
        if (foundFiles.size() > 0) {
            if (delimiter==null) {
                // only the first
                    rv = (String)foundFiles.elementAt(0);
            } else {
                // create list
                StringBuffer list = new StringBuffer();
                for(Iterator it=foundFiles.iterator(); it.hasNext(); ) {
                    list.append(it.next());
                    if (it.hasNext()) list.append(delimiter);
                }
                rv = list.toString();
            }
        }


        if (rv!=null)
            getProject().setNewProperty(location, rv);
    }

}
