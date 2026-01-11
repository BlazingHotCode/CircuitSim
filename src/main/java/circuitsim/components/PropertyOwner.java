package circuitsim.components;

import java.util.List;

public interface PropertyOwner {
    String getDisplayName();

    void setDisplayName(String displayName);

    boolean isTitleEditable();

    List<ComponentProperty> getProperties();
}
