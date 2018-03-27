package com.adyen.mirakl.service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.adyen.mirakl.startup.MiraklStartupValidator;
import com.adyen.model.Address;
import com.adyen.model.Name;
import com.adyen.model.marketpay.AccountHolderDetails;
import com.adyen.model.marketpay.BankAccountDetail;
import com.adyen.model.marketpay.BusinessDetails;
import com.adyen.model.marketpay.CreateAccountHolderRequest;
import com.adyen.model.marketpay.CreateAccountHolderRequest.LegalEntityEnum;
import com.adyen.model.marketpay.CreateAccountHolderResponse;
import com.adyen.model.marketpay.DeleteBankAccountRequest;
import com.adyen.model.marketpay.DeleteBankAccountResponse;
import com.adyen.model.marketpay.ErrorFieldType;
import com.adyen.model.marketpay.GetAccountHolderRequest;
import com.adyen.model.marketpay.GetAccountHolderResponse;
import com.adyen.model.marketpay.IndividualDetails;
import com.adyen.model.marketpay.PersonalData;
import com.adyen.model.marketpay.UpdateAccountHolderRequest;
import com.adyen.model.marketpay.UpdateAccountHolderResponse;
import com.adyen.service.Account;
import com.adyen.service.exception.ApiException;
import com.mirakl.client.mmp.domain.additionalfield.MiraklAdditionalFieldType;
import com.mirakl.client.mmp.domain.common.MiraklAdditionalFieldValue;
import com.mirakl.client.mmp.domain.common.MiraklAdditionalFieldValue.MiraklValueListAdditionalFieldValue;
import com.mirakl.client.mmp.domain.shop.MiraklContactInformation;
import com.mirakl.client.mmp.domain.shop.MiraklShop;
import com.mirakl.client.mmp.domain.shop.MiraklShops;
import com.mirakl.client.mmp.domain.shop.bank.MiraklIbanBankAccountInformation;
import com.mirakl.client.mmp.operator.core.MiraklMarketplacePlatformOperatorApiClient;
import com.mirakl.client.mmp.request.shop.MiraklGetShopsRequest;

@Service
@Transactional
public class ShopService {

    private final Logger log = LoggerFactory.getLogger(ShopService.class);

    @Resource
    private MiraklMarketplacePlatformOperatorApiClient miraklMarketplacePlatformOperatorApiClient;

    @Resource
    private Account adyenAccountService;

    @Resource
    private DeltaService deltaService;

    @Resource
    private ShareholderMappingService shareholderMappingService;

    @Resource
    private UboService uboService;

    @Resource
    private InvalidFieldsNotificationService invalidFieldsNotificationService;

    private Pattern houseNumberPattern;

    @Value("${extract.house.number.regex}")
    private String houseNumberRegex;

    @PostConstruct
    public void postConstruct() {
        houseNumberPattern = Pattern.compile(houseNumberRegex);
    }

    public void processUpdatedShops() {
        final ZonedDateTime beforeProcessing = ZonedDateTime.now();

        List<MiraklShop> shops = getUpdatedShops();
        log.debug("Retrieved shops: {}", shops.size());
        for (MiraklShop shop : shops) {
            try {
                GetAccountHolderResponse getAccountHolderResponse = getAccountHolderFromShop(shop);
                if (getAccountHolderResponse != null) {
                    processUpdateAccountHolder(shop, getAccountHolderResponse);
                } else {
                    processCreateAccountHolder(shop);
                }
            } catch (ApiException e) {
                log.error("MarketPay Api Exception: {}", e.getError(), e);
            } catch (Exception e) {
                log.error("Exception: {}", e.getMessage(), e);
            }
        }

        deltaService.updateShopDelta(beforeProcessing);
    }

    private void processCreateAccountHolder(final MiraklShop shop) throws Exception {
        CreateAccountHolderRequest createAccountHolderRequest = createAccountHolderRequestFromShop(shop);
        CreateAccountHolderResponse response = adyenAccountService.createAccountHolder(createAccountHolderRequest);
        shareholderMappingService.updateShareholderMapping(response);
        log.debug("CreateAccountHolderResponse: {}", response);
        if (! CollectionUtils.isEmpty(response.getInvalidFields())) {
            final String invalidFields = response.getInvalidFields().stream().map(ErrorFieldType::toString).collect(Collectors.joining(","));
            log.warn("Invalid fields when trying to create shop {}: {}", shop.getId(), invalidFields);
            invalidFieldsNotificationService.handleErrorsInResponse(shop, response.getInvalidFields());
        }
    }

    private void processUpdateAccountHolder(final MiraklShop shop, final GetAccountHolderResponse getAccountHolderResponse) throws Exception {
        UpdateAccountHolderRequest updateAccountHolderRequest = updateAccountHolderRequestFromShop(shop, getAccountHolderResponse);

        UpdateAccountHolderResponse response = adyenAccountService.updateAccountHolder(updateAccountHolderRequest);
        shareholderMappingService.updateShareholderMapping(response);
        log.debug("UpdateAccountHolderResponse: {}", response);

        if (! CollectionUtils.isEmpty(response.getInvalidFields())) {
            final String invalidFields = response.getInvalidFields().stream().map(ErrorFieldType::toString).collect(Collectors.joining(","));
            log.warn("Invalid fields when trying to update shop {}: {}", shop.getId(), invalidFields);
            invalidFieldsNotificationService.handleErrorsInResponse(shop, response.getInvalidFields());
        }

        // if IBAN has changed remove the old one
        if (isIbanChanged(getAccountHolderResponse, shop)) {
            DeleteBankAccountResponse deleteBankAccountResponse = adyenAccountService.deleteBankAccount(deleteBankAccountRequest(getAccountHolderResponse));
            log.debug("DeleteBankAccountResponse: {}", deleteBankAccountResponse);
        }
    }


    /**
     * Construct DeleteBankAccountRequest to remove outdated iban bankaccounts
     */
    protected DeleteBankAccountRequest deleteBankAccountRequest(GetAccountHolderResponse getAccountHolderResponse) {
        DeleteBankAccountRequest deleteBankAccountRequest = new DeleteBankAccountRequest();
        deleteBankAccountRequest.accountHolderCode(getAccountHolderResponse.getAccountHolderCode());
        List<String> uuids = new ArrayList<>();
        for (BankAccountDetail bankAccountDetail : getAccountHolderResponse.getAccountHolderDetails().getBankAccountDetails()) {
            uuids.add(bankAccountDetail.getBankAccountUUID());
        }
        deleteBankAccountRequest.setBankAccountUUIDs(uuids);

        return deleteBankAccountRequest;
    }

    public List<MiraklShop> getUpdatedShops() {
        int offset = 0;
        Long totalCount = 1L;
        List<MiraklShop> shops = new ArrayList<>();

        while (offset < totalCount) {
            MiraklGetShopsRequest miraklGetShopsRequest = new MiraklGetShopsRequest();
            miraklGetShopsRequest.setOffset(offset);

            miraklGetShopsRequest.setUpdatedSince(deltaService.getShopDelta());
            log.debug("getShops request since: " + miraklGetShopsRequest.getUpdatedSince());
            MiraklShops miraklShops = miraklMarketplacePlatformOperatorApiClient.getShops(miraklGetShopsRequest);
            shops.addAll(miraklShops.getShops());

            totalCount = miraklShops.getTotalCount();
            offset += miraklShops.getShops().size();
        }

        return shops;
    }

    private CreateAccountHolderRequest createAccountHolderRequestFromShop(MiraklShop shop) {
        CreateAccountHolderRequest createAccountHolderRequest = new CreateAccountHolderRequest();

        // Set Account holder code
        createAccountHolderRequest.setAccountHolderCode(shop.getId());

        // Set LegalEntity
        LegalEntityEnum legalEntity = getLegalEntityFromShop(shop);
        createAccountHolderRequest.setLegalEntity(legalEntity);

        // Set AccountHolderDetails
        AccountHolderDetails accountHolderDetails = new AccountHolderDetails();
        accountHolderDetails.setBankAccountDetails(setBankAccountDetails(shop));

        updateDetailsFromShop(accountHolderDetails, shop, null);

        // Set email
        MiraklContactInformation contactInformation = getContactInformationFromShop(shop);
        accountHolderDetails.setEmail(contactInformation.getEmail());
        createAccountHolderRequest.setAccountHolderDetails(accountHolderDetails);

        return createAccountHolderRequest;
    }

    private LegalEntityEnum getLegalEntityFromShop(MiraklShop shop) {
        MiraklValueListAdditionalFieldValue additionalFieldValue = (MiraklValueListAdditionalFieldValue) shop.getAdditionalFieldValues()
                                                                                                             .stream()
                                                                                                             .filter(field -> isListWithCode(field,
                                                                                                                                             MiraklStartupValidator.CustomMiraklFields.ADYEN_LEGAL_ENTITY_TYPE))
                                                                                                             .findAny()
                                                                                                             .orElseThrow(() -> new RuntimeException("Legal entity not found"));

        LegalEntityEnum legalEntity = Arrays.stream(LegalEntityEnum.values())
                                            .filter(legalEntityEnum -> legalEntityEnum.toString().equalsIgnoreCase(additionalFieldValue.getValue()))
                                            .findAny()
                                            .orElseThrow(() -> new RuntimeException("Invalid legal entity: " + additionalFieldValue.toString()));

        return legalEntity;
    }

    private MiraklContactInformation getContactInformationFromShop(MiraklShop shop) {
        return Optional.of(shop.getContactInformation()).orElseThrow(() -> new RuntimeException("Contact information not found"));
    }

    private Address createAddressFromShop(MiraklShop shop) {
        MiraklContactInformation contactInformation = getContactInformationFromShop(shop);
        if (contactInformation != null && ! StringUtils.isEmpty(contactInformation.getCountry())) {
            Address address = new Address();
            address.setHouseNumberOrName(getHouseNumberFromStreet(contactInformation.getStreet1()));
            address.setPostalCode(contactInformation.getZipCode());
            address.setStreet(contactInformation.getStreet1());
            address.setCountry(getIso2CountryCodeFromIso3(contactInformation.getCountry()));
            address.setCity(contactInformation.getCity());
            return address;
        }
        return null;
    }

    private BusinessDetails addBusinessDetailsFromShop(final MiraklShop shop, final GetAccountHolderResponse existingAccountHolder) {
        BusinessDetails businessDetails = new BusinessDetails();

        if (shop.getProfessionalInformation() != null) {
            if (StringUtils.isNotEmpty(shop.getProfessionalInformation().getCorporateName())) {
                businessDetails.setLegalBusinessName(shop.getProfessionalInformation().getCorporateName());
            }
            if (StringUtils.isNotEmpty(shop.getProfessionalInformation().getTaxIdentificationNumber())) {
                businessDetails.setTaxId(shop.getProfessionalInformation().getTaxIdentificationNumber());
            }
        }
        businessDetails.setShareholders(uboService.extractUbos(shop, existingAccountHolder));
        return businessDetails;
    }

    private IndividualDetails createIndividualDetailsFromShop(MiraklShop shop) {
        IndividualDetails individualDetails = new IndividualDetails();

        shop.getAdditionalFieldValues()
            .stream()
            .filter(MiraklAdditionalFieldValue.MiraklAbstractAdditionalFieldWithSingleValue.class::isInstance)
            .map(MiraklAdditionalFieldValue.MiraklAbstractAdditionalFieldWithSingleValue.class::cast)
            .filter(fieldValue -> "adyen-individual-dob".equals(fieldValue.getCode()))
            .findAny()
            .ifPresent(value -> {
                PersonalData personalData = new PersonalData();
                personalData.setDateOfBirth(value.getValue());
                individualDetails.setPersonalData(personalData);
            });

        MiraklContactInformation contactInformation = getContactInformationFromShop(shop);

        Name name = new Name();
        name.setFirstName(contactInformation.getFirstname());
        name.setLastName(contactInformation.getLastname());
        name.setGender(UboService.CIVILITY_TO_GENDER.getOrDefault(contactInformation.getCivility().toUpperCase(), Name.GenderEnum.UNKNOWN));
        individualDetails.setName(name);
        return individualDetails;
    }

    private boolean isListWithCode(MiraklAdditionalFieldValue additionalFieldValue, MiraklStartupValidator.CustomMiraklFields field) {
        return MiraklAdditionalFieldType.LIST.equals(additionalFieldValue.getFieldType()) && field.toString().equalsIgnoreCase(additionalFieldValue.getCode());
    }

    /**
     * Check if AccountHolder already exists in Adyen
     */
    private GetAccountHolderResponse getAccountHolderFromShop(MiraklShop shop) throws Exception {
        // lookup accountHolder in Adyen
        GetAccountHolderRequest getAccountHolderRequest = new GetAccountHolderRequest();
        getAccountHolderRequest.setAccountHolderCode(shop.getId());

        try {
            GetAccountHolderResponse getAccountHolderResponse = adyenAccountService.getAccountHolder(getAccountHolderRequest);
            if (! getAccountHolderResponse.getAccountHolderCode().isEmpty()) {
                return getAccountHolderResponse;
            }
        } catch (ApiException e) {
            // account does not exists yet
            log.debug("MarketPay Api Exception: {}", e.getError());
        }

        return null;
    }

    /**
     * Construct updateAccountHolderRequest to Adyen from Mirakl shop
     */
    protected UpdateAccountHolderRequest updateAccountHolderRequestFromShop(MiraklShop shop, GetAccountHolderResponse existingAccountHolder) {

        UpdateAccountHolderRequest updateAccountHolderRequest = new UpdateAccountHolderRequest();
        updateAccountHolderRequest.setAccountHolderCode(shop.getId());

        if (shop.getPaymentInformation() instanceof MiraklIbanBankAccountInformation) {
            MiraklIbanBankAccountInformation miraklIbanBankAccountInformation = (MiraklIbanBankAccountInformation) shop.getPaymentInformation();
            if ((! miraklIbanBankAccountInformation.getIban().isEmpty() && shop.getCurrencyIsoCode() != null) &&
                // if IBAN already exists and is the same then ignore this
                (! isIbanIdentical(miraklIbanBankAccountInformation.getIban(), existingAccountHolder))) {
                // create AccountHolderDetails
                AccountHolderDetails accountHolderDetails = new AccountHolderDetails();
                accountHolderDetails.setBankAccountDetails(setBankAccountDetails(shop));
                updateAccountHolderRequest.setAccountHolderDetails(accountHolderDetails);

            }
        }

        final AccountHolderDetails accountHolderDetails = Optional.ofNullable(updateAccountHolderRequest.getAccountHolderDetails()).orElseGet(AccountHolderDetails::new);
        updateAccountHolderRequest.setAccountHolderDetails(accountHolderDetails);
        updateDetailsFromShop(accountHolderDetails, shop, existingAccountHolder);

        return updateAccountHolderRequest;
    }

    private AccountHolderDetails updateDetailsFromShop(AccountHolderDetails accountHolderDetails, MiraklShop shop, GetAccountHolderResponse existingAccountHolder) {
        LegalEntityEnum legalEntity = getLegalEntityFromShop(shop);

        if (LegalEntityEnum.INDIVIDUAL == legalEntity) {
            IndividualDetails individualDetails = createIndividualDetailsFromShop(shop);
            accountHolderDetails.setIndividualDetails(individualDetails);
        } else if (LegalEntityEnum.BUSINESS == legalEntity) {
            BusinessDetails businessDetails = addBusinessDetailsFromShop(shop, existingAccountHolder);
            accountHolderDetails.setBusinessDetails(businessDetails);
        } else {
            throw new IllegalArgumentException(legalEntity.toString() + " not supported");
        }

        accountHolderDetails.setAddress(createAddressFromShop(shop));

        return accountHolderDetails;
    }

    /**
     * Check if IBAN is changed
     */
    protected boolean isIbanChanged(GetAccountHolderResponse getAccountHolderResponse, MiraklShop shop) {

        if (shop.getPaymentInformation() instanceof MiraklIbanBankAccountInformation) {
            MiraklIbanBankAccountInformation miraklIbanBankAccountInformation = (MiraklIbanBankAccountInformation) shop.getPaymentInformation();
            if (! miraklIbanBankAccountInformation.getIban().isEmpty()) {
                if (getAccountHolderResponse.getAccountHolderDetails() != null && ! getAccountHolderResponse.getAccountHolderDetails().getBankAccountDetails().isEmpty()) {
                    if (! miraklIbanBankAccountInformation.getIban().equals(getAccountHolderResponse.getAccountHolderDetails().getBankAccountDetails().get(0).getIban())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check if IBAN is the same as on Adyen side
     */
    protected boolean isIbanIdentical(String iban, GetAccountHolderResponse getAccountHolderResponse) {

        if (getAccountHolderResponse.getAccountHolderDetails() != null && ! getAccountHolderResponse.getAccountHolderDetails().getBankAccountDetails().isEmpty()) {
            if (iban.equals(getAccountHolderResponse.getAccountHolderDetails().getBankAccountDetails().get(0).getIban())) {
                return true;
            }
        }
        return false;
    }


    /**
     * Set bank account details
     */
    private List<BankAccountDetail> setBankAccountDetails(MiraklShop shop) {
        BankAccountDetail bankAccountDetail = createBankAccountDetail(shop);
        List<BankAccountDetail> bankAccountDetails = new ArrayList<>();
        bankAccountDetails.add(bankAccountDetail);
        return bankAccountDetails;
    }

    private BankAccountDetail createBankAccountDetail(MiraklShop shop) {
        if (! (shop.getPaymentInformation() instanceof MiraklIbanBankAccountInformation)) {
            log.debug("No IBAN bank account details, not creating bank account detail");
            return null;
        }
        MiraklIbanBankAccountInformation miraklIbanBankAccountInformation = (MiraklIbanBankAccountInformation) shop.getPaymentInformation();

        // create AcountHolderDetails
        AccountHolderDetails accountHolderDetails = new AccountHolderDetails();

        // set BankAccountDetails
        BankAccountDetail bankAccountDetail = new BankAccountDetail();

        // check if PaymentInformation is object MiraklIbanBankAccountInformation
        miraklIbanBankAccountInformation.getIban();
        bankAccountDetail.setIban(miraklIbanBankAccountInformation.getIban());
        bankAccountDetail.setBankCity(miraklIbanBankAccountInformation.getBankCity());
        bankAccountDetail.setBankBicSwift(miraklIbanBankAccountInformation.getBic());
        bankAccountDetail.setCountryCode(getBankCountryFromIban(miraklIbanBankAccountInformation.getIban())); // required field
        bankAccountDetail.setCurrencyCode(shop.getCurrencyIsoCode().toString());


        if (shop.getContactInformation() != null) {
            bankAccountDetail.setOwnerPostalCode(shop.getContactInformation().getZipCode());
            bankAccountDetail.setOwnerHouseNumberOrName(getHouseNumberFromStreet(shop.getContactInformation().getStreet1()));
            bankAccountDetail.setOwnerName(shop.getPaymentInformation().getOwner());
        }

        bankAccountDetail.setPrimaryAccount(true);

        List<BankAccountDetail> bankAccountDetails = new ArrayList<>();
        bankAccountDetails.add(bankAccountDetail);
        accountHolderDetails.setBankAccountDetails(bankAccountDetails);

        return bankAccountDetail;
    }


    /**
     * First two digits of IBAN holds ISO country code
     */
    private String getBankCountryFromIban(String iban) {
        return iban.substring(0, 2);
    }


    /**
     * Finds a number in the string street starting at the end of the string e.g.
     * 1 street name house 5
     * returns 5
     */
    private String getHouseNumberFromStreet(String street) {
        Matcher matcher = houseNumberPattern.matcher(street);
        if (matcher.find()) {
            return matcher.group(1);
        }else{
            log.warn("Unable to retrieve house number from street: {}", street);
            return null;
        }
    }

    /**
     * Get ISO-2 Country Code from ISO-3 Country Code
     */
    protected String getIso2CountryCodeFromIso3(String iso3) {
        if (! iso3.isEmpty()) {
            return countryCodes().get(iso3);
        }
        return null;
    }

    /**
     * Do this on application start-up
     */
    public Map<String, String> countryCodes() {

        Map<String, String> countryCodes = new HashMap<>();
        String[] isoCountries = Locale.getISOCountries();

        for (String country : isoCountries) {
            Locale locale = new Locale("", country);
            countryCodes.put(locale.getISO3Country(), locale.getCountry());
        }
        return countryCodes;
    }

    public void setHouseNumberPattern(final Pattern houseNumberPattern) {
        this.houseNumberPattern = houseNumberPattern;
    }
}
