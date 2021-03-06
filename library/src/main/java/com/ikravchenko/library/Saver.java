package com.ikravchenko.library;

import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

/**
 * Helper class for saving {@link android.app.Activity}s and {@link android.app.Fragment}s states by putting the marked with
 * {@link com.ikravchenko.library.SaveState} fields in the saved {@link Bundle}
 * The annotated field should be either {@link android.os.Parcelable} (preferred Android way) or {@link java.io.Serializable}.
 * throws {@link java.lang.RuntimeException} when trying to save final fields.
 *
 * @see com.ikravchenko.library.SaveState
 */
public class Saver {

    private static final String TAG = Saver.class.getSimpleName();
    private static final String SAVED_OBJECTS_MAPPING_KEY = "SAVED_OBJECTS_MAPPING";

    private ArrayList<String> fieldNames = new ArrayList<String>();

    /**
     * Saves {@link android.app.Activity} or {@link android.app.Fragment} state into the outState
     *
     * @param object   {@link android.app.Activity} or {@link android.app.Fragment} instance to save
     * @param outState usually created by system {@link Bundle}. Call it in:
     *                 {@link android.app.Activity#onSaveInstanceState(android.os.Bundle)} or
     *                 {@link android.app.Fragment#onSaveInstanceState(android.os.Bundle)}
     * @throws java.lang.RuntimeException in case of object or outState in {@code null} or
     *                                    if the field is final
     */
    public void save(Object object, Bundle outState) {
        if (object == null) {
            throw new RuntimeException("Object should not be null!");
        }
        if (outState == null) {
            throw new RuntimeException("outState bundle should not be empty!");
        }
        Log.i(TAG, "Size: " + fieldNames.size() + ", class: " + object.getClass().getSimpleName() + " " + TextUtils.join("; ", fieldNames));
        fieldNames.clear();
        Class<?> clazz = object.getClass();
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(SaveState.class)) {
                if (Modifier.isFinal(field.getModifiers())) {
                    throw new RuntimeException("final field should not be annotated to save state!");
                }
                field.setAccessible(true);
                fieldNames.add(field.getName());
                Object value;
                try {
                    value = field.get(object);
                    if (value instanceof Parcelable) {
                        outState.putParcelable(field.getName(), (Parcelable) value);
                    } else if (value instanceof Serializable) {
                        outState.putSerializable(field.getName(), (Serializable) value);
                    }
                } catch (IllegalAccessException e) {
                    Log.e(TAG, "Cannot access field: " + field + " and save it's value!");
                }
            }
        }
        if (!fieldNames.isEmpty()) {
            outState.putSerializable(SAVED_OBJECTS_MAPPING_KEY, fieldNames);
        }
    }

    /**
     * Restores {@link android.app.Activity} or {@link android.app.Fragment} state into the inState
     * If the inState is {@code null}, has no effect on the object
     *
     * @param object  {@link android.app.Activity} or {@link android.app.Fragment} instance to restore
     * @param inState usually generated by system {@link Bundle}. Call it in:
     *                {@link android.app.Activity#onCreate(android.os.Bundle)}, {@link android.app.Activity#onRestoreInstanceState(android.os.Bundle)} or
     *                {@link android.app.Fragment#onCreate(android.os.Bundle)}, {@link android.app.Fragment#onActivityCreated(android.os.Bundle)}
     * @throws java.lang.RuntimeException in case of object is {@code null}
     */
    @SuppressWarnings("unchecked")
    public void restore(Object object, Bundle inState) {
        if (object == null) {
            throw new RuntimeException("Object should not be null!");
        }
        if (inState == null) {
            return;
        }

        inState.setClassLoader(getClass().getClassLoader());

        if (!inState.containsKey(SAVED_OBJECTS_MAPPING_KEY)) {
            Log.i(TAG, "nothing was saved");
            return;
        }

        fieldNames = (ArrayList<String>) inState.getSerializable(SAVED_OBJECTS_MAPPING_KEY);

        Class<?> clazz = object.getClass();
        for (String fieldName : fieldNames) {
            try {
                Field declaredField = clazz.getDeclaredField(fieldName);
                declaredField.setAccessible(true);
                declaredField.set(object, inState.get(fieldName));
            } catch (NoSuchFieldException e) {
                Log.w(TAG, "cannot find field: " + fieldName);
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                Log.e(TAG, "Cannot access field: " + fieldName + " and set a value: " + inState.get(fieldName));
            }
        }

    }
}
