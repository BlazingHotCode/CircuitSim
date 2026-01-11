package circuitsim.components;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class BooleanProperty extends AbstractComponentProperty {
    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;

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
