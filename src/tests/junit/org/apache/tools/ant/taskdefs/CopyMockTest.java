package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import java.io.File;
import java.nio.file.Paths;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

public class CopyMockTest {

    private Copy copyTask;
    private Project mockProject;

    @Before
    public void setUp() {
        // 1. Initiative Copy Task
        copyTask = new Copy();
        
        // 2. Mock Ant Project Environment
        mockProject = Mockito.mock(Project.class);
        copyTask.setProject(mockProject);
    }

    @Test
    public void testCopySourceFileDoesNotExist() {
        // 3. Mock a fake source file (java.io.File)
        File mockSourceFile = Mockito.mock(File.class);
        
        // Tell this fake file: When someone asks if it exist, answer "No (false)"
        Mockito.when(mockSourceFile.exists()).thenReturn(false);
        
        // To prevent Ant from throwing a NullPointerException when internally converting paths, we pass it a fake path.
        Mockito.when(mockSourceFile.toPath()).thenReturn(Paths.get("dummy/missing.txt"));
        Mockito.when(mockSourceFile.getAbsolutePath()).thenReturn("/dummy/missing.txt");

        // 4. Configure the parameters for the Copy task.
        copyTask.setFile(mockSourceFile);
        copyTask.setTodir(new File("/dummy/dest")); // A fake path to any destination
        
        // Set failonerror to false so that the task will not be interrupted when it finds a file missing, but will instead rewrite the log.
        copyTask.setFailOnError(false); 

        // 5. Execute
        copyTask.execute();

        // 6. Behavior Verification
        // verification 1: The copy task checked whether the file existed. (exists() is called at least once)
        Mockito.verify(mockSourceFile, Mockito.atLeastOnce()).exists();
        
        // verification 2: The copy task detected that the file was missing and promptly sent a log notification to the Project.
        Mockito.verify(mockProject, Mockito.atLeastOnce()).log(eq(copyTask), anyString(), anyInt());
    }
}