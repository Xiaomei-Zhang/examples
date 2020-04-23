package calculation;

import lombok.Data;
import meta.ItemDef;

@Data
public class ItemValue {
	ItemDef def;
	Object val;
}
