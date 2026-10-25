Feature: Transaction history
  As a Cashi user
  I want to see the payments I have sent
  So that I can keep track of my money

  Background:
    Given the user is signed in to Cashi

  Scenario: Past payments are listed newest first
    Given the backend has these payments:
      | id | amount | currency | recipientEmail        | description      | createdAt                   |
      | p1 | 42.75  | JOD      | billing@jepco.example | Electricity bill | 2026-10-20T10:15:00Z        |
      | p2 | 450    | JOD      | landlord@example.com  | Rent - October   | 2026-10-22T18:42:00Z        |
      | p3 | 120.00 | USD      | payments@academy.io   | Online course    | 2026-10-18T21:03:00.221845Z |
    When the user opens the transaction history
    Then the history shows 3 payments
    And the payments are listed in the order "p2, p1, p3"
    And payment "p2" shows "450.00" "JOD" to "landlord@example.com" described as "Rent - October"

  Scenario: No payments yet
    When the user opens the transaction history
    Then the transaction history is empty

  Scenario: Only the first page is loaded until the user scrolls
    Given the backend has 45 payments
    And the backend returns 20 payments per page
    When the user opens the transaction history
    Then the history shows 20 payments
    And the history was requested 1 time

  Scenario: Scrolling to the end loads the remaining pages
    Given the backend has 45 payments
    And the backend returns 20 payments per page
    When the user scrolls to the end of the history
    Then the history shows 45 payments
    And the history requested the pages "first, c20, c40"

  Scenario: A payment that was just sent appears at the top
    Given the backend has these payments:
      | id | amount | currency | recipientEmail       | description | createdAt            |
      | p1 | 25.00  | JOD      | billing@isp.example  | Internet    | 2026-10-10T11:20:00Z |
    And the user opens the transaction history
    When the user sends "30" "USD" to "ahmad@gmail.com" with description "Money transfer"
    Then the history shows 2 payments
    And the transaction history starts with a payment of "30.00" "USD" to "ahmad@gmail.com"

  Scenario: The history still loads after the refresh token was revoked
    Given the backend has 3 payments
    And the access token has expired
    And the refresh token has been revoked
    When the user opens the transaction history
    Then the history shows 3 payments
    And the user was signed in again automatically

  Scenario: The history cannot be loaded while offline
    Given the backend is unreachable
    When the user opens the transaction history
    Then loading the history fails with a network error
