package circuitsim.components.properties;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Editable string property for a component.
 */
public class StringProperty extends AbstractComponentProperty {
    private final Supplier<String> getter;
    private final Consumer<String> setter;

    public StringProperty(String name, Supplier<String> getter, Consumer<String> setter,
                          boolean displayable, boolean editable) {
        super(name, ComponentPropertyType.STRING, displayable, editable);
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public Object getEditorValue() {
        return getter == null ? "" : getter.get();
    }

    @Override
    public void setValueFromEditor(Object value) {
        if (setter == null) {
            return;
        }
        setter.accept(value == null ? "" : value.toString());
    }

    @Override
    public String getDisplayValue() {
        Object value = getEditorValue();
        return value == null ? "" : value.toString();
    }
}
