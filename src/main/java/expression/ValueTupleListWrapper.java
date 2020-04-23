package expression;

import java.util.List;
import java.util.Map;

import groovy.lang.GroovyObjectSupport;
import meta.DataItemDef;

public class ValueTupleListWrapper extends GroovyObjectSupport {
	private List<ValueTuple> values;
	private Map<String,DataItemDef> dataItemDefs;
	
	public ValueTupleListWrapper(List<ValueTuple> valueList, Map<String, DataItemDef> dataItemDefs) {
		super();
		this.values = valueList;
		this.dataItemDefs = dataItemDefs;
	}

	public List<ValueTuple> getWrappedVal () {
		return values;
	}
	
	public Map<String,DataItemDef> getDataItemDefs() {
		return dataItemDefs;
	}
}
