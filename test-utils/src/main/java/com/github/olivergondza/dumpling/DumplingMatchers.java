package com.github.olivergondza.dumpling;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.util.Objects;

public class DumplingMatchers {

    public static TypeSafeMatcher<StackTraceElement> frameOf(String cls, String method, String source) {
        return new TypeSafeMatcher<StackTraceElement>() {
            @Override
            protected boolean matchesSafely(StackTraceElement item) {
                if (!Objects.equals(cls, item.getClassName())) return false;
                if (!Objects.equals(method, item.getMethodName())) return false;
                if (source != null && !Objects.equals(source, item.getFileName())) return false;

                return true;
            }

            @Override
            public void describeTo(Description description) {
                StackTraceElement expected = new StackTraceElement(cls, method, source, -1);
                description.appendText("Stack frame of ").appendValue(expected);
            }
        };
    }
}
