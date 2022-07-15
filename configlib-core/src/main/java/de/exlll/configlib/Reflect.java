package de.exlll.configlib;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class Reflect {
    private static final Map<Class<?>, Object> DEFAULT_VALUES = initDefaultValues();

    private Reflect() {}

    private static Map<Class<?>, Object> initDefaultValues() {
        return Stream.of(
                        boolean.class,
                        char.class,
                        byte.class,
                        short.class,
                        int.class,
                        long.class,
                        float.class,
                        double.class
                )
                .collect(Collectors.toMap(
                        type -> type,
                        type -> Array.get(Array.newInstance(type, 1), 0)
                ));
    }

    static <T> T getDefaultValue(Class<T> clazz) {
        @SuppressWarnings("unchecked")
        T defaultValue = (T) DEFAULT_VALUES.get(clazz);
        return defaultValue;
    }

    static <T> T newInstance(Class<T> cls) {
        try {
            Constructor<T> constructor = cls.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            String msg = "Class " + cls.getSimpleName() + " doesn't have a " +
                         "no-args constructor.";
            throw new RuntimeException(msg, e);
        } catch (IllegalAccessException e) {
            /* This exception should not be thrown because
             * we set the constructor to be accessible. */
            String msg = "No-args constructor of class " + cls.getSimpleName() +
                         " not accessible.";
            throw new RuntimeException(msg, e);
        } catch (InstantiationException e) {
            String msg = "Class " + cls.getSimpleName() + " is not instantiable.";
            throw new RuntimeException(msg, e);
        } catch (InvocationTargetException e) {
            String msg = "Constructor of class " + cls.getSimpleName() + " threw an exception.";
            throw new RuntimeException(msg, e);
        }
    }

    static <R extends Record> R newRecord(Class<R> recordType, Object... constructorArguments) {
        try {
            Constructor<R> constructor = getCanonicalConstructor(recordType);
            constructor.setAccessible(true);
            return constructor.newInstance(constructorArguments);
        } catch (NoSuchMethodException e) {
            // cannot happen because we select the constructor based on the component types
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            // cannot happen because we set the constructor to be accessible.
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            // cannot happen because records are instantiable
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            String msg = "The canonical constructor of record type '" +
                         recordType.getSimpleName() + "' threw an exception.";
            throw new RuntimeException(msg, e);
        }
    }

    static <R extends Record> Constructor<R> getCanonicalConstructor(Class<R> recordType)
            throws NoSuchMethodException {
        Class<?>[] parameterTypes = Arrays.stream(recordType.getRecordComponents())
                .map(RecordComponent::getType)
                .toArray(Class<?>[]::new);
        return recordType.getDeclaredConstructor(parameterTypes);
    }

    static <T> T[] newArray(Class<T> componentType, int length) {
        // The following cast won't fail because we just created an array of that type
        @SuppressWarnings("unchecked")
        T[] array = (T[]) Array.newInstance(componentType, length);
        return array;
    }

    static Object getValue(Field field, Object instance) {
        try {
            field.setAccessible(true);
            return field.get(instance);
        } catch (IllegalAccessException e) {
            /* This exception should not be thrown because
             * we set the field to be accessible. */
            String msg = "Illegal access of field '" + field + "' " +
                         "on object " + instance + ".";
            throw new RuntimeException(msg, e);
        }
    }

    static Object getValue(RecordComponent component, Object recordInstance) {
        final Method accessor = component.getAccessor();
        try {
            accessor.setAccessible(true);
            return accessor.invoke(recordInstance);
        } catch (IllegalAccessException e) {
            /* Should not be thrown because we set the method to be accessible. */
            String msg = "Illegal access of method '%s' on record '%s'."
                    .formatted(accessor, recordInstance);
            throw new RuntimeException(msg, e);
        } catch (InvocationTargetException e) {
            String msg = "Invocation of method '%s' on record '%s' failed."
                    .formatted(accessor, recordInstance);
            throw new RuntimeException(msg, e);
        }
    }

    static void setValue(Field field, Object instance, Object value) {
        try {
            field.setAccessible(true);
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            /* This exception should not be thrown because
             * we set the field to be accessible. */
            String msg = "Illegal access of field '" + field + "' " +
                         "on object " + instance + ".";
            throw new RuntimeException(msg, e);
        }
    }

    static boolean isIntegerType(Class<?> cls) {
        return (cls == byte.class) || (cls == Byte.class) ||
               (cls == short.class) || (cls == Short.class) ||
               (cls == int.class) || (cls == Integer.class) ||
               (cls == long.class) || (cls == Long.class);
    }

    static boolean isFloatingPointType(Class<?> cls) {
        return (cls == float.class) || (cls == Float.class) ||
               (cls == double.class) || (cls == Double.class);
    }

    static boolean isEnumType(Class<?> cls) {
        return cls.isEnum();
    }

    static boolean isArrayType(Class<?> cls) {
        return cls.isArray();
    }

    static boolean isListType(Class<?> cls) {
        return List.class.isAssignableFrom(cls);
    }

    static boolean isSetType(Class<?> cls) {
        return Set.class.isAssignableFrom(cls);
    }

    static boolean isMapType(Class<?> cls) {
        return Map.class.isAssignableFrom(cls);
    }

    static boolean isConfiguration(Class<?> cls) {
        return cls.getAnnotation(Configuration.class) != null;
    }

    static boolean isIgnored(Field field) {
        return field.getAnnotation(Ignore.class) != null;
    }
}