package com.bluesunrise.jetspeed;

import jline.console.completer.Completer;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Created by rwatler on 4/14/14.
 */
public class JavascriptSymbolCompleter implements Completer {

    private SortedSet<String> strings = new TreeSet<String>(new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return o1.toLowerCase().compareTo(o2.toLowerCase());
        }
    });

    public JavascriptSymbolCompleter(Collection<String> strings) {
        this.strings.addAll(strings);
    }

    @Override
    public int complete(String buffer, int cursor, List<CharSequence> candidates) {
        String lastSymbol = null;
        int lastSymbolIndex = 0;
        if (buffer != null) {
            lastSymbolIndex = buffer.length()-1;
            while ((lastSymbolIndex >= 0) && Character.isJavaIdentifierPart(buffer.charAt(lastSymbolIndex))) {
                lastSymbolIndex--;
            }
            lastSymbolIndex++;
            lastSymbol = buffer.substring(lastSymbolIndex);
        }
        if ((lastSymbol == null) || lastSymbol.isEmpty()) {
            candidates.addAll(strings);
        } else {
            for (String candidate : strings.tailSet(lastSymbol)) {
                if (candidate.startsWith(lastSymbol)) {
                    candidates.add(candidate);
                }
            }
        }
        return (candidates.isEmpty() ? -1 : lastSymbolIndex);
    }
}

