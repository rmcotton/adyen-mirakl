package com.adyen.mirakl;

import cucumber.api.java.Before;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;


public class MyBeforeHook extends CommonThing{

    @Before
    public void doBeforeThing(){
        cucumberStateThing.clear();
    }



}
