package com.adyen.mirakl.cucumber.stepdefs;

import com.adyen.mirakl.AdyenMiraklConnectorApp;

import com.adyen.mirakl.cucumber.stepdefs.helpers.stepshelper.StepDefsHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.ResultActions;

import org.springframework.boot.test.context.SpringBootTest;

@WebAppConfiguration
@SpringBootTest
@ContextConfiguration(classes = AdyenMiraklConnectorApp.class)
public abstract class StepDefs extends StepDefsHelper{

    protected ResultActions actions;

}
