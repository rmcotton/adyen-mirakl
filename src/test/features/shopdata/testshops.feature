@exclude
Feature: Create payout shops which will be KYC'ed for seller payout tests

#    Description:
#    this feature file is used to create the seller payout shops in Mirakl and AccountHolders in Adyen,
#    it is not to be used as part of testing or regression testing hence the @exclude tag.
#
#    Sellers:
#    PayoutShop01 will have a corresponding PayoutVoucher file in resources directory
#    PayoutShop02 will be used for failure scenario see ADY-9 failure scenario
#    UpdateShop01 will be used for the smoketest

    Scenario Outline: Create shops which are KYC for Payout
        Given a shop has been created in Mirakl for an Individual with mandatory KYC data
            | companyName   | city   | bank name   | iban   | bankOwnerName   | lastName   |
            | <companyName> | <city> | <bank name> | <iban> | <bankOwnerName> | <lastName> |
        When we process the data and push to Adyen
        Then the ACCOUNT_HOLDER_VERIFICATION notification is sent by Adyen comprising of BANK_ACCOUNT_VERIFICATION and DATA_PROVIDED
        Examples:
            | companyName  | city   | bank name | iban                   | bankOwnerName | lastName |
            | PayoutShop01 | PASSED | testBank  | GB26TEST40051512347366 | TestData      | TestData |
            | PayoutShop02 | PASSED | testBank  | GB26TEST40051512347366 | TestData      | TestData |
            | UpdateShop01 | PASSED | testBank  | GB26TEST40051512347366 | TestData      | TestData |
