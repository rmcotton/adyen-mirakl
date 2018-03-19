package com.adyen.mirakl.cucumber.stepdefs;

import com.adyen.mirakl.web.rest.AdyenNotificationResource;
import com.adyen.mirakl.web.rest.TestUtil;
import com.mirakl.client.mmp.domain.shop.MiraklShop;
import cucumber.api.java.Before;
import cucumber.api.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class ConnectorAppAdyenSteps extends StepDefs {

    @Autowired
    private AdyenNotificationResource adyenNotificationResource;

    private MockMvc restAdyenNotificationMockMvc;

    @Before
    public void setup() {
        this.restAdyenNotificationMockMvc = MockMvcBuilders.standaloneSetup(adyenNotificationResource).build();
    }

    @When("^a RETRY_LIMIT_REACHED verificationStatus has been sent to the Connector$")
    public void aRETRY_LIMIT_REACHEDVerificationStatusHasBeenSentToTheConnector(String notificationTemplate) throws Throwable {
        MiraklShop createdShop = (MiraklShop)cucumberMap.get("createdShop");
        String notification = notificationTemplate.replaceAll("\\$shopId\\$", createdShop.getId());
        restAdyenNotificationMockMvc.perform(post("/api/adyen-notifications")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(notification))
            .andExpect(status().is(201));
    }
}
