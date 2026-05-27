package com.ssafy.seveniTax.ui.navigation

sealed class Route(val path: String) {
    object Splash : Route("splash")
    object Main : Route("main")
    object Home : Route("home")
    object Dashboard : Route("dashboard")
    object IdentityVerify : Route("identity_verify")
    object SmsAuth : Route("sms_auth")
    object PinSetup : Route("pin_setup")
    object PinConfirm : Route("pin_confirm")
    object AuthSuccess : Route("auth_success")
    object PinLogin : Route("pin_login")

    object PayIntro : Route("pay_intro")
    object PayTerms : Route("pay_terms")
    object PayVerify : Route("pay_verify")
    object PayBusinessInfo : Route("pay_business_info")
    object PayConfirm : Route("pay_confirm")
    object PayComplete : Route("pay_complete")
    object QrPayment : Route("qr_payment")
    object PaymentProcessing : Route("payment_processing")
    object PaymentComplete : Route("payment_complete")

    object CardList : Route("card_list")
    object CardTypeSelect : Route("card_type_select")
    object CardInput : Route("card_input/{cardType}") {
        fun create(cardType: String) = "card_input/$cardType"
    }
    object CardAccountSelect : Route("card_account_select")
    object CardProductSelect : Route("card_product_select")
    object CardBusinessInfo : Route("card_business_info")
    object CardOwnerVerify : Route("card_owner_verify")
    object CardSms : Route("card_sms")
    object CardComplete : Route("card_complete")
    object CardChange : Route("card_change")
    object CardDetail : Route("card_detail/{cardId}") {
        fun create(cardId: String) = "card_detail/$cardId"
    }

    object ServerTest : Route("server_test")
    object NotificationSettings : Route("notification_settings")

    object BookEntryList : Route("book_entry_list")
    object BookEntryDetail : Route("book_entry_detail/{entryId}") {
        fun create(entryId: Long) = "book_entry_detail/$entryId"
    }
    object ExportPurpose : Route("export_purpose")
    object ExportDateRange : Route("export_date_range/{purpose}") {
        fun create(purpose: String) = "export_date_range/$purpose"
    }
    object BookFilter : Route("book_filter")
    object BookMemoAdd : Route("book_memo_add?entryId={entryId}&fromPayment={fromPayment}") {
        fun create(entryId: Long = -1, fromPayment: Boolean = false) =
            "book_memo_add?entryId=$entryId&fromPayment=$fromPayment"
    }
    object TaxReport : Route("tax_report")
    object TaxSavingsDetail : Route("tax_savings_detail")

    object ExportFormat : Route("export_format/{purpose}/{startDate}/{endDate}") {
        fun create(purpose: String, startDate: String, endDate: String) =
            "export_format/$purpose/$startDate/$endDate"
    }

    object TaxCalendar : Route("tax_calendar")
    object TaxCalendarDetail : Route("tax_calendar_detail/{taxName}/{deadline}/{dDay}/{description}") {
        fun create(taxName: String, deadline: String, dDay: Int, description: String): String {
            return "tax_calendar_detail/${java.net.URLEncoder.encode(taxName, "UTF-8")}/${deadline}/${dDay}/${java.net.URLEncoder.encode(description, "UTF-8")}"
        }
    }

    object ClassificationLoading : Route("classification_loading")
    object ClassificationResult : Route("classification_result")
    object CategorySelect : Route("category_select?returnTo={returnTo}&entryId={entryId}") {
        fun create(returnTo: String = "", entryId: Long = -1) = "category_select?returnTo=$returnTo&entryId=$entryId"
    }
    object MemoAdd : Route("memo_add")
    object ClassificationComplete : Route("classification_complete?returnTo={returnTo}") {
        fun create(returnTo: String = "") = "classification_complete?returnTo=$returnTo"
    }
    object UnclassifiedList : Route("unclassified_list")
    object BulkClassificationLoading : Route("bulk_classification_loading")
    object AutoClassification : Route("auto_classification")
    object AiChat : Route("ai_chat")
}
