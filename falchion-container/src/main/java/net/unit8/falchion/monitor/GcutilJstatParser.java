package net.unit8.falchion.monitor;

/**
 * @author kawasima
 */
public class GcutilJstatParser {
    public GcStat parse(String line) {
        String[] tokens = line.trim().split("\\s+");
        return new GcStat(
                Double.parseDouble(tokens[0]),
                Double.parseDouble(tokens[1]),
                Double.parseDouble(tokens[2]),
                Double.parseDouble(tokens[3]),
                Double.parseDouble(tokens[4]),
                Double.parseDouble(tokens[5]),
                Long.parseLong(tokens[6]),
                Double.parseDouble(tokens[7]),
                Long.parseLong(tokens[8]),
                Double.parseDouble(tokens[9]),
                Double.parseDouble(tokens[10])
                );
    }
}
