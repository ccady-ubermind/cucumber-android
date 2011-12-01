package cucumber.android;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;

public abstract class CucumberActivityInstrumentationTestCase2<T extends Activity> extends ActivityInstrumentationTestCase2 {
    public CucumberActivityInstrumentationTestCase2 (Class<T> activityClass) {
        super(activityClass);
    }

    @Override
    public final void runTest() throws Throwable {
        // FIXME: May need to do something with super here...
        ((CucumberInstrumentationTestRunner) getInstrumentation()).runScenario(this);
    }
}
