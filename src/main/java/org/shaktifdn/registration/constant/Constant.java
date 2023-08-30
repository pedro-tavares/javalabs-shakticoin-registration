package org.shaktifdn.registration.constant;

@SuppressWarnings("ALL")
public class Constant {
	
	public static final String TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss.SSSSSS";
	public static final String COLON = ":";
	public static final String REV = "0.1";
	
	public static final String GMT = "GMT";
	public static final String ONBOARDSHAKTI = "onboardshakti";
	public static final String USERS = "users";
	public static final String PERSONAL = "PERSONAL";
	public static final int PAGINATIONCOUNT = 10;
	
	public static final String LONGITUDE_MAX = "180.0";
	public static final String LONGITUDE_MIN = "-180.0";
	public static final String LATITUDE_MAX = "90.0";
	public static final String LATITUDE_MIN = "-90.0";
	
	public static final String IPADDRESS_PATTERN = 
	        "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
	        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
	        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
	        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
	
	public static final String PASSOWORD_PATTERN ="^(?=.*[0-9])"
            + "(?=.*[a-z])(?=.*[A-Z])"
            + "(?=.*[*!@#$%\\.^&+=_)(])"
            + "(?=\\S+$).{8,20}$";

	public static final String COUNTRY_CODE_PATTERN = "^(\\+\\d{1,3})$";

	public static final String SECURITYQUESTION = "securityquestion";
	public static final String IAMSECUTIRY = "iamsecurity";
	public static final String BEARER = "Bearer ";

	public static final String DATE_FORMAT = "yyyy-MM-dd";

	public static final String MOBILE_NO_SERVICE = "NO_SERVICE";
	public static final String EMAIL_REGISTERATION_FLOW = "User_Email_Register_Flow";
	public static final String MOBILE_REGISTERATION_FLOW = "User_Mobile_Register_Flow";
}
