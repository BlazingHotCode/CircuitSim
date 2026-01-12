package circuitsim.components;

import java.awt.Color;

public enum WireColor {
    WHITE("White", new Color(235, 235, 240)),
    RED("Red", new Color(220, 80, 80)),
    BLUE("Blue", new Color(90, 140, 230)),
    GREEN("Green", new Color(90, 190, 120)),
    YELLOW("Yellow", new Color(220, 200, 90));

    private final String name;
    private final Color color;

    WireColor(String name, Color color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }
}
