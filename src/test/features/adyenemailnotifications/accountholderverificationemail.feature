Feature: Account Holder Created verification emails

    @ADY-42
    Scenario: Invalid data is submitted when creating a Mirakl Shop
        Given a shop exists in Mirakl
            | seller       |
            | UpdateShop02 |
        When the Mirakl Shop Details have been updated with invalid data
            | UBO |
            | 1   |
        And the connector processes the data and pushes to Adyen
        Then an email will be sent to the seller
        """
        Account verification
        """
