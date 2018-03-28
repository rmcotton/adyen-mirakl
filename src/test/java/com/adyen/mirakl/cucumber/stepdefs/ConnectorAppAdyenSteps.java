package com.adyen.mirakl.cucumber.stepdefs;

import com.adyen.mirakl.web.rest.AdyenNotificationResource;
import com.adyen.mirakl.web.rest.TestUtil;
import com.adyen.model.marketpay.GetAccountHolderResponse;
import com.adyen.model.marketpay.ShareholderContact;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.jayway.jsonpath.DocumentContext;
import com.mirakl.client.mmp.domain.shop.MiraklShop;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class ConnectorAppAdyenSteps extends StepDefs {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private AdyenNotificationResource adyenNotificationResource;

    private MockMvc restAdyenNotificationMockMvc;

    @Before
    public void setup() {
        this.restAdyenNotificationMockMvc = MockMvcBuilders.standaloneSetup(adyenNotificationResource).build();
    }

    @When("^a RETRY_LIMIT_REACHED verificationStatus has been sent to the Connector$")
    public void aRETRY_LIMIT_REACHEDVerificationStatusHasBeenSentToTheConnector(String notificationTemplate) throws Throwable {
        MiraklShop createdShop = (MiraklShop) cucumberMap.get("createdShop");
        String notification = notificationTemplate.replaceAll("\\$shopId\\$", createdShop.getId());
        restAdyenNotificationMockMvc.perform(post("/api/adyen-notifications")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(notification))
            .andExpect(status().is(201));
    }

    @And("^the notifications are sent to Connector App$")
    public void theNotificationsAreSentToConnectorApp() throws Exception {
        ImmutableList<DocumentContext> notifications = (ImmutableList<DocumentContext>) cucumberMap.get("notifications");
        for (DocumentContext notification : notifications) {
            restAdyenNotificationMockMvc.perform(post("/api/adyen-notifications")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(notification.jsonString()))
                .andExpect(status().is(201));
            log.info("Notification posted to Connector: [%s]", notification.jsonString());
        }
    }

    @When("^the IDENTITY_VERIFICATION notifications containing INVALID_DATA status are sent to the Connector for each UBO$")
    public void theIDENTITY_VERIFICATIONNotificationsAreSentToTheConnector() throws Throwable {
        List<String> notifications = new LinkedList<>();
        URL url = Resources.getResource("adyenRequests/CUCUMBER_IDENTITY_VERIFICATION_INVALID_DATA.json");
        MiraklShop shop = (MiraklShop) cucumberMap.get("createdShop");
        GetAccountHolderResponse accountHolder = retrieveAccountHolderResponse(shop.getId());
        String accountHolderCode = accountHolder.getAccountHolderCode();

        List<String> shareholderCodes = accountHolder.getAccountHolderDetails().getBusinessDetails().getShareholders().stream()
            .map(ShareholderContact::getShareholderCode)
            .collect(Collectors.toList());

        for (String shareholderCode : shareholderCodes) {
            try {
                notifications.add(Resources.toString(url, Charsets.UTF_8)
                    .replaceAll("\\$accountHolderCode\\$", accountHolderCode)
                    .replaceAll("\\$shareholderCode\\$", shareholderCode));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (String notification : notifications) {
            restAdyenNotificationMockMvc.perform(post("/api/adyen-notifications")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(notification))
                .andExpect(status().is(201));
            log.info("Notification posted to Connector: [%s]", notification);
        }
    }

    @When("^the (.*) notifications containing (.*) status are sent to the Connector$")
    public void theCOMPANY_VERIFICATIONNotificationsContainingINVALID_DATAStatusAreSentToTheConnector(String eventType, String verificationType) throws Throwable {
        URL url = Resources.getResource("adyenRequests/CUCUMBER_"+eventType+"_"+verificationType+".json");
        String stringJson = Resources.toString(url, Charsets.UTF_8);
        MiraklShop shop = (MiraklShop) cucumberMap.get("createdShop");
        String notification = stringJson.replaceAll("\\$accountHolderCode\\$", shop.getId());
        restAdyenNotificationMockMvc.perform(post("/api/adyen-notifications")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(notification))
            .andExpect(status().is(201));
        log.info("Notification posted to Connector: [%s]", notification);
    }

    @When("^the notification is sent to the Connector$")
    public void theNotificationIsSentToTheConnector() throws Throwable {
        DocumentContext  notification = (DocumentContext)cucumberMap.get("notification");
        restAdyenNotificationMockMvc.perform(post("/api/adyen-notifications")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(notification.jsonString()))
            .andExpect(status().is(201));
        log.info(String.format("Notification posted to Connector: [%s]", notification.jsonString()));
    }
}
