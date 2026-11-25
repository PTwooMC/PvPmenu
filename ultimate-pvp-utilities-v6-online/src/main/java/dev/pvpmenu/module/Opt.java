package dev.pvpmenu.module;

/** One extra setting of a module. The menu turns each of these into a switch, chips, a slider or a colour picker. */
public final class Opt {
    public enum Type { ONOFF, CHOICE, SLIDER, COLOR, TEXT }

    public final Type type;
    public final String label;
    public final String name;
    public final String[] choices;
    public final int min;
    public final int max;
    public final int def;
    public final String suffix;

    public String defText = "";

    private Opt(Type type, String label, String name, String[] choices, int min, int max, int def, String suffix) {
        this.type = type;
        this.label = label;
        this.name = name;
        this.choices = choices;
        this.min = min;
        this.max = max;
        this.def = def;
        this.suffix = suffix;
    }

    public static Opt onOff(String label, String name, boolean def) {
        return new Opt(Type.ONOFF, label, name, null, 0, 1, def ? 1 : 0, "");
    }

    public static Opt choice(String label, String name, int def, String... choices) {
        return new Opt(Type.CHOICE, label, name, choices, 0, choices.length - 1, def, "");
    }

    public static Opt slider(String label, String name, int min, int max, int def, String suffix) {
        return new Opt(Type.SLIDER, label, name, null, min, max, def, suffix);
    }

    public static Opt color(String label, String name, int def) {
        return new Opt(Type.COLOR, label, name, null, 0, 0, def, "");
    }

    public static Opt text(String label, String name, String def) {
        Opt o = new Opt(Type.TEXT, label, name, null, 0, 0, 0, "");
        o.defText = def;
        return o;
    }
}
