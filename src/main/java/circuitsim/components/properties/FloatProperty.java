package circuitsim.components.properties;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Float-backed property with formatted display values.
 */
public class FloatProperty extends AbstractComponentProperty {
    private static final DecimalFormat VALUE_FORMAT =
            new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));

    private final Supplier<Float> getter;
    private final Consumer<Float> setter;

    /**
     * @param name display name for the property
     * @param getter supplies the current value
     * @param setter updates the current value
     * @param displayable whether to show the value on the canvas
     */
    public FloatProperty(String name, Supplier<Float> getter, Consumer<Float> setter, boolean displayable) {
        super(name, ComponentPropertyType.FLOAT, displayable, true);
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public Object getEditorValue() {
        return getter.get();
    }

    /**
     * @throws IllegalArgumentException when the provided value is not numeric
     */
    @Override
    public void setValueFromEditor(Object value) {
        if (value instanceof Number) {
            setter.accept(((Number) value).floatValue());
            return;
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            if (text.isEmpty()) {
                setter.accept(0f);
                return;
            }
            try {
                setter.accept(Float.parseFloat(text));
            } catch (NumberFormatException ignored) {
                throw new IllegalArgumentException("Invalid number for " + getName());
            }
        }
    }

    @Override
    public String getDisplayValue() {
        return VALUE_FORMAT.format(getter.get());
    }
}
