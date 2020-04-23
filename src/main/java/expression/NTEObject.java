package expression;

import java.util.ArrayList;
import java.util.List;

import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingMethodException;

public class NTEObject extends GroovyObjectSupport{
	private Object val;
	public NTEObject() {
		val = null;
	}
	@Override
	public Object getProperty(String property) {
		return new NTEObject();
	}

	@Override
	public Object invokeMethod(String name, Object args) {
		List arr = new ArrayList();
		arr.add(this);
		throw new MissingMethodException (name, this.getClass(), arr.toArray());
	}
}
