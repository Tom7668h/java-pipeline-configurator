package de.tom.configurator.model;

/**
 * Domain entity representing an article/part master data record.
 */
public class Article {

    private String sku;
    private String name;
    private int standardWeightG;
    private String category;
    private boolean active;

    public Article() {
        this.active = true;
    }

    public Article(String sku, String name, int standardWeightG, String category, boolean active) {
        this.sku = sku;
        this.name = name;
        this.standardWeightG = standardWeightG;
        this.category = category;
        this.active = active;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStandardWeightG() {
        return standardWeightG;
    }

    public void setStandardWeightG(int standardWeightG) {
        this.standardWeightG = standardWeightG;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "Article{" +
                "sku='" + sku + '\'' +
                ", name='" + name + '\'' +
                ", standardWeightG=" + standardWeightG +
                ", category='" + category + '\'' +
                ", active=" + active +
                '}';
    }
}
