package com.famoco.kyctelcomrtlib.biometrics;

/**
 * Created by othomas on 30/07/2017.
 */

public class SecurityOption
{
    private boolean	activated	= false;
    private String title		= "";

    public SecurityOption(boolean activated, String title)
    {
        this.activated = activated;
        this.title = title;
    }

    public boolean isActivated() {
        return activated;
    }

    public String getTitle() {
        return title;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String toString(String no, String yes)
    {
        String act = no;
        if (activated)
            act = yes;
        return act + "\t" + title;
    }
}
