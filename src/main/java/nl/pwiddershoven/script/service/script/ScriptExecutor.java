package nl.pwiddershoven.script.service.script;

import java.util.*;

import javax.script.*;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import nl.pwiddershoven.script.service.ScriptConfiguration;
import nl.pwiddershoven.script.service.script.module.JsModule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ScriptExecutor {
    private static final String SCRIPT_WRAPPER = "(function() { %s; })()";

    private ScriptEngine jsEngine;
    private Map<String, JsModule> jsModules = new HashMap<>();

    public ScriptExecutor() {
        NashornScriptEngineFactory scriptEngineFactory = new NashornScriptEngineFactory();
        jsEngine = scriptEngineFactory.getScriptEngine(new NashornClassFilter());

        try {
            Bindings bindings = jsEngine.createBindings();
            jsEngine.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);

            bindings.put("__ctx", new JsContext());
            bindings.put("require", jsEngine.eval("function(moduleName) { return __ctx.require(moduleName); };"));
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    public Object execute(ScriptConfiguration scriptConfiguration) {
        try {
            ScriptContext ctx = new SimpleScriptContext();

            // create a fresh new scope for each script
            ctx.setBindings(jsEngine.createBindings(), ScriptContext.ENGINE_SCOPE);

            // inherit the default global scope, so require is globally available
            ctx.setBindings(jsEngine.getBindings(ScriptContext.GLOBAL_SCOPE), ScriptContext.GLOBAL_SCOPE);

            Object result = jsEngine.eval(String.format(SCRIPT_WRAPPER, scriptConfiguration.processingScript), ctx);

            if (result instanceof ScriptObjectMirror)
                result = MarshalingHelper.unwrap((ScriptObjectMirror) result);

            return result;
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    public class JsContext {
        public Object require(String moduleName) {
            JsModule module = jsModules.get(moduleName);
            if (module == null)
                return new RuntimeException("Module not found");

            return module;
        }
    }

    @Autowired
    public void setJsModules(Set<JsModule> jsModules) {
        for (JsModule module : jsModules) {
            this.jsModules.put(module.name(), module);
        }
    }
}
