package com.adyen.mirakl.cucumber.stepdefs;

import com.adyen.mirakl.cucumber.stepdefs.helpers.stepshelper.StepDefsHelper;
import com.adyen.mirakl.domain.AdyenPayoutError;
import com.adyen.model.marketpay.*;
import com.adyen.service.exception.ApiException;
import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.mirakl.client.mmp.domain.shop.MiraklShop;
import com.mirakl.client.mmp.operator.domain.shop.create.MiraklCreatedShops;
import cucumber.api.DataTable;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.ResponseBody;
import net.minidev.json.JSONArray;
import org.assertj.core.api.Assertions;
import org.awaitility.Duration;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static junit.framework.TestCase.fail;
import static org.awaitility.Awaitility.await;

public class MiraklAdyenSteps extends StepDefsHelper {

    private MiraklShop shop;
    private DocumentContext notificationResponse;
    private String accountHolderCode;
    private List<DocumentContext> notifications;
    private DocumentContext adyenNotificationBody;
    private GetUploadedDocumentsResponse uploadedDocuments;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Given("^a shop has been created in Mirakl for an (.*) with Bank Information$")
    public void aShopHasBeenCreatedInMiraklForAnIndividualWithBankInformation(String legalEntity, DataTable table) {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);
        MiraklCreatedShops shops = miraklShopApi
            .createShopForIndividualWithBankDetails(miraklMarketplacePlatformOperatorApiClient, cucumberTable, legalEntity);
        shop = retrieveCreatedShop(shops);

        cucumberMap.put("createdShop", shop);
    }

    @Given("^a new shop has been created in Mirakl for an (.*)$")
    public void aNewShopHasBeenCreatedInMiraklForAnIndividual(String legalEntity, DataTable table) {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);
        MiraklCreatedShops shops = miraklShopApi.createShopForIndividual(miraklMarketplacePlatformOperatorApiClient, cucumberTable, legalEntity);
        shop = retrieveCreatedShop(shops);
    }

    @Given("^a shop has been created in Mirakl for an (.*) with mandatory KYC data$")
    public void aShopHasBeenCreatedInMiraklForAnIndividualWithMandatoryKYCData(String legalEntity, DataTable table) {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);
        MiraklCreatedShops shops = miraklShopApi.createShopForIndividualWithBankDetails(miraklMarketplacePlatformOperatorApiClient, cucumberTable, legalEntity);
        shop = retrieveCreatedShop(shops);

        cucumberMap.put("createdShop", shop);
    }

    @When("^a new shop has been created in Mirakl for a (.*)")
    public void aNewShopHasBeenCreatedInMiraklForABusiness(String legalEntity, DataTable table) {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);
        MiraklCreatedShops shops = miraklShopApi.createBusinessShopWithNoUBOs(miraklMarketplacePlatformOperatorApiClient, cucumberTable, legalEntity);
        shop = retrieveCreatedShop(shops);
    }

    @When("^a new shop has been created in Mirakl with UBO Data for a (.*)")
    public void aNewShopHasBeenCreatedAsABusinessWithUBOData(String legalEntity, DataTable table) {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);
        MiraklCreatedShops shops = miraklShopApi.createBusinessShopWithFullUboInfo(miraklMarketplacePlatformOperatorApiClient, cucumberTable, legalEntity);
        shop = retrieveCreatedShop(shops);
    }

    @Given("^a new (.*) shop has been created in Mirakl with some Mandatory data missing$")
    public void aNewBusinessShopHasBeenCreatedInMiraklWithoutMandatoryShareholderInformation(String legalEntity, DataTable table) {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);
        MiraklCreatedShops shops = miraklShopApi.createBusinessShopWithMissingUboInfo(miraklMarketplacePlatformOperatorApiClient, cucumberTable, legalEntity);
        shop = retrieveCreatedShop(shops);

        cucumberMap.put("createdShop", shop);
    }

    @Given("^a new (.*) shop has been created in Mirakl with invalid data$")
    public void aNewBusinessShopHasBeenCreatedInMiraklWithInvalidData(String legalEntity, DataTable table) {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);
        MiraklCreatedShops shops = miraklShopApi.createBusinessShopWithMissingUboInfo(miraklMarketplacePlatformOperatorApiClient, cucumberTable, legalEntity);
        shop = retrieveCreatedShop(shops);

        cucumberMap.put("createdShop", shop);
    }

    @When("^the seller uploads a Bank Statement in Mirakl$")
    public void theSellerUploadsABankStatementInMirakl() {
        miraklUpdateShopApi
            .uploadBankStatementToExistingShop(shop.getId(), miraklMarketplacePlatformOperatorApiClient);
    }

    @When("^the IBAN has been modified in Mirakl$")
    public void theIBANHasBeenModifiedInMirakl(DataTable table) {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);
        shop = miraklUpdateShopApi
            .updateShopsIbanNumberOnly(shop, shop.getId(), miraklMarketplacePlatformOperatorApiClient, cucumberTable);
    }

    @And("^a new IBAN has been provided by the seller in Mirakl and the mandatory IBAN fields have been provided$")
    public void aNewIBANHasBeenProvidedByTheSellerInMiraklAndTheMandatoryIBANFieldsHaveBeenProvided() {
        shop = miraklUpdateShopApi
            .updateShopToAddBankDetails(shop, shop.getId(), miraklMarketplacePlatformOperatorApiClient);
    }

    @And("^Mirakl has been updated with a taxId$")
    public void miraklHasBeenUpdatedWithATaxId() {
        shop = miraklUpdateShopApi
            .updateShopToIncludeVATNumber(shop, shop.getId(), miraklMarketplacePlatformOperatorApiClient);
    }

    @When("^we update the shop by adding more shareholder data$")
    public void weUpdateTheShopByAddingMoreShareholderData(DataTable table) {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);
        shop = miraklUpdateShopApi
            .addMoreUbosToShop(shop, shop.getId(), miraklMarketplacePlatformOperatorApiClient, cucumberTable);
    }

    @When("^the shareholder data has been updated in Mirakl$")
    public void theShareholderDataHasBeenUpdatedInMirakl(DataTable table) {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);
        shop = miraklUpdateShopApi
            .updateUboData(shop, shop.getId(), miraklMarketplacePlatformOperatorApiClient, cucumberTable);
    }

    @Then("^the (.*) notification is sent by Adyen comprising of (.*) and (.*)")
    public void theACCOUNT_HOLDER_VERIFICATIONNotificationIsSentByAdyenComprisingOfBANK_ACCOUNT_VERIFICATIONAndPASSED(String notification,
                                                                                                                      String verificationType,
                                                                                                                      String verificationStatus) {
        waitForNotification();
        await().untilAsserted(() -> {
            Map<String, Object> adyenNotificationBody = restAssuredAdyenApi
                .getAdyenNotificationBody(startUpTestingHook.getBaseRequestBinUrlPath(), shop.getId(), notification, verificationType);

            Assertions.assertThat(adyenNotificationBody).withFailMessage("No data received from notification endpoint").isNotNull();
            Assertions.assertThat(JsonPath.parse(adyenNotificationBody.get("content"))
                .read("verificationStatus").toString()).isEqualTo(verificationStatus);
            Assertions.assertThat(JsonPath.parse(adyenNotificationBody.get("content"))
                .read("verificationType").toString()).isEqualTo(verificationType);
        });
    }

    @Then("^a new bankAccountDetail will be created for the existing Account Holder$")
    public void aNewBankAccountDetailWillBeCreatedForTheExistingAccountHolder(DataTable table) {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);
        waitForNotification();
        await().untilAsserted(() -> {
            String eventType = cucumberTable.get(0).get("eventType");
            Map<String, Object> adyenNotificationBody = retrieveAdyenNotificationBody(eventType, shop.getId());

            List<Map<Object, Object>> bankAccountDetails = JsonPath.parse(adyenNotificationBody
                .get("content"))
                .read("accountHolderDetails.bankAccountDetails");

            ImmutableList<String> miraklBankAccountDetail = assertionHelper.miraklBankAccountInformation(shop).build();
            ImmutableList<String> adyenBankAccountDetail = assertionHelper.adyenBankAccountDetail(bankAccountDetails, cucumberTable).build();
            Assertions.assertThat(miraklBankAccountDetail).containsAll(adyenBankAccountDetail);

            Assertions
                .assertThat(assertionHelper.getParsedBankAccountDetail().read("primaryAccount").toString())
                .isEqualTo("true");
            Assertions
                .assertThat(assertionHelper.getParsedBankAccountDetail().read("bankAccountUUID").toString())
                .isNotEmpty();
        });
    }

    @Then("^adyen will send the (.*) comprising of (\\w*) and status of (.*)")
    public void adyenWillSendTheACCOUNT_HOLDER_VERIFICATIONComprisingOfCOMPANY_VERIFICATION(String eventType, String verificationType, String status) {
        waitForNotification();
        await().untilAsserted(() -> {
            Map<String, Object> adyenNotificationBody = restAssuredAdyenApi
                .getAdyenNotificationBody(startUpTestingHook.getBaseRequestBinUrlPath(), shop.getId(), eventType, verificationType);
            Assertions.assertThat(adyenNotificationBody).isNotEmpty();
            Assertions.assertThat(JsonPath.parse(adyenNotificationBody.get("content"))
                .read("verificationType").toString()).isEqualTo(verificationType);
            Assertions.assertThat(JsonPath.parse(adyenNotificationBody.get("content"))
                .read("verificationStatus").toString()).isEqualTo(status);
        });
    }

    @Then("^adyen will send the (.*) comprising of accountHolder (.*) and status of (.*)")
    public void adyenWillSendTheACCOUNT_HOLDER_VERIFICATIONComprisingOfCOMPANY_VERIFICATIONAccountHolder(String eventType, String verificationType, String status) {
        waitForNotification();
        await().untilAsserted(() -> {
            Map<String, Object> adyenNotificationBody = restAssuredAdyenApi
                .getAdyenNotificationBody(startUpTestingHook.getBaseRequestBinUrlPath(), shop.getId(), eventType, verificationType);
            Assertions.assertThat(adyenNotificationBody).isNotEmpty();
            Assertions.assertThat(JsonPath.parse(adyenNotificationBody.get("content"))
                .read("verification.accountHolder.checks[0].type").toString()).isEqualTo(verificationType);
            Assertions.assertThat(JsonPath.parse(adyenNotificationBody.get("content"))
                .read("verification.accountHolder.checks[0].status").toString()).isEqualTo(status);
        });
    }

    @Then("^adyen will send the (.*) notification with multiple (.*) of status (.*)")
    public void adyenWillSendTheACCOUNT_HOLDER_UPDATEDNotificationWithMultipleIDENTITY_VERIFICATIONOfStatusDATA_PROVIDED(
        String eventType, String verificationType, String verificationStatus) {
        waitForNotification();
        await().untilAsserted(() -> {
            Map<String, Object> adyenNotificationBody = restAssuredAdyenApi
                .getAdyenNotificationBody(startUpCucumberHook.getBaseRequestBinUrlPath(), shop.getId(), eventType, verificationType);

            Assertions.assertThat(adyenNotificationBody).isNotEmpty();
            JSONArray shareholderJsonArray = JsonPath.parse(adyenNotificationBody).read("content.verification.shareholders.*");
            for (Object shareholder : shareholderJsonArray) {
                Object checks = JsonPath.parse(shareholder).read("checks[0]");
                Assertions.assertThat(JsonPath.parse(checks).read("type").toString()).isEqualTo(verificationType);
                Assertions.assertThat(JsonPath.parse(checks).read("status").toString()).isEqualTo(verificationStatus);
            }
        });
    }


    @And("^getAccountHolder will have the correct amount of shareholders and data in Adyen$")
    public void getaccountholderWillHaveTheCorrectAmountOfShareholdersAndDataInAdyen(DataTable table) throws Throwable {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);
        String maxUbos = cucumberTable.get(0).get("maxUbos");

        GetAccountHolderResponse accountHolder = getGetAccountHolderResponse(shop);

        BusinessDetails businessDetails = new BusinessDetails();
        List<ShareholderContact> shareholders = businessDetails.getShareholders();
        accountHolder.getAccountHolderDetails().businessDetails(businessDetails);

        for (ShareholderContact contact : shareholders) {
            Assertions
                .assertThat(shop.getContactInformation().getFirstname())
                .isEqualTo(contact.getName().getFirstName());
            Assertions
                .assertThat(shop.getContactInformation().getLastname())
                .isEqualTo(contact.getName().getLastName());
            Assertions
                .assertThat(shop.getContactInformation().getEmail())
                .isEqualTo(contact.getEmail());
            Assertions
                .assertThat(shareholders.size())
                .isEqualTo(Integer.valueOf(maxUbos));
        }
    }

    @And("^the document is successfully uploaded to Adyen$")
    public void theDocumentIsSuccessfullyUploadedToAdyen(DataTable table) throws Throwable {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);

        GetUploadedDocumentsRequest getUploadedDocumentsRequest = new GetUploadedDocumentsRequest();
        getUploadedDocumentsRequest.setAccountHolderCode(shop.getId());
        GetUploadedDocumentsResponse uploadedDocuments = adyenAccountService.getUploadedDocuments(getUploadedDocumentsRequest);

        boolean documentTypeAndFilenameMatch = uploadedDocuments.getDocumentDetails().stream()
            .anyMatch(doc ->
                DocumentDetail.DocumentTypeEnum.valueOf(cucumberTable.get(0).get("documentType")).equals(doc.getDocumentType())
                    && cucumberTable.get(0).get("filename").equals(doc.getFilename()));

        String uploadedDocResponse = uploadedDocuments.getDocumentDetails().toString();
        Assertions.assertThat(documentTypeAndFilenameMatch)
            .withFailMessage(String.format("Document upload response:[%s]", JsonPath.parse(uploadedDocResponse).toString()))
            .isTrue();
    }

    @Then("^an email will be sent to the seller$")
    public void anEmailWillBeSentToTheSeller(String title) {
        String email = shop.getContactInformation().getEmail();

        await().untilAsserted(() -> {
            ResponseBody responseBody = RestAssured.get(mailTrapConfiguration.mailTrapEndPoint()).thenReturn().body();
            List<Map<String, Object>> emailLists = responseBody.jsonPath().get();

            String htmlBody = null;
            Assertions.assertThat(emailLists.size()).isGreaterThan(0);
            for (Map list : emailLists) {
                if (list.get("to_email").equals(email)) {
                    htmlBody = list.get("html_body").toString();
                    Assertions.assertThat(email).isEqualTo(list.get("to_email"));
                    break;
                } else {
                    Assertions.fail("Email was not found in mailtrap. Email: [%s]", email);
                }
            }
            Assertions
                .assertThat(htmlBody).isNotNull();
            Document parsedBody = Jsoup.parse(htmlBody);
            Assertions
                .assertThat(parsedBody.body().text())
                .contains(shop.getId())
                .contains(shop.getContactInformation().getCivility())
                .contains(shop.getContactInformation().getFirstname())
                .contains(shop.getContactInformation().getLastname());

            Assertions.assertThat(parsedBody.title()).isEqualTo(title);
        });
    }

    @Then("^adyen will send the (.*) notification with status$")
    public void adyenWillSendTheACCOUNT_HOLDER_PAYOUTNotificationWithStatusCode(String notification, DataTable table) {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);
        waitForNotification();
        await().untilAsserted(() -> {
            Map<String, Object> adyenNotificationBody = retrieveAdyenNotificationBody(notification, shop.getId());
            DocumentContext content = JsonPath.parse(adyenNotificationBody.get("content"));
            Assertions.assertThat(cucumberTable.get(0).get("statusCode"))
                .withFailMessage("Status was not correct.")
                .isEqualTo(content.read("status.statusCode"));

            String message = cucumberTable.get(0).get("message");
            if (!message.equals("")) {
                Assertions
                    .assertThat(content.read("status.message.text").toString())
                    .contains(message);

            }
            log.info(content.toString());
        });
    }

    @And("^an AccountHolder will be created in Adyen with status Active$")
    public void anAccountHolderWillBeCreatedInAdyenWithStatusActive() throws Throwable {
        GetAccountHolderRequest accountHolderRequest = new GetAccountHolderRequest();
        accountHolderRequest.setAccountHolderCode(shop.getId());

        try {
            GetAccountHolderResponse accountHolderResponse = adyenAccountService.getAccountHolder(accountHolderRequest);
            Assertions.assertThat(accountHolderResponse.getAccountHolderStatus().getStatus().toString()).isEqualTo("Active");
        } catch (ApiException e) {
            log.error("Failing test due to exception", e);
            fail(e.getError().toString());
        }
    }

    @And("^a notification will be sent pertaining to (.*)$")
    public void aNotificationWillBeSentPertainingToACCOUNT_HOLDER_CREATED(String notification) {
        waitForNotification();
        await().untilAsserted(() -> {
            Map<String, Object> mappedAdyenNotificationResponse = retrieveAdyenNotificationBody(notification, shop.getId());
            Assertions.assertThat(mappedAdyenNotificationResponse).isNotNull();
            notificationResponse = JsonPath.parse(mappedAdyenNotificationResponse);
            Assertions.assertThat(notificationResponse.read("content.accountHolderCode").toString())
                .isEqualTo(shop.getId());
            Assertions.assertThat(notificationResponse.read("eventType").toString())
                .isEqualTo(notification);
        });
    }

    @Then("^no account holder is created in Adyen$")
    public void noAccountHolderIsCreatedInAdyen() {
        await().pollDelay(Duration.TEN_SECONDS).untilAsserted(() -> {
            Map mapResult = restAssuredAdyenApi.getAdyenNotificationBody(startUpCucumberHook
                .getBaseRequestBinUrlPath(), shop.getId(), "ACCOUNT_HOLDER_CREATED", null);
            Assertions.assertThat(mapResult == null);
        });
    }

    @And("^the shop data is correctly mapped to the Adyen Account$")
    public void theShopDataIsCorrectlyMappedToTheAdyenAccount() throws Exception {
        GetAccountHolderResponse response = retrieveAccountHolderResponse(shop.getId());
        ImmutableList<String> adyen = assertionHelper.adyenIndividualAccountDataBuilder(response).build();
        ImmutableList<String> mirakl = assertionHelper.miraklShopDataBuilder(shop.getContactInformation().getEmail(), shop).build();
        Assertions.assertThat(adyen).containsAll(mirakl);
    }

    @And("^the account holder is created in Adyen with status Active$")
    public void theAccountHolderIsCreatedInAdyenWithStatusActive() {
        Assertions.assertThat(notificationResponse.read("content.accountHolderStatus.status").toString())
            .isEqualTo("Active");
    }

    @And("^the shop data is correctly mapped to the Adyen Business Account$")
    public void theShopDataIsCorrectlyMappedToTheAdyenBusinessAccount(DataTable table) {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);

        ImmutableList<String> adyen = assertionHelper.adyenShareHolderAccountDataBuilder(notificationResponse).build();
        ImmutableList<String> mirakl = assertionHelper.miraklShopShareHolderDataBuilder(shop, cucumberTable).build();
        Assertions.assertThat(adyen).containsAll(mirakl);
    }

    @When("^the Mirakl Shop Details have been updated$")
    public void theMiraklShopDetailsHaveBeenUpdated(DataTable table) {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);
        cucumberTable.forEach(row ->
            shop = miraklUpdateShopApi.updateExistingShopsContactInfoWithTableData(shop, shop.getId(), miraklMarketplacePlatformOperatorApiClient, row)
        );
    }

    @When("^the Mirakl Shop Details have been changed")
    public void theMiraklShopDetailsHaveBeenchanged() {
        shop = miraklUpdateShopApi.updateExistingShopAddressFirstLine(shop, shop.getId(), miraklMarketplacePlatformOperatorApiClient);
    }

    @Test(enabled = false)
    @Given("^a shop exists in Mirakl$")
    public void updateShopExistsInMirakl(DataTable table) {
        List<Map<Object, Object>> rows = table.getTableConverter().toMaps(table, String.class, String.class);

        String seller = shopConfiguration.shopIds.get(rows.get(0).get("seller").toString()).toString();
        shop = getMiraklShop(miraklMarketplacePlatformOperatorApiClient, seller);
    }

    @Then("^adyen will send the (.*) notification$")
    public void adyenWillSendTheACCOUNT_HOLDER_PAYOUTNotification(String notification, DataTable table) {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);
        waitForNotification();
        await().untilAsserted(() -> {
            Map<String, Object> adyenNotificationBody = retrieveAdyenNotificationBody(notification, accountHolderCode);
            DocumentContext content = JsonPath.parse(adyenNotificationBody.get("content"));
            cucumberTable.forEach(row -> {

                Assertions.assertThat(row.get("statusCode"))
                    .isEqualTo(content.read("status.statusCode"));

                Assertions.assertThat(row.get("currency"))
                    .isEqualTo(content.read("amounts[0].Amount.currency"));

                Assertions.assertThat(row.get("amount"))
                    .isEqualTo(Double.toString(content.read("amounts[0].Amount.value")));

                Assertions.assertThat(row.get("iban"))
                    .isEqualTo(content.read("bankAccountDetail.iban"));
            });
        });
    }

    @When("^the Mirakl Shop Details have been updated with invalid data$")
    public void theMiraklShopDetailsHaveBeenUpdatedWithInvalidData(DataTable table) {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);
        shop = miraklUpdateShopApi.updateUboDataWithInvalidData(shop, shop.getId(), miraklMarketplacePlatformOperatorApiClient, cucumberTable);
    }

    @When("^the accountHolders balance is increased$")
    public void theAccountHoldersBalanceIsIncreased(DataTable table) throws Throwable {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);
        accountHolderCode = shop.getId();
        transferAccountHolderBalance(cucumberTable, shop);
    }

    @When("^the accountHolders balance is increased beyond the tier level$")
    public void theAccountHoldersBalanceIsIncreasedBeyondTheTierLevel(DataTable table) throws Throwable {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);
        accountHolderCode = shop.getId();
        transferAccountHolderBalanceBeyondTier(cucumberTable, shop);
    }

    @And("^the failed payout record is removed from the Connector database$")
    public void theFailedPayoutRecordIsRemovedFromTheConnectorDatabase() throws Throwable {
        List<AdyenPayoutError> byAccountHolderCode = adyenPayoutErrorRepository.findByAccountHolderCode(this.accountHolderCode);
        Assertions
            .assertThat(byAccountHolderCode)
            .isEmpty();
    }

    @When("^the seller uploads a document in Mirakl$")
    public void theSellerUploadsADocumentInMirakl(DataTable table) {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);
        miraklUpdateShopApi.uploadIdentityDocumentToExistingShop(shop.getId(), miraklMarketplacePlatformOperatorApiClient, cucumberTable);
    }

    @And("^sets the photoIdType in Mirakl$")
    public void setsThePhotoidtypeToPASSPORT(DataTable table) {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);
        miraklUpdateShopApi.updateShopWithPhotoIdForShareHolder(shop, shop.getId(), miraklMarketplacePlatformOperatorApiClient, cucumberTable);
    }

    @Then("^the documents are successfully uploaded to Adyen$")
    public void theDocumentsAreSuccessfullyUploadedToAdyen(DataTable table) throws Exception {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);

        GetUploadedDocumentsRequest getUploadedDocumentsRequest = new GetUploadedDocumentsRequest();
        getUploadedDocumentsRequest.setAccountHolderCode(shop.getId());
        uploadedDocuments = adyenAccountService.getUploadedDocuments(getUploadedDocumentsRequest);

        ArrayList<DocumentDetail> documentDetails = new ArrayList<>(uploadedDocuments.getDocumentDetails());

        for (Map<String, String> stringStringMap : cucumberTable) {
            String documentType = stringStringMap.get("documentType");
            String filename = stringStringMap.get("filename");
            boolean fileMatch = documentDetails.stream()
                .anyMatch(detail ->
                    documentType.equals(DocumentDetail.DocumentTypeEnum.valueOf(documentType).toString())
                        && detail.getFilename().equals(filename));
            Assertions
                .assertThat(fileMatch)
                .withFailMessage(String.format("Document upload response:[%s]", JsonPath.parse(uploadedDocuments).toString()))
                .isTrue();
        }
    }

    @And("^the following document will not be uploaded to Adyen$")
    public void theFollowingDocumentWillNotBeUploadedToAdyen(DataTable table) {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);

        cucumberTable.forEach(row -> {
            String documentType = row.get("documentType");
            String filename = row.get("filename");

            boolean documentTypeAndFilenameMatch = uploadedDocuments.getDocumentDetails().stream()
                .anyMatch(doc ->
                    DocumentDetail.DocumentTypeEnum.valueOf(documentType).equals(doc.getDocumentType())
                        && filename.equals(doc.getFilename())
                );
            Assertions
                .assertThat(documentTypeAndFilenameMatch)
                .withFailMessage(String.format("Document upload response:[%s]", JsonPath.parse(uploadedDocuments).toString()))
                .isFalse();
        });
    }

    @Then("^the updated documents are successfully uploaded to Adyen$")
    public void theUpdatedDocumentsAreSuccessfullyUploadedToAdyen(DataTable table) throws Exception {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);
        for (Map<String, String> row : cucumberTable) {
            String documentType = row.get("documentType");
            String filename = row.get("filename");
            GetUploadedDocumentsRequest getUploadedDocumentsRequest = new GetUploadedDocumentsRequest();
            getUploadedDocumentsRequest.setAccountHolderCode(shop.getId());
            uploadedDocuments = adyenAccountService.getUploadedDocuments(getUploadedDocumentsRequest);

            List<DocumentDetail> documents = uploadedDocuments.getDocumentDetails().stream()
                .filter(doc ->
                    DocumentDetail.DocumentTypeEnum.valueOf(documentType).equals(doc.getDocumentType())
                        && filename.equals(doc.getFilename()))
                .collect(Collectors.toList());

            Assertions
                .assertThat(documents)
                .hasSize(2);
        }
    }

    @And("^a passport has been uploaded to Adyen$")
    public void aPassportHasBeenUploadedToAdyen() throws Throwable {
        uploadPassportToAdyen(this.shop);
    }

    @And("^a notification will be sent in relation to the balance change$")
    public void aNotficationWillBeSentInRelationToTheBalanceChange(DataTable table) {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);
        String eventType = cucumberTable.get(0).get("eventType");
        String reason = cucumberTable.get(0).get("reason");
        String allowPayout = cucumberTable.get(0).get("previousPayoutState");

        waitForNotification();
        await().untilAsserted(() -> {
            notifications = restAssuredAdyenApi
                .getMultipleAdyenNotificationBodies(startUpTestingHook.getBaseRequestBinUrlPath(), shop.getId(), eventType, null);

            Assertions.assertThat(notifications).isNotEmpty();
            boolean foundReason = notifications.stream()
                .anyMatch(notification -> notification.read("content.reason").toString().contains(reason) &&
                notification.read("content.oldStatus.payoutState.allowPayout").equals(allowPayout));
            Assertions.assertThat(foundReason).isTrue();

            for (DocumentContext notification : notifications) {
                String notificationReason = notification.read("content.reason").toString();
                if (notificationReason.contains(reason) &&
                    notification.read("content.oldStatus.payoutState.allowPayout").equals(allowPayout)) {
                    adyenNotificationBody = notification;
                    break;
                }
            }
            cucumberMap.clear();
            cucumberMap.put("notification", adyenNotificationBody);
        });
    }

    @And("^the previous BankAccountDetail will be removed$")
    public void thePreviousBankAccountDetailWillBeRemoved(DataTable table) {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);
        String eventType = cucumberTable.get(0).get("eventType");
        String reason = cucumberTable.get(0).get("reason");

        waitForNotification();
        await().untilAsserted(() -> {
            notifications = restAssuredAdyenApi
                .getMultipleAdyenNotificationBodies(startUpTestingHook.getBaseRequestBinUrlPath(), shop.getId(), eventType, null);

            Assertions.assertThat(notifications).isNotEmpty();
            boolean foundReason = notifications.stream()
                .anyMatch(notification -> notification.read("content.reason").toString().contains(reason));
            Assertions.assertThat(foundReason).isTrue();

            for (DocumentContext notification : notifications) {
                String notificationReason = notification.read("content.reason").toString();
                if (notificationReason.contains(reason)) {
                    Assertions.assertThat(notificationReason.contains(reason));
                    break;
                }
            }
        });
    }


    @When("^the PayoutState allowPayout changes from false to true$")
    public void thePayoutStateAllowPayoutChangesFromFalseToTrue() {
        await().untilAsserted(() -> {
            GetAccountHolderResponse account = getGetAccountHolderResponse(this.shop);
            Boolean allowPayout = account.getAccountHolderStatus().getPayoutState().getAllowPayout();
            Assertions
                .assertThat(allowPayout)
                .isTrue();
            log.info(String.format("Payout status is [%s]", allowPayout.toString()));
        });
    }

    @When("^the PayoutState allowPayout changes from true to false$")
    public void thePayoutStateAllowPayoutChangesFromTrueToFalse() throws Throwable {
        await().untilAsserted(() -> {
            GetAccountHolderResponse account = getGetAccountHolderResponse(this.shop);
            Boolean allowPayout = account.getAccountHolderStatus().getPayoutState().getAllowPayout();
            Assertions
                .assertThat(allowPayout)
                .isFalse();
            log.info(String.format("Payout status is [%s]", allowPayout.toString()));
        });
    }

    @Then("^a remedial email will be sent for each ubo$")
    public void aRemedialEmailWillBeSentForEachUbo(String title) throws Throwable {
        GetAccountHolderResponse accountHolder = retrieveAccountHolderResponse(this.shop.getId());

        List<String> uboEmails = accountHolder.getAccountHolderDetails().getBusinessDetails().getShareholders().stream()
            .map(ShareholderContact::getEmail)
            .collect(Collectors.toList());

        await().untilAsserted(() -> {
                ResponseBody responseBody = RestAssured.get(mailTrapConfiguration.mailTrapEndPoint()).thenReturn().body();
                List<Map<String, Object>> emailLists = responseBody.jsonPath().get();

                List<String> htmlBody = new LinkedList<>();

                Assertions.assertThat(emailLists.size()).isGreaterThan(0);

                boolean foundEmail = emailLists.stream()
                    .anyMatch(map -> map.get("to_email").equals(uboEmails.iterator().next()));

                Assertions.assertThat(foundEmail).isTrue();

                for (String uboEmail : uboEmails) {
                    emailLists.stream()
                        .filter(map -> map.get("to_email").equals(uboEmail))
                        .findAny()
                        .ifPresent(map -> htmlBody.add(map.get("html_body").toString()));
                }

                Assertions.assertThat(htmlBody).isNotEmpty();
                Assertions.assertThat(htmlBody).hasSize(uboEmails.size());

                for (String body : htmlBody) {
                    Document parsedBody = Jsoup.parse(body);
                    Assertions
                        .assertThat(parsedBody.body().text())
                        .contains(shop.getId());

                    Assertions.assertThat(parsedBody.title()).isEqualTo(title);
                }
            }
        );
    }

    @Then("^adyen will send multiple (.*) notifications with (.*) of status (.*)$")
    public void adyenWillSendMultipleACCOUNT_HOLDER_VERIFICATIONNotificationWithIDENTITY_VERIFICATIONOfStatusDATA_PROVIDED(
        String eventType, String verificationType, String verificationStatus, DataTable table) throws Throwable {
        List<Map<String, Integer>> cucumberTable = table.getTableConverter().toMaps(table, String.class, Integer.class);
        waitForNotification();

        // get shareholderCodes from Adyen
        GetAccountHolderResponse accountHolder = getGetAccountHolderResponse(shop);

        List<String> shareholderCodes = accountHolder.getAccountHolderDetails().getBusinessDetails().getShareholders().stream()
            .map(ShareholderContact::getShareholderCode)
            .collect(Collectors.toList());

        await().untilAsserted(() -> {
            // get all ACCOUNT_HOLDER_VERIFICATION notifications
            List<DocumentContext> notifications = restAssuredAdyenApi
                .getMultipleAdyenNotificationBodies
                    (startUpTestingHook.getBaseRequestBinUrlPath(), shop.getId(), eventType, verificationType);

            ImmutableList<DocumentContext> shareHolderNotifications = restAssuredAdyenApi
                .extractShareHolderNotifications(notifications, shareholderCodes);

            Assertions
                .assertThat(notifications)
                .withFailMessage("Notification is empty.")
                .isNotEmpty();
            Integer maxUbos = cucumberTable.get(0).get("maxUbos");
            Assertions.assertThat(notifications).hasSize(maxUbos);

            for (DocumentContext notification : notifications) {
                Assertions
                    .assertThat(notification.read("content.verificationStatus").toString())
                    .isEqualTo(verificationStatus);
            }
            cucumberMap.put("notifications", shareHolderNotifications);
        });
    }

    @Then("^(.*) notification will be sent by Adyen$")
    public void TRANSFER_FUNDSNotificationWillBeSentByAdyen(String eventType, String status) throws Throwable {
        waitForNotification();
        GetAccountHolderResponse response = getGetAccountHolderResponse(shop);
        String accountCode = response.getAccounts().stream()
            .map(Account::getAccountCode)
            .findAny()
            .orElse(null);

        await().untilAsserted(() -> {
            ImmutableList<DocumentContext> notificationBodies = restAssuredAdyenApi
                .getMultipleAdyenTransferNotifications(startUpCucumberHook.getBaseRequestBinUrlPath(), eventType, subscriptionTransferCode);

            Assertions.assertThat(notificationBodies).isNotEmpty();
            Assertions.assertThat(notificationBodies.size()).isGreaterThan(1);

            DocumentContext transferNotification = null;
            for (DocumentContext notification : notificationBodies) {
                transferNotification = restAssuredAdyenApi
                    .extractCorrectTransferNotification(notification, liableAccountCode, accountCode);
                if (transferNotification != null) {
                    break;
                }
            }
            Assertions.assertThat(transferNotification).isNotNull();
            Assertions
                .assertThat(transferNotification.read("content.status.statusCode").toString())
                .isEqualTo(status);
        });
    }

    @And("^the accountHolder receives balance$")
    public void theAccountHolderReceivesBalance(DataTable table) throws Throwable {
        List<Map<String, String>> cucumberTable = table.getTableConverter().toMaps(table, String.class, String.class);
        uploadPassportToAdyen(this.shop);
        transferAccountHolderBalance(cucumberTable, shop);
    }
}
