package expression;

import java.util.Map;

import javax.el.PropertyNotFoundException;

import groovy.lang.GroovyObjectSupport;
import meta.DataItemDef;
import tech.tablesaw.api.Row;

public class RowWrapper  extends GroovyObjectSupport {
	private Row row;
	private Map<String, DataItemDef> dataItemDefs;
	
	public RowWrapper(Row row, Map<String, DataItemDef> dataItemDefs) {
		super();
		this.row = row;
		this.dataItemDefs = dataItemDefs;
	}
	
	@Override
	public Object getProperty(String property) {
		DataItemDef dataItemDef = dataItemDefs.get(property);
		if(dataItemDef == null) {
			throw new PropertyNotFoundException(property + "is not found");
		}
		
		if (row.columnNames().contains(dataItemDef.getTitle()))
			return row.getObject(dataItemDef.getTitle());
		else
			throw new PropertyNotFoundException(property + "is not found");
	}

}
