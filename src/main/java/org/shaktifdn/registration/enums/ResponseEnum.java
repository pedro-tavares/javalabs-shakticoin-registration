package org.shaktifdn.registration.enums;

import org.springframework.http.HttpStatus;

public enum ResponseEnum {
	
	OK(200,HttpStatus.OK.name()),
	CREATED(201,HttpStatus.CREATED.name()),
	UPDATED(202,"UPDATED"),
	DELETED(203,"DELETED"),
	NO_CONTENT(204,HttpStatus.NO_CONTENT.name()),
	BAD_REQUEST(400,HttpStatus.BAD_REQUEST.name()),	
	UNAUTHORIZED(401,HttpStatus.UNAUTHORIZED.name()),
	FORBIDDEN(403,HttpStatus.FORBIDDEN.name()),
	METHODNOTALLOWED(405,HttpStatus.METHOD_NOT_ALLOWED.name()),
	NOTFOUND(404,HttpStatus.NOT_FOUND.name()),
	CONFLICT(409,HttpStatus.CONFLICT.name()),
	GONE(410,HttpStatus.GONE.name()),
	INTERNALSERVERERROR(500,HttpStatus.INTERNAL_SERVER_ERROR.name());
	

	private final Integer statusCode;
    private final String statusMessage;
    
    ResponseEnum(Integer statusCode, String statusMessage) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }
    
    public Integer getCode() {
        return this.statusCode;
    }

    public String getMessage() {
        return this.statusMessage;
    }
    
    public static ResponseEnum getResponse(int code) {
    	for(ResponseEnum re : ResponseEnum.values()) {
    		if(re.statusCode==code) {
    			return re;
    		}
    	}
    	return null;
    }
   
 }
