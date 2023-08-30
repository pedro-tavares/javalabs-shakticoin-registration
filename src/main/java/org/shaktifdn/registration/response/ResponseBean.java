package org.shaktifdn.registration.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.shaktifdn.registration.constant.Constant;
import org.shaktifdn.registration.enums.ResponseEnum;

import java.util.Date;
import java.util.Objects;

@Data
@Jacksonized
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ResponseBean {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constant.TIMESTAMP_PATTERN)
    private String timestamp = String.valueOf(new Date());
    private Integer status;
    private String message;
    private Object details;


    public ResponseBean(int code, Object details) {
        ResponseEnum response = ResponseEnum.getResponse(code);
        this.setStatus(Objects.requireNonNull(response).getCode());
        this.setMessage(response.getMessage());
        this.setDetails(details);
    }


}
