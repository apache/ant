package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.*;
    
import java.io.*;

/**
 * Parses output from jikes and
 * passes errors and warnings
 * into the right logging channels of Project.
 *
 * TODO: 
 * Parsing could be much better
 * @author skanthak@muehlheim.de
 */
public class JikesOutputParser {
    protected Task task;
    protected boolean errorFlag = false; // no errors so far
    protected int errors,warnings;
    protected boolean error = false;
    protected boolean emacsMode;
    
    /**
     * Construct a new Parser object
     * @param task - task in whichs context we are called
     */
    protected JikesOutputParser(Task task, boolean emacsMode) {
	super();
	this.task = task;
        this.emacsMode = emacsMode;
    }

    /**
     * Parse the output of a jikes compiler
     * @param reader - Reader used to read jikes's output
     */
    protected void parseOutput(BufferedReader reader) throws IOException {
       if (emacsMode)
           parseEmacsOutput(reader);
       else
           parseStandardOutput(reader);
    }

    private void parseStandardOutput(BufferedReader reader) throws IOException {
	String line;
	String lower;
	// We assume, that every output, jike does, stands for an error/warning
	// XXX 
	// Is this correct?

        // TODO:
        // A warning line, that shows code, which contains a variable
        // error will cause some trouble. The parser should definitely
        // be much better.

	while ((line = reader.readLine()) != null) {
	    lower = line.toLowerCase();
	    if (line.trim().equals(""))
		continue;
	    if (lower.indexOf("error") != -1)
		setError(true);
	    else if (lower.indexOf("warning") != -1)
                setError(false);
            else {
                // If we don't know the type of the line
                // and we are in emacs mode, it will be
                // an error, because in this mode, jikes won't
                // always print "error", but sometimes other
                // keywords like "Syntax". We should look for
                // all those keywords.
                if (emacsMode)
                    setError(true);
            }
            log(line);
	}
    }

    private void parseEmacsOutput(BufferedReader reader) throws IOException {
       // This may change, if we add advanced parsing capabilities.
       parseStandardOutput(reader);
    }

    private void setError(boolean err) {
        error = err;
        if(error)
            errorFlag = true;
    }

    private void log(String line) {
       if (!emacsMode) {
           task.log("", (error ? Project.MSG_ERR : Project.MSG_WARN));
       }
       task.log(line, (error ? Project.MSG_ERR : Project.MSG_WARN));
    }

    /**
     * Indicate if there were errors during the compile
     * @return if errors ocured
     */
    protected boolean getErrorFlag() {
	return errorFlag;
    }
}
