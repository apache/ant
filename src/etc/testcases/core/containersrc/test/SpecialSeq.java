package test;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileSet;
import java.util.*;

public class SpecialSeq extends Task implements TaskContainer {
    /** Optional Vector holding the nested tasks */
    private Vector nestedTasks = new Vector();

    private FileSet fileset;
    
    /**
     * Add a nested task.
     * <p>
     * @param nestedTask  Nested task to execute
     * <p>
     */
    public void addTask(Task nestedTask) {
        nestedTasks.addElement(nestedTask);
    }

    /**
     * Execute all nestedTasks.
     */
    public void execute() throws BuildException {
        for (Enumeration e = nestedTasks.elements(); e.hasMoreElements();) {
            Task nestedTask = (Task) e.nextElement();
            nestedTask.perform();
        }
    }

    public void addFileset(FileSet fileset) {
        this.fileset = fileset;
    }
}
