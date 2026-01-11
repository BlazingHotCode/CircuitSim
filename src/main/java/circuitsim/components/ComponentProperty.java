package circuitsim.components;

public interface ComponentProperty {
    String getName();

    ComponentPropertyType getType();

    Object getEditorValue();

    void setValueFromEditor(Object value);

    String getDisplayValue();

    boolean isDisplayable();

    boolean isEditable();
}
