package ru.shtrm.iamclever.db.tables;

public class Languages extends BaseTable {

	private String name;
    private String icon;

	public Languages() {
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
    public String getIcon() {
        return icon;
    }
    public void setIcon(String icon) {
        this.icon = icon;
    }

}
