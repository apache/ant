package org.apache.tools.ant.taskdefs;

import java.io.*;
import org.apache.tools.ant.*;

/**
 * Encapsulates a Jikes compiler, by
 * directly executing an external process.
 * @author skanthak@muehlheim.de
 */
public class Jikes {
    protected JikesOutputParser jop;
    protected String command;
    
    /**
     * Constructs a new Jikes obect.
     * @param jop - Parser to send jike's output to
     * @param command - name of jikes executeable
     */
    protected Jikes(JikesOutputParser jop,String command) {
        super();
        this.jop = jop;
        this.command = command;
    }

    /**
     * Do the compile with the specified arguments.
     * @param args - arguments to pass to process on command line
     */
    protected void compile(String[] args) {
        String[] commandArray = new String[args.length+1];
        commandArray[0] = command;
        System.arraycopy(args,0,commandArray,1,args.length);
        
        // We assume, that everything jikes writes goes to
        // standard output, not to standard error. The option
        // -Xstdout that is given to Jikes in Javac.doJikesCompile()
        // should guarantee this. At least I hope so. :)
        try {
            Process jikes = Runtime.getRuntime().exec(commandArray);
            BufferedReader reader = new BufferedReader(new InputStreamReader(jikes.getInputStream()));
            jop.parseOutput(reader);
        } catch (IOException e) {
            throw new BuildException("Error running Jikes compiler", e);                
        }
    }
}
