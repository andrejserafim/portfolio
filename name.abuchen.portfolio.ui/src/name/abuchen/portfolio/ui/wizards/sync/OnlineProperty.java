package name.abuchen.portfolio.ui.wizards.sync;

import name.abuchen.portfolio.model.OnlineState;
import name.abuchen.portfolio.model.OnlineState.State;

public class OnlineProperty
{
    private String originalValue;
    private OnlineState.State originalState;

    private String suggestedValue;

    private boolean isModified;

    public OnlineProperty(String originalValue, OnlineState.State originalState)
    {
        this.originalValue = originalValue;
        this.originalState = originalState;
    }

    public String getOriginalValue()
    {
        return originalValue;
    }

    public String getSuggestedValue()
    {
        return suggestedValue;
    }

    public void setSuggestedValue(String suggestedValue)
    {
        this.suggestedValue = suggestedValue;

        // no online value found --> keep existing value
        if (suggestedValue == null || suggestedValue.isEmpty())
            isModified = false;

        // online value is identical --> nothing to do
        else if (suggestedValue.equals(originalValue))
            isModified = false; // NOSONAR

        // accept new value if no sync decision was made
        else if (originalState == OnlineState.State.BLANK)
            isModified = true;
    }

    public State getSuggestedState()
    {
        if (!isModified())
        {
            return suggestedValue != null && suggestedValue.equals(originalValue) ? OnlineState.State.SYNCED
                            : OnlineState.State.CUSTOM;
        }
        else
        {
            return OnlineState.State.SYNCED;
        }
    }

    public String getValue()
    {
        return isModified ? suggestedValue : originalValue;
    }

    public boolean isModified()
    {
        return isModified;
    }

    public void setModified(boolean isModified)
    {
        this.isModified = isModified;
    }

}
