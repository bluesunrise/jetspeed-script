package com.bluesunrise.jetspeed;

import jline.TerminalFactory;
import jline.console.ConsoleReader;
import jline.console.completer.StringsCompleter;
import org.apache.commons.io.IOUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.WrappedException;
import org.ringojs.util.ScriptUtils;
import org.ringojs.wrappers.ScriptableList;
import org.ringojs.wrappers.ScriptableMap;
import org.ringojs.wrappers.ScriptableWrapper;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Created by rwatler on 4/11/14.
 */
public class JetspeedScript {

    public static ConsoleReader CONSOLE;
    static {
        try {
            CONSOLE = new ConsoleReader();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static JetspeedContext jetspeedContext;

    public static void main(String[] args) {

        bridgeJavaLogging();

        Thread shutdownHook = new Thread(new Runnable() {
            @Override
            public void run() {
                shutdown(false);
            }
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        boolean exit = false;
        String springFilter = "portal.dbPageManager";
        if (args.length > 0) {
            springFilter = args[1];
        }

        try {
            Properties properties = new Properties();
            File propertiesFile = new File("jetspeed-script.properties");
            if (!propertiesFile.isFile()) {
                System.out.println("jetspeed-script.properties missing, template file created.");
                System.out.println("Configure properties in file before restarting.");
                PrintWriter propertiesFileWriter = new PrintWriter(new FileWriter(propertiesFile));
                propertiesFileWriter.println("#");
                propertiesFileWriter.println("# jetspeed-script configuration");
                propertiesFileWriter.println("#");
                propertiesFileWriter.println();
                propertiesFileWriter.println("# Database");
                propertiesFileWriter.println(JetspeedContext.DATABASE_DRIVER_PROP_NAME+"=");
                propertiesFileWriter.println(JetspeedContext.DATABASE_URL_PROP_NAME+"=");
                propertiesFileWriter.println(JetspeedContext.DATABASE_USER_PROP_NAME+"=");
                propertiesFileWriter.println(JetspeedContext.DATABASE_PASSWORD_PROP_NAME+"=");
                propertiesFileWriter.close();
                exit = true;
            } else {
                FileReader propertiesFileReader = new FileReader(propertiesFile);
                properties.load(propertiesFileReader);
                propertiesFileReader.close();
            }

            if (!exit) {
                Context scriptContext = Context.enter();
                try {
                    scriptContext.setLanguageVersion(Context.VERSION_1_8);
                    Scriptable scriptScope = new Global(scriptContext);
                    scriptContext.evaluateString(scriptScope, "importClass(Packages."+ScriptUtils.class.getName()+")", "main", 1, null);
                    scriptContext.evaluateString(scriptScope, "importClass(Packages."+ScriptableList.class.getName()+")", "main", 1, null);
                    scriptContext.evaluateString(scriptScope, "importClass(Packages."+ScriptableMap.class.getName()+")", "main", 1, null);
                    scriptContext.evaluateString(scriptScope, "importClass(Packages."+ScriptableWrapper.class.getName()+")", "main", 1, null);

                    jetspeedContext = new JetspeedContext(springFilter, properties);
                    jetspeedContext.start();
                    Map<String,Object> jetspeedComponents = jetspeedContext.getComponents();
                    for (Map.Entry<String,Object> jetspeedComponentEntry : jetspeedComponents.entrySet()) {
                        ScriptableObject.putProperty(scriptScope, jetspeedComponentEntry.getKey(), Context.javaToJS(jetspeedComponentEntry.getValue(), scriptScope));
                    }

                    CONSOLE.addCompleter(new JavascriptSymbolCompleter(jetspeedComponents.keySet()));
                    CONSOLE.addCompleter(new JavascriptSymbolCompleter(IOUtils.readLines(JetspeedScript.class.getResourceAsStream("/javascript-keywords.txt"))));
                    CONSOLE.addCompleter(new StringsCompleter(Arrays.asList(new String[]{"exit", "quit"})));
                    CONSOLE.setPrompt("jetspeed> ");
                    String bufferedLine = "";
                    String line = "";
                    while (!exit && (line = CONSOLE.readLine()) != null) {
                        if (line.endsWith("\\")) {
                            bufferedLine = bufferedLine+line.substring(0, line.length()-1);
                            CONSOLE.setPrompt("> ");
                        } else {
                            bufferedLine = (bufferedLine+line).trim();
                            if (bufferedLine.equals("exit") || bufferedLine.equals("quit")) {
                                exit = true;
                            } else {
                                try {
                                    Object result = scriptContext.evaluateString(scriptScope, bufferedLine, "console", 1, null);
                                    if ((result != null) && !(result instanceof Undefined)) {
                                        CONSOLE.println(scriptContext.toString(result));
                                    }
                                } catch (WrappedException we) {
                                    CONSOLE.println(we.getWrappedException().toString());
                                    we.getWrappedException().printStackTrace();
                                } catch (RhinoException re) {
                                    String message = re.getMessage();
                                    if (message.endsWith(" (console#1)")) {
                                        message=message.substring(0, message.length()-12);
                                    }
                                    CONSOLE.println(message);
                                }
                                bufferedLine = "";
                                CONSOLE.setPrompt("jetspeed> ");
                            }
                        }
                    }
                    CONSOLE.shutdown();
                } finally {
                    Context.exit();
                }
            }
        } catch (Exception e) {
            System.out.println();
            e.printStackTrace();
        } finally {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
            try {
                TerminalFactory.get().restore();
            } catch (Exception e) {
            }
            shutdown(exit);
        }
    }

    public static void shutdown(boolean exit) {
        if (!exit) {
            System.out.println();
        }
        try {
            if (jetspeedContext != null) {
                jetspeedContext.stop();
            }
        } catch (Exception e) {
        }
    }

    private static void bridgeJavaLogging() {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        for (int i = 0; i < handlers.length; i++) {
            rootLogger.removeHandler(handlers[i]);
        }
        SLF4JBridgeHandler.install();
    }

    public static class Global extends ImporterTopLevel {

        public Global(Context scriptContext) {
            super(scriptContext);
            this.defineFunctionProperties(new String[]{"print", "println"}, getClass(), ScriptableObject.DONTENUM);
        }

        public static void print(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
            boolean first = true;
            for (Object arg : args) {
                String argString = (first ? "" : " ")+cx.toString(arg);
                try {
                    CONSOLE.print(argString);
                } catch (Exception e) {
                    System.out.print(argString);
                }
                first = false;
            }
        }

        public static void println(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
            print(cx, thisObj, args, funObj);
            try {
                CONSOLE.println();
            } catch (Exception e) {
                System.out.println();
            }
        }
    }
}
