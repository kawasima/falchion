package net.unit8.falchion.monitor;

/**
 * @author kawasima
 */
public class GcutilJstatParser {
    private static double parseDouble(String s) {
        return "-".equals(s) ? 0.0 : Double.parseDouble(s);
    }

    private static long parseLong(String s) {
        return "-".equals(s) ? 0L : Long.parseLong(s);
    }

    public GcStat parse(String line) {
        String[] tokens = line.trim().split("\\s+");

        // Java 9+ adds CGC and CGCT columns between FGCT and GCT:
        //   S0 S1 E O M CCS YGC YGCT FGC FGCT CGC CGCT GCT  (13 columns)
        // Java 8:
        //   S0 S1 E O M CCS YGC YGCT FGC FGCT GCT            (11 columns)
        int gctIndex = tokens.length >= 13 ? 12 : 10;

        return new GcStat(
                parseDouble(tokens[0]),
                parseDouble(tokens[1]),
                parseDouble(tokens[2]),
                parseDouble(tokens[3]),
                parseDouble(tokens[4]),
                parseDouble(tokens[5]),
                parseLong(tokens[6]),
                parseDouble(tokens[7]),
                parseLong(tokens[8]),
                parseDouble(tokens[9]),
                parseDouble(tokens[gctIndex])
                );
    }
}
