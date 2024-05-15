package aiefu.eso.data.client;

public class BackgroundColorData {
    protected String hexColor;
    protected float alpha;

    public BackgroundColorData(String hexColor, float alpha) {
        this.hexColor = hexColor;
        this.alpha = alpha;
    }

    public String getHexColor() {
        return hexColor;
    }

    public float getAlpha() {
        return alpha;
    }

    public static BackgroundColorData getDefault(){
        return new BackgroundColorData("#CFC9BA", 1.0F);
    }
}
