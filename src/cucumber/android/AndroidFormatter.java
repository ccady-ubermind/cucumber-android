package cucumber.android;

import gherkin.formatter.PrettyFormatter;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Step;
import android.util.Log;

public class AndroidFormatter extends PrettyFormatter {
    public AndroidFormatter(Appendable out, boolean monochrome, boolean executing) {
        super(out, monochrome, executing);
    }

    public void step (Step step)
    {
        super.step(step);
        Log.d("AndroidFormatter", "GOT STEP!!! " + step);
    }

    public void result (Result result)
    {
        super.result(result);
        
        Log.d("AndroidFormatter", "GOT RESULT!!! " + result + "  " + result.getStatus() + "  ");
        
        // FIXME: Not best place for this!
//        if (result.getStatus() == "undefined")
//            throw new RuntimeException("Step definition not found.");
    }
}
