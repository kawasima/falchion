package net.unit8.falchion;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author syobochim
 */
public class ContainerTest {

    @Test
    public void createClasspathWithBaseDirAndAplVersion() {
        Container sut = new Container(1);
        String basePath = new File(ContainerTest.class.getClassLoader().getResource("containerTestResources").getPath())
                .getAbsolutePath();

        String versionFolder = basePath + File.separator + "0.1.0" + File.separator;
        assertThat(sut.createClasspath(basePath, "0.1.0")).startsWith(basePath + ":");
        assertThat(sut.createClasspath(basePath, "0.1.0")).contains(versionFolder + "applicationA.jar");
        assertThat(sut.createClasspath(basePath, "0.1.0"))
                .contains(versionFolder + "subDir" + File.separator + "applicationB.jar");
        assertThat(sut.createClasspath(basePath, "0.1.1")).isEqualTo(basePath);
    }

    @Test
    public void returnBaseDirWhenFileNotFound() {
        Container sut = new Container(1);
        String basePath = new File(ContainerTest.class.getClassLoader().getResource("containerTestResources").getPath())
                .getAbsolutePath();
        assertThat(sut.createClasspath(basePath, "0.2.0")).isEqualTo(basePath);
    }
}
