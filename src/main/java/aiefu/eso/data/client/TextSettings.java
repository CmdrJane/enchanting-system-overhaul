package aiefu.eso.data.client;

public class TextSettings {
    protected String activeColor;
    protected String inactiveColor;
    protected boolean dropShadow;

    protected String searchBarHintColor;
    protected boolean searchBarDropShadow;

    public TextSettings(String activeColor, String inactiveColor, boolean dropShadow, String searchBarHintColor, boolean searchBarDropShadow) {
        this.activeColor = activeColor;
        this.inactiveColor = inactiveColor;
        this.dropShadow = dropShadow;
        this.searchBarHintColor = searchBarHintColor;
        this.searchBarDropShadow = searchBarDropShadow;
    }

    public boolean isDropShadow() {
        return dropShadow;
    }

    public String getActiveColor() {
        return activeColor;
    }

    public String getInactiveColor() {
        return inactiveColor;
    }

    public String getSearchBarHintColor() {
        return searchBarHintColor;
    }

    public boolean isSearchBarDropShadow() {
        return searchBarDropShadow;
    }

    public static TextSettings getDefault(){
        return new TextSettings("#3F3F3F", "#9E9E9E", false, "#DDDDDD", true);
    }
}
