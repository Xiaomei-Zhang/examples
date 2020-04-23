package meta;

import lombok.Data;

@Data
public class ItemDef {
	String name;
	String title;
	String expression;
	int order;
	String parameters;
	String calculate;

}
