package expression;
/*******************************************************************************
 * Copyright (c) 2017 有帮智能.
 *******************************************************************************/


import java.util.Deque;
import java.util.Map;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.springframework.stereotype.Service;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.MissingMethodException;
import groovy.lang.Script;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GroovyExpressionEvaluator implements ExpressionEvaluator {
    public static int CACHE_SIZE = 100;
    private static GroovyShell shell = GroovyExpressionEvaluator.initShell();
    private Binding context;
    private static ThreadLocal<Deque<String>> mdcExpressions = new ThreadLocal<Deque<String>>();
    private final static ScriptCache scriptCache = new ScriptCache();

    public GroovyExpressionEvaluator() {
        this.context = new Binding();
    }

    private static GroovyShell initShell() {
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.setOutput(null);
        cc.setSourceEncoding("UTF-8");
        // inject default imports
        setImportCustomizer(cc);
        return new GroovyShell(cc);
    }

    public static void setImportCustomizer(CompilerConfiguration cc) {
        ImportCustomizer ic = new ImportCustomizer();
        ic.addStaticStars("expression.Functions");
        cc.addCompilationCustomizers(ic);
    }

    @Override
    public void setEvaluationCtx(Map<String, Object> evaluationCtx) {
        for (String varName : evaluationCtx.keySet()) {
            context.setVariable(varName, evaluationCtx.get(varName));
        }
    }

    @Override
    public Object evaluate(String expression, String location) {
        log.trace("MDC adding new expression {}, old expressions  {}", location, mdcExpressions.get());
        try {
            log.trace("Begin Evaluating at location {}, expression {}", location, expression);
            Script script = getScript(expression, location);
            script.setBinding(context);
            Object result = script.run();
            log.debug("Successfully evaluated at location {}, result {}", expression, result);
            return result;
        } catch (RuntimeException e) {
        	if (e instanceof MissingMethodException) {
        		if (((MissingMethodException)e).getArguments().length == 1 && ((MissingMethodException)e).getArguments()[0].getClass().getName().equals("expression.NTEObject")) {
        			throw new NTEException (e);
        		}
        	}else if (e instanceof NTEException) {
        		throw e;
        	}
            String msg = String.format("Failed to evaluate expression at %s\n context=%s\n body=%s", location, shell.getContext().getVariables(), expression);
            log.error(msg, e);
            throw new ExpressionEvaluationException("Groovy 脚本无法执行", e);
        } finally {
        }
    }

    private Script getScript(String expression, String location) {
        Script script = scriptCache.getScript(expression);
        if (script == null) {
            script = shell.parse(expression, location);
            scriptCache.addToCache(expression, script);
        }
        return script;
    }

    @Override
    public Map<String, Object> getEvaluationCtx() {
        return context.getVariables();
    }

    @Override
    public Class parseGroovyClass(String body, String name) {
        return shell.getClassLoader().parseClass(body, name);
    }
}
