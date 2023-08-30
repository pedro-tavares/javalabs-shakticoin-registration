package org.shaktifdn.registration.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.shaktifdn.registration.constant.Constant;
import org.shaktifdn.registration.constant.Message;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class GeoJSONModel {


    @DecimalMax(value = Constant.LONGITUDE_MAX, message = Message.GREATER_INVALID_LONGITUDE)
    @DecimalMin(value = Constant.LONGITUDE_MIN, message = Message.LESSER_INVALID_LONGITUDE)
    double longitude;

    @DecimalMax(value = Constant.LATITUDE_MAX, message = Message.GREATER_INVALID_LATITUDE)
    @DecimalMin(value = Constant.LATITUDE_MIN, message = Message.LESSER_INVALID_LATITUDE)
    double latitude;

}
