package com.adyen.mirakl;

import cucumber.api.java.en.Then;

import javax.annotation.Resource;
import java.util.Map;

public class MyTestStepsVerify extends CommonThing {

    @Then("^I can get my state$")
    public void iCanGetMyState(){
        System.out.println(cucumberStateThing);
    }
}
