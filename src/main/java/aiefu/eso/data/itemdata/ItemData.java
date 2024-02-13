package aiefu.eso.data.itemdata;

public class ItemData {
    public String id;
    public String tag;
    public ItemData[] itemArray;
    public int amount;
    public String remainderId;
    public int remainderAmount;
    public String remainderTag;

    public ItemData(String id, String tag, ItemData[] itemArray, int amount, String remainderId, int remainderAmount, String remainderTag) {
        this.id = id;
        this.tag = tag;
        this.itemArray = itemArray;
        this.amount = amount;
        this.remainderId = remainderId;
        this.remainderAmount = remainderAmount;
        this.remainderTag = remainderTag;
    }

    public ItemData(String id, int amount, String tag, String remainderId, int remainderAmount, String remainderTag) {
        this.id = id;
        this.tag = tag;
        this.amount = amount;
        this.remainderId = remainderId;
        this.remainderAmount = remainderAmount;
        this.remainderTag = remainderTag;
    }

    public ItemData(String id, int amount) {
        this.id = id;
        this.amount = amount;
    }
}
