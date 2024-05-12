package aiefu.eso.data.client;

public class SliderColorData {
    protected String outerColor;
    protected String innerColor;
    protected float alpha;

    public SliderColorData(String outerColor, String innerColor, float alpha) {
        this.outerColor = outerColor;
        this.innerColor = innerColor;
        this.alpha = alpha;
    }

    public String getOuterColor() {
        return outerColor;
    }

    public String getInnerColor() {
        return innerColor;
    }

    public float getAlpha() {
        return alpha;
    }

    public static SliderColorData getDefault(){
        return new SliderColorData("#FAB9EF", "#B688AE", 1.0F);
    }
}
