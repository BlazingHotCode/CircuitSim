package circuitsim.components;

import java.util.List;

/**
 * Exposes a name and editable properties to the UI.
 */
public interface PropertyOwner {
    /**
     * @return the display label for the owner
     */
    String getDisplayName();

    /**
     * Updates the display label.
     *
     * @param displayName new label
     */
    void setDisplayName(String displayName);

    /**
     * @return true when the display label can be edited by users
     */
    boolean isTitleEditable();

    /**
     * @return properties shown in the properties panel
     */
    List<ComponentProperty> getProperties();
}
