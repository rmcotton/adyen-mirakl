@cucumber
Feature: Bank Account Verification

    @ADY-13 @ADY-77 @ADY-102
    Scenario: ACCOUNT_HOLDER_VERIFICATION notification is received after seller provides Bank Account Details
        Given a shop has been created in Mirakl for an Individual with Bank Information
            | city   | bank name | iban                   | bankOwnerName | lastName |
            | PASSED | testBank  | GB26TEST40051512347366 | TestData      | TestData |
        When we process the data and push to Adyen
        Then the ACCOUNT_HOLDER_VERIFICATION notification is sent by Adyen comprising of BANK_ACCOUNT_VERIFICATION and DATA_PROVIDED

    @ADY-8 @ADY-77 @ADY-84 @ADY-102
    Scenario: New BankAccountDetail is created for Account Holder upon new Bank Account entry in Mirakl
        Given a shop has been created in Mirakl for an Individual with Bank Information
            | city   | bank name | iban                   | bankOwnerName | lastName |
            | PASSED | RBS       | GB26TEST40051512347366 | TestData      | TestData |
        When we process the data and push to Adyen
        Then a new bankAccountDetail will be created for the existing Account Holder
            | eventType              | iban                   |
            | ACCOUNT_HOLDER_CREATED | GB26TEST40051512347366 |

    @ADY-8 @ADY-71 @ADY-84 @ADY-104
    Scenario: New BankAccountDetail is created for Account Holder upon new IBAN entry in Mirakl for an existing Adyen accountHolder
        Given a new shop has been created in Mirakl for an Individual
            | lastName |
            | TestData |
        And we process the data and push to Adyen
        And a new IBAN has been provided by the seller in Mirakl and the mandatory IBAN fields have been provided
        When we process the data and push to Adyen
        Then a new bankAccountDetail will be created for the existing Account Holder
            | eventType              |
            | ACCOUNT_HOLDER_UPDATED |

    @ADY-8 @ADY-71 @ADY-84 @ADY-100
    Scenario: Editing IBAN in Mirakl will create new BankAccountDetail in Adyen
        Given a shop has been created in Mirakl for an Individual with Bank Information
            | city   | bank name | iban                   | bankOwnerName | lastName |
            | PASSED | RBS       | GB26TEST40051512347366 | TestData      | TestData |
        And we process the data and push to Adyen
        When the IBAN has been modified in Mirakl
            | iban                   |
            | GB26TEST40051512393150 |
        And we process the data and push to Adyen
        Then a new bankAccountDetail will be created for the existing Account Holder
            | eventType              | iban                   |
            | ACCOUNT_HOLDER_UPDATED | GB26TEST40051512393150 |
        And the previous BankAccountDetail will be removed
            | eventType                    | reason                |
            | ACCOUNT_HOLDER_STATUS_CHANGE | Bank account deletion |

    @ADY-15 @ADY-89
    Scenario: Seller uploads Bank Statement Mirakl to fulfil BANK_ACCOUNT_VERIFICATION in Adyen
        Given a shop has been created in Mirakl for an Individual with Bank Information
            | city   | bank name | iban                   | bankOwnerName | lastName |
            | PASSED | testBank  | GB26TEST40051512347366 | TestData      | TestData |
        And we process the data and push to Adyen
        When the seller uploads a Bank Statement in Mirakl
        And we process the document data and push to Adyen
        And the document is successfully uploaded to Adyen
            | documentType   | filename          |
            | BANK_STATEMENT | BankStatement.jpg |
