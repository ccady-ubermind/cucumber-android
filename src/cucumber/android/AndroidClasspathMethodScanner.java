package cucumber.android;

import android.content.Context;
import cucumber.annotation.After;
import cucumber.annotation.Before;
import cucumber.annotation.Order;
import cucumber.runtime.java.StepDefAnnotation;
import ext.android.test.ClassPathPackageInfo;
import ext.android.test.ClassPathPackageInfoSource;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class AndroidClasspathMethodScanner {
    private AndroidBackend             mBackend;
    private ClassPathPackageInfoSource mSource;
    private ClassPathPackageInfo       mPackageInfo;

    public AndroidClasspathMethodScanner(AndroidBackend backend, Context context) {
        mBackend = backend;

        mSource = new ClassPathPackageInfoSource();
        mSource.setClassLoader(context.getClassLoader());

        mPackageInfo = mSource.getPackageInfo("cucumber.annotation");
    }

    public void scan(String packagePrefix) {
        List<Class<? extends Annotation>> cucumberAnnotations = findCucumberAnnotationClasses();
        for (Class<?> clazz : mSource.getPackageInfo(packagePrefix).getTopLevelClassesRecursive()) {
            try {
                if (Modifier.isPublic(clazz.getModifiers()) && !Modifier.isAbstract(clazz.getModifiers())) {
                    // TODO: How do we know what other dependencies to add?
                }
                for (Method method : clazz.getMethods()) {
                    if (Modifier.isPublic(method.getModifiers())) {
                        scan(method, cucumberAnnotations);
                    }
                }
            } catch (NoClassDefFoundError ignore) {
            } catch (SecurityException ignore) {
            }
        }
    }

    private List<Class<? extends Annotation>> findCucumberAnnotationClasses() {
        List<Class<? extends Annotation>> result = new ArrayList<Class<? extends Annotation>>();

        for (Class klass : mPackageInfo.getTopLevelClassesRecursive()) {
            if (klass.isAnnotation()) {
                result.add(klass);
            }
        }

        return result;
    }

    private void scan(Method method, List<Class<? extends Annotation>> cucumberAnnotationClasses) {
        for (Class<? extends Annotation> cucumberAnnotationClass : cucumberAnnotationClasses) {
            Annotation annotation = method.getAnnotation(cucumberAnnotationClass);
            if (annotation != null && !annotation.annotationType().equals(Order.class)) {
                if (isHookAnnotation(annotation)) {
                    mBackend.addHook(annotation, method);
                } else if(isStepdefAnnotation(annotation)) {
                    mBackend.addStepDefinition(annotation, method);
                }
            }
        }
    }

    private boolean isHookAnnotation(Annotation annotation) {
        Class<? extends Annotation> annotationClass = annotation.annotationType();
        return annotationClass.equals(Before.class) || annotationClass.equals(After.class);
    }

    private boolean isStepdefAnnotation(Annotation annotation) {
        Class<? extends Annotation> annotationClass = annotation.annotationType();
        return annotationClass.getAnnotation(StepDefAnnotation.class) != null;
    }
}
