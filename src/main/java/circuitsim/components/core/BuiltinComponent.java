package circuitsim.components.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link CircuitComponent} as a built-in component that can be discovered and registered
 * automatically for the palette and for state deserialization.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BuiltinComponent {
    /**
     * @return palette group name
     */
    String group();

    /**
     * @return palette entry label (defaults to the component type name)
     */
    String paletteName() default "";

    /**
     * @return serialized type id (defaults to the class simple name)
     */
    String type() default "";

    /**
     * @return additional serialized type ids accepted for deserialization
     */
    String[] aliases() default {};

    /**
     * @return true if the component should show in the palette UI
     */
    boolean paletteVisible() default true;

    /**
     * @return ordering for group insertion (lower first)
     */
    int groupOrder() default 0;

    /**
     * @return ordering within a group (lower first)
     */
    int paletteOrder() default 0;
}

