package cucumber.android;

import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.Bundle;
import android.test.AndroidTestRunner;
import android.test.InstrumentationTestRunner;
import android.util.Log;

import cucumber.runtime.Backend;
import cucumber.runtime.FeatureBuilder;
import cucumber.runtime.model.CucumberFeature;
import cucumber.runtime.model.CucumberScenario;
import cucumber.runtime.model.CucumberTagStatement;
import ext.android.test.ClassPathPackageInfo;
import ext.android.test.ClassPathPackageInfoSource;
import gherkin.formatter.PrettyFormatter;
import gherkin.formatter.model.Step;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class CucumberInstrumentationTestRunner extends InstrumentationTestRunner {
    private static final String TAG = "CucumberInstrumentationTestRunner";
    
    private static final String ARGUMENT_TEST_CLASS   = "class";
    private static final String ARGUMENT_TEST_PACKAGE = "package";
    
    private cucumber.runtime.Runtime    mRuntime;
    private AndroidTestRunner           mTestRunner;
    private ClassPathPackageInfo        mClassPathPackageInfo;
    private Map<Class<?>, CucumberFeature> mClassFeatures = new HashMap<Class<?>, CucumberFeature>();
    
    public static TestCase sCurrentTestCase;

    public CucumberInstrumentationTestRunner () {
        super();
        Log.d(TAG, "Constructor");
    }

    @Override
    public void onCreate (Bundle arguments)
    {
        super.onCreate(arguments);

        Log.d(TAG, "onCreate. package: " + arguments.getString(ARGUMENT_TEST_PACKAGE));
        Log.d(TAG, "onCreate. class:   " + arguments.getString(ARGUMENT_TEST_CLASS));
    }

    @Override
    public void onStart () {
        Log.d(TAG, "onStart");

        ArrayList<Backend> backends = new ArrayList<Backend>();
        backends.add(new AndroidBackend(getContext()));

        ArrayList<String> gluePaths = new ArrayList<String>(); // FIXME

        mRuntime = new cucumber.runtime.Runtime(gluePaths, backends, false);

               
        

        // FIXME: This happens in more than one place.
        // Also, both these packages seem to be the same...
        String[] apkPaths = { getTargetContext().getPackageCodePath(), getContext().getPackageCodePath() };
        ClassPathPackageInfoSource.setApkPaths(apkPaths);
        
        Log.d(TAG, "findCucumberTestCases() targetContext packageCodePath: " + getTargetContext().getPackageCodePath());
        Log.d(TAG, "findCucumberTestCases() context       packageCodePath: " + getContext().getPackageCodePath());
        Log.d(TAG, "findCucumberTestCases() targetContext packageName:     " + getTargetContext().getPackageName());
        Log.d(TAG, "findCucumberTestCases() context       packageName:     " + getContext().getPackageName());

        ClassPathPackageInfoSource source = new ClassPathPackageInfoSource();
        source.setClassLoader(getContext().getClassLoader());
        // ClassPathPackageInfo info = source.getPackageInfo(getTargetContext().getPackageName()); // FIXME: getContext() or getTargetContext() ?
        mClassPathPackageInfo = source.getPackageInfo(getContext().getPackageName());
        
        Log.d(TAG, "findCucucmberTests() info topLevelClassesRecursive: " + mClassPathPackageInfo.getTopLevelClassesRecursive());       
        
        
        
        try {
            final TestSuite rootSuite = new TestSuite("Cucumber Tests WOOOHOO");

            final Class<?>[] testCases = findCucumberTestCases();
            
            Log.d(TAG, "onStart cucumber test cases: " + testCases);
            
            if (testCases.length == 0)
                throw new Exception("No Cucumber tests found.");

            for (Class<?> klass : testCases) {
                CucumberTest testAnnotation = (CucumberTest) klass.getAnnotation(CucumberTest.class);

                List<CucumberFeature> cucumberFeatures = new ArrayList<CucumberFeature>();
                FeatureBuilder builder = new FeatureBuilder(cucumberFeatures);
                builder.parse(new AndroidResource(getContext(), testAnnotation.resource()), new ArrayList<Object>());
                
                CucumberFeature cucumberFeature = cucumberFeatures.get(0);
                TestSuite featureSuite = new TestSuite(cucumberFeature.getFeature().getName());

                mClassFeatures.put(klass, cucumberFeature);
                
                for (CucumberTagStatement cucumberTagStatement : cucumberFeature.getFeatureElements()) {
                    if (cucumberTagStatement instanceof CucumberScenario) {
                        final CucumberScenario scenario = (CucumberScenario) cucumberTagStatement;
                        TestCase testCase = (TestCase) klass.newInstance();
                        testCase.setName(scenario.getVisualName());
                        featureSuite.addTest(testCase);
                        
                    } else {
                        // FIXME
                        throw new Exception("Unknown thing");
                    }
                }
                    
                rootSuite.addTest(featureSuite);
            }


            for (TestCase testCase : mTestRunner.getTestCases()) {
                Log.d(TAG, "Existing TestCase: " + testCase.getName());
            }
            
            mTestRunner.setTest(rootSuite);
            
            for (TestCase testCase : mTestRunner.getTestCases()) {
                Log.d(TAG, "New TestCase: " + testCase.getName());
            }
            
            // FIXME:
            // Append cucumber tests instead of overwriting existing tests...
            //Enumeration tests = rootSuite.tests();
            //while (tests.hasMoreElements()) {
            //    mTestRunner.getTestCases().add((TestCase) tests.nextElement());
            //}

        } catch (Exception e) {
            if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            else
                throw new RuntimeException(e);
        }

        super.onStart();
    }

    private Class<?>[] findCucumberTestCases() {
        List<Class<?>> result = new ArrayList<Class<?>>();
        
        for (Class<?> klass : mClassPathPackageInfo.getTopLevelClassesRecursive()) {
            Log.d(TAG, "findCucumberTests() Found top-level class: " + klass.getName());
            
            if (klass.isAnnotationPresent(CucumberTest.class)) {
                result.add(klass);                                            
            }
        }
        
        Log.d(TAG, "findCucumberTestCases(): " + result);

        return result.toArray(new Class[result.size()]);
    }

    @Override
    protected AndroidTestRunner getAndroidTestRunner () {
        mTestRunner = super.getAndroidTestRunner();
        return mTestRunner;
    }

    public void runScenario(TestCase testCase) throws Throwable {
        StringWriter    writer    = new StringWriter();
        //PrettyFormatter formatter = new PrettyFormatter(writer, false, true);
        AndroidFormatter formatter = new AndroidFormatter(writer, false, true);

        CucumberScenario scenario = findScenario(testCase);

        // FIXME:
        List<String> gluePaths = gluePaths(testCase.getClass());
        String[] apkPaths = { getTargetContext().getPackageCodePath(), getContext().getPackageCodePath() };
        // List<String> gluePaths = Arrays.asList(apkPaths);
        gluePaths.addAll(Arrays.asList(apkPaths));
        
        sCurrentTestCase = testCase;

        // This code came from the CucumberScenario.run() method but was
        // modified to actually throw an exception on failure.
        // scenario.run(formatter, formatter, mRuntime, new ArrayList<Backend>(), gluePaths);
        scenario.createWorld(gluePaths, mRuntime);
        Throwable throwable = scenario.runBackground(formatter, formatter); // FIXME: Break this out so it can be logged like steps.
        if (throwable != null)
            throw throwable;
        scenario.format(formatter);

        Log.d("UberCucumber", scenario.getVisualName());
        
        for (Step step : scenario.getSteps()) {

            if (step.getRows() != null) {
                StringBuilder tableStringBuilder = new StringBuilder();
                PrettyFormatter f = new PrettyFormatter(tableStringBuilder, true, false);
                f.table(step.getRows());
                Log.d("UberCucumber", "... " + step.getName() + "\n" + tableStringBuilder.toString());
            } else
                Log.d("UberCucumber", "... " + step.getName());


            throwable = scenario.runStep(step, formatter);
            if (throwable != null)
                throw throwable;
            
            // FIXME: sadface
            Field undefinedStepsField = cucumber.runtime.Runtime.class.getDeclaredField("undefinedSteps");
            undefinedStepsField.setAccessible(true);
            List<Step> undefinedSteps = (List<Step>) undefinedStepsField.get(mRuntime);
            if (undefinedSteps.contains(step)) {
                throw new StepDefNotFoundException(step);
            }
        }
        scenario.disposeWorld();

        sCurrentTestCase = null;

        // FIXME: Log output is a problem...
        Log.d(TAG, "Finished??? " + testCase.getName());
        for (String line : writer.toString().split("\n")) {
            Log.d(TAG, line);
        }
    }
    
    private CucumberScenario findScenario(TestCase testCase) {
        CucumberFeature cucumberFeature = mClassFeatures.get(testCase.getClass());
        for (CucumberTagStatement tag : cucumberFeature.getFeatureElements()) {
            if (tag instanceof CucumberScenario) {
                if (tag.getVisualName().equals(testCase.getName())) {
                    return (CucumberScenario) tag;
                }
            }
        }
        return null;
    }

    private static List<String> gluePaths(Class<?> clazz) {
      String className = clazz.getName();
      String packageName = packageName(className);
      List<String> gluePaths = new ArrayList<String>();
      gluePaths.add(packageName);
      return gluePaths;
    }

    private static String packageName(String className) {
      return className.substring(0, Math.max(0, className.lastIndexOf(".")));
    }
}
