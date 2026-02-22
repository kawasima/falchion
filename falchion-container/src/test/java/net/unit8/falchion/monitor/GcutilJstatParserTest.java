package net.unit8.falchion.monitor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GcutilJstatParserTest {

    private final GcutilJstatParser parser = new GcutilJstatParser();

    @Test
    void parseJava8Format_11columns() {
        // S0 S1 E O M CCS YGC YGCT FGC FGCT GCT
        String line = "  85.16   0.00   0.00  70.51  99.23  97.60     22    0.024     2    0.020    0.044";
        GcStat stat = parser.parse(line);

        assertThat(stat.getS0()).isEqualTo(85.16);
        assertThat(stat.getS1()).isEqualTo(0.0);
        assertThat(stat.getE()).isEqualTo(0.0);
        assertThat(stat.getO()).isEqualTo(70.51);
        assertThat(stat.getM()).isEqualTo(99.23);
        assertThat(stat.getCcs()).isEqualTo(97.60);
        assertThat(stat.getYgc()).isEqualTo(22L);
        assertThat(stat.getYgct()).isEqualTo(0.024);
        assertThat(stat.getFgc()).isEqualTo(2L);
        assertThat(stat.getFgct()).isEqualTo(0.020);
        assertThat(stat.getGct()).isEqualTo(0.044);
    }

    @Test
    void parseJava9PlusFormat_13columns() {
        // S0 S1 E O M CCS YGC YGCT FGC FGCT CGC CGCT GCT
        String line = "  85.16   0.00   0.00  70.51  99.23  97.60     22    0.024     2    0.020     3    0.005    0.049";
        GcStat stat = parser.parse(line);

        assertThat(stat.getS0()).isEqualTo(85.16);
        assertThat(stat.getYgc()).isEqualTo(22L);
        assertThat(stat.getYgct()).isEqualTo(0.024);
        assertThat(stat.getFgc()).isEqualTo(2L);
        assertThat(stat.getFgct()).isEqualTo(0.020);
        // GCT should come from index 12, not index 10 (which is CGC=3)
        assertThat(stat.getGct()).isEqualTo(0.049);
    }

    @Test
    void parseDashValues() {
        // jstat outputs "-" when a value is unavailable
        String line = "  -   0.00   0.00  70.51  99.23  97.60     22    0.024     2    0.020    0.044";
        GcStat stat = parser.parse(line);

        assertThat(stat.getS0()).isEqualTo(0.0);
        assertThat(stat.getGct()).isEqualTo(0.044);
    }

    @Test
    void parseJava9PlusWithDashes() {
        // Java 9+ format with "-" in CGC/CGCT columns
        String line = "  -  35.51   1.75   0.20  96.57  87.77      1     0.002     0     0.000     -     -     0.002";
        GcStat stat = parser.parse(line);

        assertThat(stat.getS0()).isEqualTo(0.0);
        assertThat(stat.getS1()).isEqualTo(35.51);
        assertThat(stat.getYgc()).isEqualTo(1L);
        assertThat(stat.getFgc()).isEqualTo(0L);
        assertThat(stat.getFgct()).isEqualTo(0.0);
        assertThat(stat.getGct()).isEqualTo(0.002);
    }
}
