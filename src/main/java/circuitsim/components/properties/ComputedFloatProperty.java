package circuitsim.components.properties;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Read-only float property derived from a supplier.
 */
public class ComputedFloatProperty extends AbstractComponentProperty {
    private static final DecimalFormat VALUE_FORMAT =
            new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));

    private final Supplier<Float> getter;

    /**
     * @param name display name for the property
     * @param getter supplies the computed value
     * @param displayable whether to show the value on the canvas
     */
    public ComputedFloatProperty(String name, Supplier<Float> getter, boolean displayable) {
        super(name, ComponentPropertyType.FLOAT, displayable, false);
        this.getter = getter;
    }

    @Override
    public Object getEditorValue() {
        return getter.get();
    }

    @Override
    public void setValueFromEditor(Object value) {
        // Read-only.
    }

    @Override
    public String getDisplayValue() {
        Float value = getter.get();
        return value == null ? "" : VALUE_FORMAT.format(value);
    }
}
