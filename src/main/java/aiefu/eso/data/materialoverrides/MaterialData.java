package aiefu.eso.data.materialoverrides;

public class MaterialData {
    protected int maxEnchantments;
    protected int maxCurses;
    protected int curseMultiplier;

    public MaterialData(int maxEnchantments, int maxCurses, int curseMultiplier) {
        this.maxEnchantments = maxEnchantments;
        this.maxCurses = maxCurses;
        this.curseMultiplier = curseMultiplier;
    }

    public int getMaxEnchantments() {
        return maxEnchantments;
    }

    public int getMaxCurses() {
        return maxCurses;
    }

    public int getCurseMultiplier() {
        return curseMultiplier;
    }
}
