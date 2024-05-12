package aiefu.eso.data.client;

import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FastColor;

public class ColorDataHolder{
    //Compiled background colors
    protected int backgroundColor;
    protected float backgroundAlpha;
    //Compiled slider colors
    protected int sliderOuterColor;
    protected int sliderInnerColor;
    protected float sliderAlpha;
    //Compiled text colors
    protected int textActiveColor;
    protected int textInactiveColor;
    protected boolean dropShadow;
    protected int searchBarHintColor;
    protected boolean searchBarHintDropShadow;

    public ColorDataHolder(BackgroundColorData bcd, SliderColorData scd, TextSettings tx){
        this.backgroundColor = getPackedColorFromHex(checkHEXColor(bcd.hexColor), bcd.alpha);
        this.backgroundAlpha = bcd.alpha;

        this.sliderOuterColor = getPackedColorFromHex(checkHEXColor(scd.outerColor), scd.alpha);
        this.sliderInnerColor = getPackedColorFromHex(checkHEXColor(scd.innerColor), scd.alpha);
        this.sliderAlpha = scd.alpha;

        this.textActiveColor = TextColor.parseColor(tx.activeColor).result().get().getValue();
        this.textInactiveColor = TextColor.parseColor(tx.inactiveColor).result().get().getValue();
        this.dropShadow = tx.dropShadow;

        this.searchBarHintColor = TextColor.parseColor(tx.searchBarHintColor).result().get().getValue();
        this.searchBarHintDropShadow = tx.isSearchBarDropShadow();

    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public float getBackgroundAlpha() {
        return backgroundAlpha;
    }

    public int getSliderOuterColor() {
        return sliderOuterColor;
    }

    public int getSliderInnerColor() {
        return sliderInnerColor;
    }

    public float getSliderAlpha() {
        return sliderAlpha;
    }

    public int getTextActiveColor() {
        return textActiveColor;
    }

    public int getTextInactiveColor() {
        return textInactiveColor;
    }

    public boolean isDropShadow() {
        return dropShadow;
    }

    public int getSearchBarHintColor() {
        return searchBarHintColor;
    }

    public boolean isSearchBarHintDropShadow() {
        return searchBarHintDropShadow;
    }

    public static int getPackedColorFromHex(String hexCode, float alpha){
        int resultRed = Integer.valueOf(hexCode.substring(0, 2), 16);
        int resultGreen = Integer.valueOf(hexCode.substring(2, 4), 16);
        int resultBlue = Integer.valueOf(hexCode.substring(4, 6), 16);
        return FastColor.ARGB32.color((int) (alpha * 255), resultRed, resultGreen, resultBlue);
    }

    public static String checkHEXColor(String s){
        return s.startsWith("#") ? s.substring(1) : s;
    }
}
