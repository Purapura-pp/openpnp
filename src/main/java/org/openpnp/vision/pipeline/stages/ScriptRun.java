package org.openpnp.vision.pipeline.stages;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.openpnp.model.Configuration;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

import com.google.common.io.Files;

import bsh.engine.BshScriptEngineFactory;

@Stage(description="Run an arbitrary script file using the built in scripting engine. pipeline and stage are exposed as globals for use by the script. To return a pipeline result you can't use a return statement, but instead just let the object be the last thing the script evaluates.")
public class ScriptRun extends CvStage {
    @Attribute
    private File file = new File("");

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    @Attribute
    private String args = new String("");

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    /**
     * A pipeline is untrusted input: it is routinely pasted from the clipboard or imported from a
     * shared configuration, and it runs unattended once assigned to a feeder or vision settings.
     * The script is therefore confined to the scripts directory, which the user already controls
     * deliberately.
     * <p>
     * Containment is decided lexically first, so a path that never belonged here at all - a UNC
     * share, a mapped network drive, a parent traversal - is refused before anything touches the
     * file system. Only a path that already looks contained is then resolved for real, which
     * additionally catches a link inside the directory that leads back out of it.
     */
    protected File resolveScript() throws Exception {
        File scriptsDirectory = Configuration.get().getScripting().getScriptsDirectory();
        if (scriptsDirectory == null) {
            throw new Exception("ScriptRun requires a configured scripts directory.");
        }
        Path allowed = scriptsDirectory.getAbsoluteFile().toPath().normalize();
        Path requested = file.toPath();
        // A relative path is taken against the scripts directory rather than the working
        // directory, since that is the only reading of it that can ever be permitted.
        Path candidate =
                (requested.isAbsolute() ? requested : allowed.resolve(requested)).normalize();
        if (!candidate.startsWith(allowed)) {
            throw new Exception("ScriptRun only runs scripts inside " + allowed
                    + ". Move the script there and update this stage. Refusing to run " + candidate
                    + ".");
        }
        if (!candidate.toFile().exists()) {
            // Nothing to follow, and the caller skips a script that is not there. Reporting the
            // path as asked for keeps a stale stage behaving the way it always has.
            return candidate.toFile();
        }
        // Links have to be followed with toRealPath: File.getCanonicalFile normalises the spelling
        // of a path but does not resolve a link on Windows, which would leave the directory open
        // to being escaped by one.
        Path allowedReal = allowed.toFile().exists() ? allowed.toRealPath() : allowed;
        Path resolved = candidate.toRealPath();
        if (!resolved.startsWith(allowedReal)) {
            throw new Exception("ScriptRun only runs scripts inside " + allowed + ", but "
                    + candidate + " leads out of it, to " + resolved + ". Refusing to run it.");
        }
        return resolved.toFile();
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (file.getPath().isEmpty()) {
            return null;
        }

        File script = resolveScript();
        if (!script.exists()) {
            return null;
        }

        ScriptEngineManager manager = new ScriptEngineManager();
        // Hack to fix BSH on Windows. See https://github.com/openpnp/openpnp/issues/462
        manager.registerEngineExtension("bsh", new BshScriptEngineFactory());
        manager.registerEngineExtension("java", new BshScriptEngineFactory());
        ScriptEngine engine = manager.getEngineByExtension(Files.getFileExtension(script.getName()));
        
        if (engine == null) {
            throw new Exception("Unable to find scriping engine for " + script);
        }

        engine.put("pipeline", pipeline);
        engine.put("stage", this);
        engine.put("args",args);

        try (FileReader reader = new FileReader(script)) {
            Object result = engine.eval(reader);
            if (result instanceof Result) {
                return (Result) result;
            }
            return null;
        }
    }
}
