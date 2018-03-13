package com.adyen.mirakl.cucumber.stepdefs;

import com.adyen.mirakl.cucumber.stepdefs.helpers.stepshelper.StepDefsHelper;
import com.mirakl.client.mmp.domain.shop.MiraklShop;
import com.mirakl.client.mmp.operator.domain.shop.create.MiraklCreatedShops;
import cucumber.api.DataTable;
import cucumber.api.java.en.And;
import cucumber.api.java.en.When;

import static com.adyen.mirakl.cucumber.stepdefs.helpers.hooks.CucumberHooks.cucumberMap;
import static com.adyen.mirakl.cucumber.stepdefs.helpers.hooks.CucumberHooks.cucumberTable;
import static com.adyen.mirakl.cucumber.stepdefs.helpers.hooks.CucumberHooks.rows;

public class MiraklUpdateShopsSteps extends StepDefsHelper {

    @When("^the seller uploads a Bank Statement in Mirakl$")
    public void theSellerUploadsABankStatementInMirakl() {
        MiraklCreatedShops createdShops = (MiraklCreatedShops) cucumberMap.get("createdShops");
        String shopId = retrieveShopIdFromCreatedShop(createdShops);
        miraklUpdateShopApi.uploadBankStatementToExistingShop(shopId, miraklMarketplacePlatformOperatorApiClient);
    }

    @When("^the IBAN has been modified in Mirakl$")
    public void theIBANHasBeenModifiedInMirakl(DataTable table) {
        cucumberTable.put("table", table);
        cucumberMap.put("iban", rows().get(0).get("iban"));

        MiraklShop createdShop = (MiraklShop) cucumberMap.get("createdShop");
        MiraklShop updatedMiraklShop = miraklUpdateShopsApi.updateShopsIbanNumberOnly(createdShop, createdShop.getId(), miraklMarketplacePlatformOperatorApiClient, rows());

        // map needs to be cleared as we've updated the shop
        cucumberMap.remove("createdShop");
        cucumberMap.put("createdShop", updatedMiraklShop);
    }

    @And("^a new IBAN has been provided by the seller in Mirakl and the mandatory IBAN fields have been provided$")
    public void aNewIBANHasBeenProvidedByTheSellerInMiraklAndTheMandatoryIBANFieldsHaveBeenProvided() {
        MiraklShop createdShop = (MiraklShop) cucumberMap.get("createdShop");
        MiraklShop updatedMiraklShop = miraklUpdateShopsApi.updateShopToAddBankDetails(createdShop, createdShop.getId(), miraklMarketplacePlatformOperatorApiClient);

        // map needs to be cleared as we've updated the shop
        cucumberMap.remove("createdShop");
        cucumberMap.put("createdShop", updatedMiraklShop);
    }

    @And("^Mirakl has been updated with a taxId$")
    public void miraklHasBeenUpdatedWithATaxId() {
        MiraklShop createdShop = (MiraklShop) cucumberMap.get("createdShop");
        MiraklShop updatedMiraklShop = miraklUpdateShopsApi.updateShopToIncludeVATNumber(createdShop, createdShop.getId(), miraklMarketplacePlatformOperatorApiClient);
        // map needs to be cleared as we've updated the shop
        cucumberMap.remove("createdShop");
        cucumberMap.put("createdShop", updatedMiraklShop);
    }

    @When("^the Mirakl Shop Details have been updated as the same as before$")
    public void theMiraklShopDetailsHaveBeenUpdatedAsTheSameAsBefore(DataTable table) {
        cucumberTable.put("table", table);
        MiraklShop createdShop = (MiraklShop) cucumberMap.get("createdShop");
        rows().forEach(row ->
            miraklUpdateShopApi.updateExistingShopsContactInfoWithTableData(createdShop, createdShop.getId(), miraklMarketplacePlatformOperatorApiClient, row)
        );
    }

    @When("^Invalid data is provided to Mirakl and pushed to Adyen multiple times$")
    public void invaliddataIsProvidedToAdyenMultipleTimes(DataTable table) {
        cucumberTable.put("table", table);
        MiraklShop createdShop = (MiraklShop) cucumberMap.get("createdShop");
        rows().forEach(row -> {
            miraklUpdateShopApi.updateExistingShopsContactInfoWithTableData(createdShop, createdShop.getId(), miraklMarketplacePlatformOperatorApiClient, row);
            shopService.processUpdatedShops();
        });
    }
}
