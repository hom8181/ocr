package kr.jhpark.ocr.service;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import kr.jhpark.ocr.domain.EnumOcrResultType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class MainService {

    public void imageOCR(Map<String, MultipartFile> ocrMap, Map<String, String> ocrResult) {

        String secretKey = "YOUR OCR SECRET KEY";
        String apiURL = "YOUR API URL";
        File file = null;

        try {
            URL url = new URL(apiURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setUseCaches(false);
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setReadTimeout(30000);
            con.setRequestMethod("POST");
            String boundary = "----" + UUID.randomUUID().toString().replaceAll("-", "");
            con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            con.setRequestProperty("X-OCR-SECRET", secretKey);

            JSONObject json = new JSONObject();
            json.put("version", "V2");
            json.put("requestId", UUID.randomUUID().toString());
            json.put("timestamp", System.currentTimeMillis());
            JSONObject image = new JSONObject();
            image.put("format", "jpg");
            image.put("name", "demo");
            JSONArray images = new JSONArray();
            images.put(image);
            json.put("images", images);
            String postParams = json.toString();

            con.connect();
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            MultipartFile businessLicenseImage = ocrMap.get("image");

            file = new File(Objects.requireNonNull(Objects.requireNonNull(businessLicenseImage.getOriginalFilename())));
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(businessLicenseImage.getBytes());
            fos.close();

            writeMultiPart(wr, postParams, file, boundary);
            wr.close();
            file.delete();

            int responseCode = con.getResponseCode();
            BufferedReader br;
            JSONObject jsonObject;
            if (responseCode == 200) {
                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String text = readAll(br);

                // ?????? ?????? jsonObject ??????
                jsonObject = new JSONObject(text);

                // images Array??? ??????
                JSONArray imageList = jsonObject.getJSONArray("images");
                JSONObject imageJson = (JSONObject) imageList.get(0);

                // images ?????? fields Array ??????
                JSONArray fields = imageJson.getJSONArray("fields");

                // fields ?????? ????????? ocr ????????? ?????? JSONObject ?????? ??????
                JSONObject nameJson = new JSONObject();                     // ?????????
                JSONObject addressJson = new JSONObject();                  // ????????? ?????????
                JSONObject businessLicenseNumberJson = new JSONObject();          // ????????? ?????? ??????
                JSONObject representativeNameJson = new JSONObject();       // ????????????
                JSONObject businessTypeJson = new JSONObject();             // ??????
                JSONObject businessItemJson = new JSONObject();             // ??????

                for (int i = 0; i < fields.length(); i++) {
                    // JSONArray.get(i)??? JSONObject??? ?????????
                    JSONObject key = (JSONObject) fields.get(i);

                    // fields ?????? key = "name"
                    String resultType = key.getString("name");

                    // ocr ?????? fields ????????? ???????????? ?????? ????????? JSONObject??? ??????
                    EnumOcrResultType ocrResultType = EnumOcrResultType.valueOf(resultType);
                    switch (ocrResultType) {
                        case company_name:
                            nameJson = (JSONObject) fields.get(i);
                            break;
                        case address:
                            addressJson = (JSONObject) fields.get(i);
                            break;
                        case business_license_number:
                            businessLicenseNumberJson = (JSONObject) fields.get(i);
                            break;
                        case representative_name:
                            representativeNameJson = (JSONObject) fields.get(i);
                            break;
                        case business_type:
                            businessTypeJson = (JSONObject) fields.get(i);
                            break;
                        case business_item:
                            businessItemJson = (JSONObject) fields.get(i);
                            break;
                        default:
                            break;
                    }
                }

                // ????????? JSONObject?????? OCR ?????? ?????? inferText ??????
                String name = nameJson.getString("inferText");
                String address = addressJson.getString("inferText");
                String businessLicenseNumber = businessLicenseNumberJson.getString("inferText");
                String representativeName = representativeNameJson.getString("inferText");
                String businessType = businessTypeJson.getString("inferText");
                String businessItem = businessItemJson.getString("inferText");

                // key value ????????? ?????? ocrResult??? ??????
                ocrResult.put("name", name);
                ocrResult.put("address", address);
                ocrResult.put("businessLicenseNumber", businessLicenseNumber);
                ocrResult.put("representativeName", representativeName);
                ocrResult.put("businessType", businessType);
                ocrResult.put("businessItem", businessItem);

            } else {
                br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            }
            br.close();
        } catch (Exception e) {
            System.out.println(e);
            file.delete();
        }

    }


    private static void writeMultiPart(OutputStream out, String jsonMessage, File file, String boundary) throws
            IOException {

        String sb = "--" + boundary + "\r\n" +
                "Content-Disposition:form-data; name=\"message\"\r\n\r\n" +
                jsonMessage +
                "\r\n";
        out.write(sb.getBytes(StandardCharsets.UTF_8));
        out.flush();

        if (file != null && file.isFile()) {
            out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            String fileString = "Content-Disposition:form-data; name=\"file\"; filename=" +
                    "\"" + file.getName() + "\"\r\n" +
                    "Content-Type: application/octet-stream\r\n\r\n";
            out.write(fileString.getBytes(StandardCharsets.UTF_8));
            out.flush();

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
                out.write("\r\n".getBytes());
            }

            out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        }
        out.flush();
    }

    /**
     * BufferdReader to String
     */
    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

}
