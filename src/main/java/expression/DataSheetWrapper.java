package expression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.el.PropertyNotFoundException;

import org.codehaus.groovy.runtime.InvokerHelper;

import com.fasterxml.jackson.databind.ObjectMapper;

import groovy.lang.GroovyObjectSupport;
import meta.DataItemDef;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

public class DataSheetWrapper extends GroovyObjectSupport {
	protected Table table;
	protected Map<String, DataItemDef> defs;
	static ObjectMapper mapper = new ObjectMapper();

	public DataSheetWrapper(Table table, Map<String, DataItemDef> defs) {
		super();
		this.table = table;
		this.defs = defs;
		setMetaClass(InvokerHelper.getMetaClass(this.getClass()));

	}

	@SuppressWarnings("unchecked")
	public static Object wrap(Object val, Map<String, DataItemDef> defs) {

		if (val instanceof Column) {
			return new ColumnWrapper((Column) val);
		} else if (val instanceof Table) {
			return new DataSheetWrapper((Table)val, defs);
		}
		else {
			return val;
		}
	}

	public static Object unwrap(Object val) {
		if (val instanceof ColumnWrapper) {
			return ((ColumnWrapper) val).getWrappedVal();
		} else if (val instanceof DataSheetWrapper) {
			return ((DataSheetWrapper) val).getWrappedVal();

		} else {
			return val;
		}
	}

	private Object getWrappedVal() {
		return table;
	}

	@Override
	public Object getProperty(String property) {
		Object ret = getProperty_internal(property);
		return ret;
	}

	protected Object getProperty_internal(String property) {
		DataItemDef def = this.defs.get(property);
		if (def == null) {
			throw new PropertyNotFoundException(property + "is not found for " + table.name());
		}

		Column col = table.column(def.getTitle());
		if (col.size() == 1) {
			List list = col.asList();
			return list.get(0);
		} else
			return col;
	}

	@Override
	public Object invokeMethod(String name, Object args) {
		Object ret = InvokerHelper.getMetaClass(this.table.getClass()).invokeMethod(this.table, name, args);

		if (ret != null) {
			return ret;
		} else {
			return ret;
		}
	}

	@Override
	public void setProperty(String property, Object newValue) {
		throw new ExpressionEvaluationException("set property is not allowed");
	}

	Map<String,DataItemDef> getDef() {
		return defs;
	}
}
