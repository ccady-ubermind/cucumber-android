package cucumber.android;

import android.content.Context;
import ext.android.test.ClassPathPackageInfoSource;
import android.util.Log;
import cucumber.annotation.After;
import cucumber.annotation.Before;
import cucumber.annotation.Order;
import cucumber.runtime.Backend;
import cucumber.runtime.CucumberException;
import cucumber.runtime.World;
import cucumber.runtime.java.JavaHookDefinition;
import cucumber.runtime.java.JavaSnippetGenerator;
import cucumber.runtime.java.JavaStepDefinition;
import cucumber.runtime.java.ObjectFactory;
import cucumber.runtime.java.picocontainer.PicoFactory;
import gherkin.formatter.model.Step;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

public class AndroidBackend implements Backend {
    private static final String TAG = "AndroidBackend";

    private final Set<Class> stepDefinitionClasses = new HashSet<Class>();
    private final ObjectFactory objectFactory;

    private World   world;
    private Context context;
    private AndroidClasspathMethodScanner mScanner; 

    public AndroidBackend(Context context) {
        this(context, new PicoFactory());
    }

    public AndroidBackend(Context context, ObjectFactory objectFactory) {
        this.context       = context;
        this.objectFactory = objectFactory;
    }

    @Override
    public void buildWorld(List<String> gluePaths, World world) {
        this.world = world;

        // FIXME: Right now gluePaths contains both a list of packages AND apk paths.
        Log.d(TAG, "Building world! " + this + "  " + world.getStepDefinitions().size() + "    "  + gluePaths);
        ClassPathPackageInfoSource.setApkPaths(gluePaths.toArray(new String[gluePaths.size()]));
        
        if (mScanner == null) { // FIXME: Do the glue paths change?
            mScanner = new AndroidClasspathMethodScanner(this, this.context);
        }
        
        for (String gluePath : gluePaths) {
            mScanner.scan(gluePath);
        }

        objectFactory.createInstances();
    }

    @Override
    public void disposeWorld() {
        objectFactory.disposeInstances();
    }

    @Override
    public String getSnippet(Step step) {
        return new JavaSnippetGenerator(step).getSnippet();
    }

    void addStepDefinition(Annotation annotation, Method method) {
        try {
            Method regexpMethod = annotation.getClass().getMethod("value");
            String regexpString = (String) regexpMethod.invoke(annotation);
            if (regexpString != null) {
                Pattern pattern = Pattern.compile(regexpString);
                Class<?> clazz = method.getDeclaringClass();
                registerClassInObjectFactory(clazz);
                world.addStepDefinition(new JavaStepDefinition(method, pattern, objectFactory));
            }
        } catch (NoSuchMethodException e) {
            throw new CucumberException(e);
        } catch (InvocationTargetException e) {
            throw new CucumberException(e);
        } catch (IllegalAccessException e) {
            throw new CucumberException(e);
        }
    }

    void addHook(Annotation annotation, Method method) {
        Class<?> clazz = method.getDeclaringClass();
        registerClassInObjectFactory(clazz);

        Order order = method.getAnnotation(Order.class);
        int hookOrder = (order == null) ? Integer.MAX_VALUE : order.value();

        if (annotation.annotationType().equals(Before.class)) {
            String[] tagExpressions = ((Before) annotation).value();
            world.addBeforeHook(new JavaHookDefinition(method, tagExpressions, hookOrder, objectFactory));
        } else {
            String[] tagExpressions = ((After) annotation).value();
            world.addAfterHook(new JavaHookDefinition(method, tagExpressions, hookOrder, objectFactory));
        }
    }

    private void registerClassInObjectFactory(Class<?> clazz) {
        if (!stepDefinitionClasses.contains(clazz)) {
            objectFactory.addClass(clazz);
            stepDefinitionClasses.add(clazz);
            addConstructorDependencies(clazz);
        }
    }

    private void addConstructorDependencies(Class<?> clazz) {
        for (Constructor constructor : clazz.getConstructors()) {
            for (Class paramClazz : constructor.getParameterTypes()) {
                registerClassInObjectFactory(paramClazz);
            }
        }
    }
}