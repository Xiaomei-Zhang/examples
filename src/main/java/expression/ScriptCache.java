package expression;


import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import groovy.lang.Script;

public class ScriptCache {
	private final Map<String, Script> scriptsByExpression = new ConcurrentHashMap<String, Script>(GroovyExpressionEvaluator.CACHE_SIZE);
	private final Queue<String> scripts = new ConcurrentLinkedQueue<String>();
	
	public ScriptCache() {
		;
	}
	
	public Script getScript (String exp) {
		return scriptsByExpression.get(exp);
	}
	
	public synchronized void addToCache(String exp, Script script) {
		if (this.scripts.size() >= GroovyExpressionEvaluator.CACHE_SIZE) {
			// rolling out the oldest consumerAccount
			rollOutCache();
		}
		// rolling in the most recent one
		rollInToCache(exp, script);

	}

	private void rollOutCache() {
		String exp = scripts.remove();
		scriptsByExpression.remove(exp);
	}

	private void rollInToCache(String exp, Script script) {
		scripts.add(exp);
		scriptsByExpression.put(exp, script);
	}

	
	
}