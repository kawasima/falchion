package net.unit8.falchion;

/**
 * @author kawasima
 */
public class JvmResult {
    private String id;
    private int exitStatus;
    private long pid;

    public JvmResult(String id, long pid, int exitStatus) {
        this.id = id;
        this.pid = pid;
        this.exitStatus = exitStatus;
    }

    public String getId() {
        return id;
    }

    public int getExitStatus() {
        return exitStatus;
    }

    public long getPid() {
        return pid;
    }
}
