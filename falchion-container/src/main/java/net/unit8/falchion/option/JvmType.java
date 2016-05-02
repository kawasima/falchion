package net.unit8.falchion.option;

/**
 * @author kawasima
 */
public enum JvmType {
    SERVER("-server"),
    CLIENT("-client");

    private String optionValue;

    JvmType(String optionValue) {
        this.optionValue = optionValue;
    }

    public String getOptionValue() {
        return optionValue;
    }
}
