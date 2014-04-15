package com.bluesunrise.jetspeed;

import jline.TerminalFactory;
import jline.console.ConsoleReader;
import jline.console.completer.StringsCompleter;
import org.apache.commons.io.IOUtils;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
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
                ScriptEngineManager factory = new ScriptEngineManager();
                ScriptEngine engine = factory.getEngineByName("JavaScript");
                ScriptContext context = new SimpleScriptContext();
                Bindings scope = context.getBindings(ScriptContext.ENGINE_SCOPE);

                jetspeedContext = new JetspeedContext(springFilter, properties);
                jetspeedContext.start();
                Map<String,Object> jetspeedComponents = jetspeedContext.getComponents();
                scope.putAll(jetspeedComponents);

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
                                engine.eval(bufferedLine, context);
                            } catch (ScriptException se) {
                                String message = se.getMessage();
                                if (message.contains(": ")) {
                                    message = message.substring(message.indexOf(": ")+1).trim();
                                }
                                if (message.contains(" (<Unknown source>")) {
                                    message = message.substring(0, message.indexOf(" (<Unknown source>")).trim();
                                }
                                CONSOLE.println(message);
                            }
                            bufferedLine = "";
                            CONSOLE.setPrompt("jetspeed> ");
                        }
                    }
                }
                CONSOLE.shutdown();
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
            e.printStackTrace();
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
}
