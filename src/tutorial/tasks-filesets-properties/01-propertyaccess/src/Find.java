import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

public class Find extends Task {

    private String property;
    private String value;
    private String print;

    public void setProperty(String property) {
        this.property = property;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setPrint(String print) {
        this.print = print;
    }

    public void execute() {
        if (print != null) {
            String propValue = getProject().getProperty(print);
            log(propValue);
        } else {
            if (property == null) throw new BuildException("property not set");
            if (value    == null) throw new BuildException("value not set");
            getProject().setNewProperty(property, value);
        }
    }

}
