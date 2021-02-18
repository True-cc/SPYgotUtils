package uwu.smsgamer.spygotutils.managers;

import org.python.core.*;
import org.python.util.PythonInterpreter;
import uwu.smsgamer.spygotutils.SPYgotUtils;
import uwu.smsgamer.spygotutils.config.*;
import uwu.smsgamer.spygotutils.utils.python.*;
import uwu.smsgamer.spygotutils.utils.python.spigot.PycketListener;

import java.io.File;
import java.util.*;

public class PythonManager {
    public static PyFunction[] defaultFuns;
    public static PyObject packetListener;
    private static File[] files;
    private static final List<PyScript> scripts = new ArrayList<>();

    public static void init() {
        PythonInterpreter interpreter = new PythonInterpreter();
        if (SPYgotUtils.getInstance().onSpigot) {
            interpreter.exec("def register_event(event_type, priority, function):\n" +
              "    from uwu.smsgamer.spygotutils.utils.python.spigot import PyListener\n" +
              "    PyListener.registerEvent(event_type, priority, function)\n" +
              "def Command(name, description=\"\", usage_msg=None, aliases=None):\n" +
              "    if usage_msg is None:\n" +
              "        usage_msg = \"/\" + name\n" +
              "    if aliases is None:\n" +
              "        aliases = []\n" +
              "    from uwu.smsgamer.spygotutils.utils.python.spigot import PyCommand\n" +
              "    return PyCommand(name, description, usage_msg, aliases)\n");

            packetListener = Py.java2py(PycketListener.getInstance());
        } else {
            interpreter.exec("def register_event(event_type, priority, function):\n" +
              "    from uwu.smsgamer.spygotutils.utils.python.bungee import BPyListener\n" +
              "    BPyListener.registerEvent(event_type, priority, function)\n" +
              "#Pretend I'm an object\n" +
              "def Command(name, aliases=None):\n" +
              "    if aliases is None:\n" +
              "        aliases = []\n" +
              "    from uwu.smsgamer.spygotutils.utils.python.bungee import BPyCommand\n" +
              "    return BPyCommand(name, aliases)\n");
        }
        interpreter.exec("def get_data_folder():\n" +
          "    from uwu.smsgamer.spygotutils import SPYgotUtils\n" +
          "    return SPYgotUtils.getInstance().getDataFolder()\n"+
          "def on_spigot():\n" +
          "    from uwu.smsgamer.spygotutils import SPYgotUtils\n" +
          "    return SPYgotUtils.getInstance().onSpigot\n");
        defaultFuns = new PyFunction[]{(PyFunction) interpreter.get("register_event"),
          (PyFunction) interpreter.get("Command"),
          (PyFunction) interpreter.get("get_data_folder"),
          (PyFunction) interpreter.get("on_spigot")};
        interpreter.exec("from sys import path\n" +
          "path.append(\"" + SPYgotUtils.getLoader().getDataFolder() + File.separator + "scripts\")");
    }

    public static void loadScripts() {
        File dir = new File(SPYgotUtils.getLoader().getDataFolder(), "scripts");
        if (!dir.exists()) return;
        List<File> exclude = new ArrayList<>(files == null ? Collections.emptyList() : Arrays.asList(files));
        for (String fileName : loadScripts.getValue()) {
            File file = getFile(fileName);
            if (!file.exists() || exclude.contains(file)) continue;
            newScript(file);
            exclude.add(file);
        }
        files = exclude.toArray(new File[0]);
    }

    public static void newScript(File file) {
        if (!file.exists()) return;
        PyScript script = new PyScript(file).setFuns(defaultFuns);
        if (packetListener != null) script.set("packet_listener", packetListener);
        try {
            script.execFile();
            scripts.add(script);
            script.getGoodFuns();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ConfVal<List<String>> loadScripts = new ConfVal<>("load-scripts", "py-settings", Collections.emptyList());

    public static File getFile(String scriptName) {
        return new File(SPYgotUtils.getLoader().getDataFolder(), "scripts" + File.separator + scriptName);
    }

    public static void onEnable() {
        for (PyScript script : scripts) script.execAll(script.enableFuns);
    }

    public static void onReload() {
        for (PyScript script : scripts) script.execAll(script.reloadFuns);
    }

    public static void onDisable() {
        for (PyScript script : scripts) script.execAll(script.disableFuns);
    }
}
