package com.adyen.mirakl;

import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.Map;

public class MyTestSteps extends CommonThing{


    @Given("^I set my state$")
    public void iSetMyState(){
        System.out.println("\n\n\n\n");
        System.out.println(cucumberStateThing);
        cucumberStateThing.put("first", "one");
    }

    @And("^I add to my state$")
    public void iAddToMyState(){
        System.out.println(cucumberStateThing);
        cucumberStateThing.put("second", "two");
    }
}
