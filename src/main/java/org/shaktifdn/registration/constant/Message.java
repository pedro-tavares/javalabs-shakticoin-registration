package org.shaktifdn.registration.constant;

public class Message {

    private Message() {
    }

    public static final String EMAIL_NOT_VERIFIED = "Email is not verified.";
    public static final String EMAIL_NOT_FOUND = "Email is not found.";
    public static final String EMAIL_EXPIRED = "Email authentication duration is expired.";
    public static final String EMAIL_ALREADY_REGISTERED = "Email is already registered : {0}";

    public static final String MOBILE_NOT_VERIFIED = "Mobile is not verified.";
    public static final String MOBILE_NOT_FOUND = "Mobile is not found.";
    public static final String MOBILE_EXPIRED = "Mobile authentication duration is expired.";

    public static final String CHECK_EMAIL_STATUS_SERVICE_ERROR = "Check email status service error, Please contact administrator.";
    public static final String CHECK_MOBILE_STATUS_SERVICE_ERROR = "Check mobile status service error, Please contact administrator.";
    public static final String UNAUTHORIZED = "Unauthorized user";

    public static final String GREATER_INVALID_LONGITUDE = "More than 180 is invalid for longitude";
    public static final String LESSER_INVALID_LONGITUDE = "Less than -180 is invalid for longitude";

    public static final String GREATER_INVALID_LATITUDE = "More than 90 is invalid for latitude";
    public static final String LESSER_INVALID_LATITUDE = "Less than -90 is invalid for latitude";
    public static final String INVALID_IP = "IPAddress number is invalid";
    public static final String INVALID_PASSWORD = "Password value is invalid, please follow password rules";
    public static final String INVALID_MOBILE_NUMBER = "Mobile number is invalid, please follow mobile number rules";

    public static final String PASSWORD_MANDATORY = "Please insert password, it is a mandatory field";
    public static final String COUNTRYCODE_MANDATORY = "Please insert countryCode, it is a mandatory field";
    public static final String EMAIL_MANDATORY = "Please insert email, it is a mandatory field";
    public static final String DEVICE_ID_MANDATORY = "Device ID a mandatory field";
    public static final String INVALID_EMAIL = "Please provide a valid email";
    public static final String QUESTIONID_MANDATORY = "Please insert questionID, it is a mandatory field";
    public static final String ANSWER_MANDATORY = "Please insert answer, it is a mandatory field";
    public static final String PASSPHRASE_MANDATORY = "Please insert passphrase, it is a mandatory field";
    public static final String NEWPASSOWRD_MANDATORY = "Please insert newPassword, it is a mandatory field";
    public static final String CURRENTPASSOWRD_MANDATORY = "Please insert currentPassword, it is a mandatory field";
    public static final String USERNAME_MANDATORY = "Please insert userName, it is a mandatory field";
    public static final String LIST_QUESTION_ANSWER_SIZE = "Invalid request, please provide minimum 5 and maximum 10 Security Question";
    public static final String VERIFY_QUESTION_ANSWER_SIZE = "Please answer all the questions";
    public static final String INVALID_QUESTIONANSWERPAIR_SIZE = "Can not insert more than 10 question and answer pair at a time.";


    public static final String VERIFIED_QUA_ANS_STATUS_ERROR = "You inserted incorrect answers or questionID, Please recheck answers";
    public static final String VERIFIED_QUA_ANS_STATUS_SUCCESS = "All question and answers are correct.";
    public static final String INVALID_COUNTRY_CODE_NUMBER = "Country code is invalid, please follow country code rules";

    public static final String PIN_MANDATORY = "Please insert PIN, it is a mandatory field";
    public static final String INVALID_USER = "User details not found.";
    public static final String INVALID_PIN = "Please enter a valid PIN";
    public static final String VERIFIED_PIN_SUCCESS = "PIN verified successfully";
    public static final String VERIFIED_PIN_ERROR = "Incorrect PIN. Please insert the valid ";
    public static final String PIN_UPDATE_SUCCESS = "PIN has been updated successfully";
    public static final String EMAIL_VERIFICATION_SENT_SUCCESS = "Verification link has been sent to your email";
    public static final String EMAIL_OTP_SENT_SUCCESS = "OTP has been sent to your email";
    public static final String EMAIL_OTP_SENT_FAILED = "Error occurred while sending OTP";
    public static final String EMAIL_VERIFIED_SUCCESS = "Your email has been verified successfully";
    public static final String INVALID_VERFIY_QUESTIONANSWER_SIZE = "Provide the answers to 3 questions";

    public static final String MOBILE_OTP_SENT_SUCCESS = "OTP has been sent successfully on your primary Mobile Number";
    public static final String MOBILE_OTP_SENT_FAILED = "Error occurred while sending OTP to Mobile";
    public static final String MOBILE_OTP_VERIFIED_SUCCESS = "OTP has verified successfully";
    public static final String MOBILE_OTP_VERIFIED_FAILED = "OTP has not been verified successfully";
    public static final String MOBILE_LOGIN_FLOW_EMAIL_NOT_VERIFIED = "email is not verified";


}
