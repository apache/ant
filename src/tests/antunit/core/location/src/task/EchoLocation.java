package task;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

public class EchoLocation extends Task {
	public void execute() {
        log("Line: " + getLocation().getLineNumber(), Project.MSG_INFO);
    }
}