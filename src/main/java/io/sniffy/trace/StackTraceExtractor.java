package io.sniffy.trace;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StackTraceExtractor {
    
    private StackTraceExtractor() {
    }

    public static List<StackTraceElement> getTraceForProxiedMethod(Method method) throws ClassNotFoundException {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        Class<?> baseClass = method.getDeclaringClass();
        // skip all elements until proxied call
        int startIndex = 0;
        StackTraceElement baseMethodTrace = null;
        for (int i = 0; i < stackTraceElements.length; i++) {
            StackTraceElement traceElement = stackTraceElements[i];
            String traceElementClassName = traceElement.getClassName();
            if (traceElementClassName.contains("Proxy")
                    && baseClass.isAssignableFrom(Class.forName(traceElementClassName))) {
                baseMethodTrace = createTraceElement(method, traceElement);
                startIndex = i + 1;
                break;
            }
        }
        if (startIndex == 0) {
            // no proxy, return entire collection
            return Arrays.asList(stackTraceElements);
        } else {
            List<StackTraceElement> result = new ArrayList<StackTraceElement>();
            result.add(baseMethodTrace);
            result.addAll(Arrays.asList(Arrays.copyOfRange(stackTraceElements, startIndex, stackTraceElements.length - 1)));
            return result;
        }
    }

    public static String printStackTrace(List<StackTraceElement> stackTraceElements) {
        if (stackTraceElements == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String lineSeparator = System.lineSeparator();
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            sb.append(stackTraceElement.toString()).append(lineSeparator);
        }
        if (sb.length() > 0) {
            return sb.substring(0, sb.length() - 1);
        } else {
            return "";
        }
    }

    private static StackTraceElement createTraceElement(Method method, StackTraceElement baseTraceElement) {
        return new StackTraceElement(
                method.getDeclaringClass().getCanonicalName(),
                method.getName(),
                baseTraceElement.getFileName(),
                baseTraceElement.getLineNumber());
    }

}
