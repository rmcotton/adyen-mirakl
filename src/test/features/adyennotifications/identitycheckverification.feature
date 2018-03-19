@cucumber
Feature: Identity verification check

    @ADY-18
    Scenario: Mandatory shareholder data is passed to Adyen to perform KYC Identity Check
        Given a new shop has been created in Mirakl with UBO Data for a Business
            | maxUbos | lastName |
            | 4       | testData |
        And we process the data and push to Adyen
        Then adyen will send multiple ACCOUNT_HOLDER_CREATED notification with IDENTITY_VERIFICATION of status DATA_PROVIDED

    @ADY-18
    Scenario: Mandatory shareholder data is updated and sent to Adyen to re-perform KYC Identity Check
        Given a new shop has been created in Mirakl with UBO Data for a Business
            | maxUbos | lastName |
            | 4       | testData |
        And we process the data and push to Adyen
        When the shareholder data has been updated in Mirakl
            | UBO | firstName | lastName |
            | 1   | John      | Smith    |
            | 2   | Sarah     | Godwin   |
            | 3   | Alex      | Pincher  |
            | 4   | Faye      | Jarvis   |
        And we process the data and push to Adyen
        Then adyen will send the ACCOUNT_HOLDER_UPDATED notification with multiple IDENTITY_VERIFICATION of status DATA_PROVIDED
        And getAccountHolder will have the correct amount of shareholders and data in Adyen
            | maxUbos |
            | 4       |

    @ADY-18 @ADY-102 @bug
    Scenario: Share Holder mandatory information is not provided therefore Identity Check will return AWAITING_DATA
        Given a new Business shop has been created in Mirakl without mandatory Shareholder Information
            | maxUbos | lastName |
            | 4       | testData |
        And we process the data and push to Adyen
        Then adyen will send multiple ACCOUNT_HOLDER_CREATED notification with IDENTITY_VERIFICATION of status DATA_PROVIDED
