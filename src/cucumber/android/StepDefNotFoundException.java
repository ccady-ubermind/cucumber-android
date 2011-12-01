package cucumber.android;

import gherkin.formatter.model.Step;

public class StepDefNotFoundException extends Exception {
    public StepDefNotFoundException(Step step) {
        super(step.getName());
    }
}
