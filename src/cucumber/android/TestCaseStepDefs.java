package cucumber.android;

import junit.framework.TestCase;

public abstract class TestCaseStepDefs<T extends TestCase> {    
    protected T getTestCase() {
        TestCase currentTestCase = CucumberInstrumentationTestRunner.sCurrentTestCase;
        if (currentTestCase != null) {
            return (T) currentTestCase;
        }
        return null;
    }
}
