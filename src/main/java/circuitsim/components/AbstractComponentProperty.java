package circuitsim.components;

public abstract class AbstractComponentProperty implements ComponentProperty {
    private final String name;
    private final ComponentPropertyType type;
    private final boolean displayable;
    private final boolean editable;

    protected AbstractComponentProperty(String name, ComponentPropertyType type, boolean displayable, boolean editable) {
        this.name = name;
        this.type = type;
        this.displayable = displayable;
        this.editable = editable;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ComponentPropertyType getType() {
        return type;
    }

    @Override
    public boolean isDisplayable() {
        return displayable;
    }

    @Override
    public boolean isEditable() {
        return editable;
    }
}
