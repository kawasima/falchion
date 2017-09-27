package net.unit8.falchion.monitor;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.Locale;

/**
 * @author kawasima
 */
public class GcStat implements MonitorStat {
    private Double e;

    private Double s0;
    private Double s1;

    private Long ygc;
    private Double ygct;

    private Double o;

    private Long fgc;
    private Double fgct;
    private Double gct;

    private Double m;

    private Double ccs;

    public GcStat(Double s0, Double s1, Double e, Double o, Double m, Double ccs, Long ygc, Double ygct, Long fgc, Double fgct, Double gct) {
        this.e = e;
        this.s0 = s0;
        this.s1 = s1;
        this.ygc = ygc;
        this.ygct = ygct;
        this.o = o;
        this.fgc = fgc;
        this.fgct = fgct;
        this.gct = gct;
        this.m = m;
        this.ccs = ccs;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "S0=%.2f, S1=%.2f, E=%.2f, O=%.2f, M=%.2f, CCS=%.2f"
                + ", YGC=%d, YGCT=%.3f, FGC=%d, FGCT=%.3f, GCT=%.3f",
                s0, s1, e, o, m, ccs, ygc, ygct, fgc, fgct, gct);
    }

    @Override
    public JsonObject toJson() {
        return Json.createObjectBuilder()
                .add("type", "gcutil")
                .add("S0", s0)
                .add("S1", s1)
                .add("E",  e)
                .add("O",  o)
                .add("M",  m)
                .add("CCS", ccs)
                .add("YGC", ygc)
                .add("YGCT", ygct)
                .add("FGC", fgc)
                .add("FGCT", fgct)
                .add("GCT", gct)
                .build();

    }

    public Double getE() {
        return e;
    }

    public Double getS0() {
        return s0;
    }

    public Double getS1() {
        return s1;
    }

    public Long getYgc() {
        return ygc;
    }

    public Double getYgct() {
        return ygct;
    }

    public Double getO() {
        return o;
    }

    public Long getFgc() {
        return fgc;
    }

    public Double getFgct() {
        return fgct;
    }

    public Double getGct() {
        return gct;
    }

    public Double getM() {
        return m;
    }

    public Double getCcs() {
        return ccs;
    }
}
