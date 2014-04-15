package com.bluesunrise.jetspeed;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Created by rwatler on 4/11/14.
 */
public class JetspeedScriptLoggingAppender extends AppenderSkeleton {

    @Override
    protected void append(LoggingEvent loggingEvent) {
        String consoleMessage = loggingEvent.getLevel()+": "+loggingEvent.getMessage();
        try {
            JetspeedScript.CONSOLE.println(consoleMessage);
            JetspeedScript.CONSOLE.flush();
        } catch (Exception e) {
            System.out.println(consoleMessage);
        }
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    @Override
    public void close() {
    }
}
