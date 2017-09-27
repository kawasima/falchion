package net.unit8.falchion;

import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author syobochim
 */
public class ContainerTest {

    @Test
    public void createClasspathWithBaseDirAndAplVersion() throws Exception {
        Container sut = new Container(1);
        String basePath = new File(ContainerTest.class.getClassLoader().getResource("containerTestResources").getPath())
                .getAbsolutePath();

        String versionFolder = basePath + File.separator + "0.1.0" + File.separator;
        assertThat(sut.createClasspath(basePath, "0.1.0"), startsWith(basePath + ":"));
        assertThat(sut.createClasspath(basePath, "0.1.0"), containsString(versionFolder + "applicationA.jar"));
        assertThat(sut.createClasspath(basePath, "0.1.0"),
                containsString(versionFolder + "subDir" + File.separator + "applicationB.jar"));
        assertThat(sut.createClasspath(basePath, "0.1.1"), is(basePath));
    }

    @Test
    public void returnBaseDirWhenFileNotFound() throws Exception {
        Container sut = new Container(1);
        String basePath = new File(ContainerTest.class.getClassLoader().getResource("containerTestResources").getPath())
                .getAbsolutePath();
        assertThat(sut.createClasspath(basePath, "0.2.0"), is(basePath));
    }
}