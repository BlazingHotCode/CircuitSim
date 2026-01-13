package circuitsim.components.properties;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Boolean-backed property with simple getter/setter wiring.
 */
public class BooleanProperty extends AbstractComponentProperty {
    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;

    /**
     * @param name display name for the property
     * @param getter supplies the current value
     * @param setter updates the current value
     * @param displayable whether to show the value on the canvas
     */
    public BooleanProperty(String name, Supplier<Boolean> getter, Consumer<Boolean> setter, boolean displayable) {
        super(name, ComponentPropertyType.BOOLEAN, displayable, true);
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public Object getEditorValue() {
        return getter.get();
    }

    @Override
    public void setValueFromEditor(Object value) {
        if (value instanceof Boolean) {
            setter.accept((Boolean) value);
        }
    }

    @Override
    public String getDisplayValue() {
        return String.valueOf(getter.get());
    }
}
