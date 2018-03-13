package com.adyen.mirakl.cucumber.stepdefs;

import com.adyen.mirakl.cucumber.stepdefs.helpers.stepshelper.StepDefsHelper;
import com.adyen.model.marketpay.GetAccountHolderRequest;
import com.adyen.model.marketpay.GetAccountHolderResponse;
import com.adyen.service.exception.ApiException;
import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.mirakl.client.mmp.domain.shop.MiraklShop;
import cucumber.api.DataTable;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.adyen.mirakl.cucumber.stepdefs.helpers.hooks.CucumberHooks.*;
import static junit.framework.TestCase.fail;
import static org.awaitility.Awaitility.await;

public class AccountManagementSteps extends StepDefsHelper {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Then("^we process the data and push to Adyen$")
    public void adyenWillProcessTheData() {
        shopService.processUpdatedShops();
    }

    @And("^an AccountHolder will be created in Adyen with status Active$")
    public void anAccountHolderWillBeCreatedInAdyenWithStatusActive() throws Throwable {
        GetAccountHolderRequest accountHolderRequest = new GetAccountHolderRequest();
        MiraklShop createdShop = (MiraklShop) cucumberMap.get("createdShop");
        accountHolderRequest.setAccountHolderCode(createdShop.getId());

        try{
            GetAccountHolderResponse accountHolderResponse = adyenAccountService.getAccountHolder(accountHolderRequest);
            Assertions.assertThat(accountHolderResponse.getAccountHolderStatus().getStatus().toString()).isEqualTo("Active");
        }catch (ApiException e){
            log.error("Failing test due to exception", e);
            fail(e.getError().toString());
        }
    }

    @And("^a notification will be sent pertaining to (.*)$")
    public void aNotificationWillBeSentPertainingToACCOUNT_HOLDER_CREATED(String notification) {
        waitForNotification();
        await().untilAsserted(() -> {
            MiraklShop createdShop = (MiraklShop) cucumberMap.get("createdShop");
            Map<String, Object> mappedAdyenNotificationResponse = getAdyenNotificationBody(notification, createdShop.getId());
            Assertions.assertThat(mappedAdyenNotificationResponse).isNotNull();
            DocumentContext notificationResponse = JsonPath.parse(mappedAdyenNotificationResponse);
            cucumberMap.put("notificationResponse", notificationResponse);
            Assertions.assertThat(notificationResponse.read("content.accountHolderCode").toString())
                .isEqualTo(createdShop.getId());
            Assertions.assertThat(notificationResponse.read("eventType").toString())
                .isEqualTo(notification);
        });
    }

    @When("^a complete shareholder is not provided$")
    public void aCompleteShareholderIsNotProvided() {
        // UBOs were not provided in this test.
    }

    @Then("^no account holder is created in Adyen$")
    public void noAccountHolderIsCreatedInAdyen() {
        waitForNotification();
        await().untilAsserted(() -> {
            MiraklShop createdShop = (MiraklShop) cucumberMap.get("createdShop");
            Map mapResult = restAssuredAdyenApi.getAdyenNotificationBody(startUpCucumberHook
                .getBaseRequestBinUrlPath(), createdShop.getId(), "ACCOUNT_HOLDER_CREATED", null);
            Assertions.assertThat(mapResult == null);
        });
    }

    @And("^the shop data is correctly mapped to the Adyen Account$")
    public void theShopDataIsCorrectlyMappedToTheAdyenAccount() {
        MiraklShop createdShop = (MiraklShop) cucumberMap.get("createdShop");
        DocumentContext notificationResponse = (DocumentContext)cucumberMap.get("notificationResponse");
        ImmutableList<String> adyen = assertionHelper.adyenAccountDataBuilder(notificationResponse).build();
        ImmutableList<String> mirakl = assertionHelper.miraklShopDataBuilder(createdShop.getContactInformation().getEmail(), createdShop).build();
        Assertions.assertThat(adyen).containsAll(mirakl);
    }

    @And("^the account holder is created in Adyen with status Active$")
    public void theAccountHolderIsCreatedInAdyenWithStatusActive()  {
        DocumentContext notificationResponse = (DocumentContext)cucumberMap.get("notificationResponse");
        Assertions.assertThat(notificationResponse.read("content.accountHolderStatus.status").toString())
            .isEqualTo("Active");
    }

    @And("^the shop data is correctly mapped to the Adyen Business Account$")
    public void theShopDataIsCorrectlyMappedToTheAdyenBusinessAccount()  {
        MiraklShop createdShop = (MiraklShop) cucumberMap.get("createdShop");
        DocumentContext notificationResponse = (DocumentContext)cucumberMap.get("notificationResponse");
        ImmutableList<String> adyen = assertionHelper.adyenShareHolderAccountDataBuilder(notificationResponse).build();
        ImmutableList<String> mirakl = assertionHelper.miraklShopShareHolderDataBuilder(createdShop).build();
        Assertions.assertThat(adyen).containsAll(mirakl);
    }

    @When("^the Mirakl Shop Details have been updated$")
    public void theMiraklShopDetailsHaveBeenUpdated(DataTable table) {
        cucumberTable.put("table", table);
        MiraklShop createdShop = (MiraklShop) cucumberMap.get("createdShop");
        rows().forEach(row ->
            miraklUpdateShopsApi.updateExistingShopsContactInfoWithTableData(createdShop, createdShop.getId(), miraklMarketplacePlatformOperatorApiClient, row)
        );
    }

    @And("^a notification of (.*) will not be sent$")
    public void aNotificationOfACCOUNT_HOLDER_UPDATEDWillNotBeSent(String notification) {
        waitForNotification();
        await().untilAsserted(() -> {
            MiraklShop createdShop = (MiraklShop) cucumberMap.get("createdShop");
            Map<String, Object> response = restAssuredAdyenApi
                .getAdyenNotificationBody(startUpCucumberHook.getBaseRequestBinUrlPath(), createdShop.getId(), notification, null);
            Assertions.assertThat(response).isNull();
        });
    }

    @Given("^a AccountHolder exists who (?:is not|is) eligible for payout$")
    public void aAccountHolderExistsWhoHasPassedKYCChecksAndIsEligibleForPayout(DataTable table) throws Throwable {
        cucumberTable.put("table", table);
        String seller = rows().get(0).get("seller").toString();
        String accountHolderCode = shopConfiguration.shopIds.get(seller).toString();
        cucumberMap.put("accountHolderCode", accountHolderCode);

        GetAccountHolderRequest request = new GetAccountHolderRequest();
        request.setAccountHolderCode(accountHolderCode);
        GetAccountHolderResponse response = adyenConfiguration.adyenAccountService().getAccountHolder(request);
        Boolean allowPayout = response.getAccountHolderStatus().getPayoutState().getAllowPayout();
        String accountCode = response.getAccounts()
            .stream()
            .map(com.adyen.model.marketpay.Account::getAccountCode)
            .findFirst()
            .orElse(null);
        cucumberMap.put("accountCode", accountCode);

        Assertions.assertThat(allowPayout)
            .withFailMessage("Payout status is not true for accountHolderCode: <%s> (seller: <%s>)", accountHolderCode, seller)
            .isEqualTo(Boolean.parseBoolean(rows().get(0).get("allowPayout").toString()));
    }
}
