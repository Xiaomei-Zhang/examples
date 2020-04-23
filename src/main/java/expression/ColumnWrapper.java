package expression;

import org.codehaus.groovy.runtime.InvokerHelper;

import groovy.lang.GroovyObjectSupport;
import tech.tablesaw.columns.Column;

public class ColumnWrapper extends GroovyObjectSupport {
	private Column col;
	public ColumnWrapper(Column col) {
		super();
		this.col = col;
	}
	
	@Override
	public Object invokeMethod(String name, Object args) {
		Object ret = InvokerHelper.getMetaClass(this.col.getClass()).invokeMethod(this.col, name, args);

		if (ret != null) {
			return ret;
		} else {
			return ret;
		}
	}

	public Object getWrappedVal() {
		return col;
	}

}
