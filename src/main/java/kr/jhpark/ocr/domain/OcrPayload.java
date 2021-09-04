package kr.jhpark.ocr.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OcrPayload {

    private String company_name;
    private String business_license_number;
    private String representative_name;
    private String business_type;
    private String business_item;
    private String address;

}
