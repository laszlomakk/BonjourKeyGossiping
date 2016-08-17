package uk.ac.cam.cl.lm649.bonjourtesting.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation used to mark that a method is used via reflection.
 * The IDE can then be told not to mark said method as 'unused'.
 */
@Target(ElementType.METHOD)
public @interface UsedViaReflection {
}
