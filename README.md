# Android support for Cucumber

This is a work in progress. It works well enough but isn't considered stable.

## License

This code is open-sourced under [The MIT License](http://www.opensource.org/licenses/mit-license.php). The goal is get it included upstream into the [cucumber-jvm](https://github.com/cucumber/cucumber-jvm) project.

## Usage

1. Include cucumber-android into your application's test project, either as a jar (recommended) or as a library project.

2. Add an additional `<instrumentation>` element inside your *test project's* `AndroidManifest.xml`:

        <instrumentation
            android:name="cucumber.android.CucumberInstrumentationTestRunner"
            android:targetPackage="com.example.calculator" />

    Remember to set `targetPackage` to your application's package name.

3. Create a new package such as `com.example.calculator.test.cucumber`. If your existing unit tests are inside your test project's top-level package (`com.example.calculator.test`), consider moving them into a sub-package (such as `com.example.calculator.test.unit`).

3. Create a new "Android Test" run configuration in your IDE and set the instrumentation runner to `cucumber.android.CucumberInstrumentationTestRunner`. Set it to run all tests inside the package you just created.

## Writing Features and Step Definitions

1. Features are stored as Android "raw" resources. Create a directory `res/raw/features` and place your features here. For consistency, name your files with a `.feature` extension. 

   For example, if you're writing tests for an RPN calculator app, the file `res/raw/features/addition.feature` may contain:

        Feature: Addition
           Scenario: Add two numbers
               Given I have entered 50 into the calculator
               And I have entered 70 into the calculator
               When I press add
               Then The result should be 120

2. Each feature also needs a corresponding TestCase class which defines where to find the resource and which activity to start the test with. These classes must be inside the package you configured in your Run Configuration (see above).
    
    **This will be cleaned up in a future revision.**
		
       @CucumberTest(resource = R.raw.addition)
       public class AdditionFeature extends CucumberActivityInstrumentationTestCase2<CalculatorActivity> {
           public AdditionFeature() {
               super(MainActivity.class);
           }
       }


2. For consistency, step definitions should be defined in their own package such as `com.example.calculator.test.cucumber.steps` and must extend the TestCaseStepDefs class. 

    The following example also demonstrates how to use [Robotium](XXX) in your step definitions.


        public class CalculatorSteps extends TestCaseStepDefs<ActivityInstrumentationTestCase2<?>> {
            private Solo mRobotium;
        
            @Before
            public void before() {
                mRobotium = new Solo(getTestCase().getInstrumentation(), getTestCase().getActivity());
            }

            @Given("I have entered (\\d+) into the calculator")
            public void i_have_entered(int number) {
                mRobotium.enterText(0, String.valueOf(number));
                mRobotium.clickOnButton("Enter");
            }

            @When("I press add")
            public void i_press_add() {
                mRobotium.clickOnButton("+");
            }

            @Then("The result should be (\\d+)")
            public void the_result_should_be(int expected_result) {
                Assert.assertTrue(mRobotium.searchText(String.valueOf(expected_result)));
            }
        }

