/** (C) Copyright 2004 Hewlett-Packard Development Company, LP

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 For more information: www.smartfrog.org

 */


package org.apache.tools.ant.taskdefs.rmic;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Rmic;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;

import java.io.IOException;

/**
 * This is an extension of the sun rmic compiler, which forks rather than
 * executes it inline. Why so? Because rmic is dog slow, but if you fork the
 * compiler you can have multiple copies compiling different bits of your project
 * at the same time. Which, on a multi-cpu system results in significant speedups.
 *
 * @since ant1.7
 */
public class ForkingSunRmic extends DefaultRmicAdapter {

    /**
     * the name of this adapter for users to select
     */
    public static final String COMPILER_NAME = "forking";
    
    /**
     * exec by creating a new command
     * @return
     * @throws BuildException
     */
    public boolean execute() throws BuildException {
        Rmic owner=getRmic();
        Commandline cmd = setupRmicCommand();
        Project project=owner.getProject();
        //rely on RMIC being on the path
        cmd.setExecutable(SunRmic.RMIC_EXECUTABLE);

        //set up the args
        String[] args=cmd.getCommandline();

        try {
            Execute exe = new Execute(new LogStreamHandler(owner,
                    Project.MSG_INFO,
                    Project.MSG_WARN));
            exe.setAntRun(project);
            exe.setWorkingDirectory(project.getBaseDir());
            exe.setCommandline(args);

            exe.execute();
            return exe.getExitValue()==0;
        } catch (IOException exception) {
            throw new BuildException("Error running "+ SunRmic.RMIC_EXECUTABLE
                    +" -maybe it is not on the path" ,
                    exception);
        }
    }
}
