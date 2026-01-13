package circuitsim.components.properties;

/**
 * Represents a property that can be displayed and optionally edited in the UI.
 */
public interface ComponentProperty {
    /**
     * @return human-readable property name
     */
    String getName();

    /**
     * @return the UI/editor type for this property
     */
    ComponentPropertyType getType();

    /**
     * @return current value used by the editor widget
     */
    Object getEditorValue();

    /**
     * Updates the property from a UI/editor value.
     *
     * @param value editor-provided value
     */
    void setValueFromEditor(Object value);

    /**
     * @return formatted string for in-canvas display
     */
    String getDisplayValue();

    /**
     * @return true when the property should be rendered on the canvas
     */
    boolean isDisplayable();

    /**
     * @return true when the property should be editable in the UI
     */
    boolean isEditable();
}
