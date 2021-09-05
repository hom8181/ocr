package kr.jhpark.ocr.controller;

import kr.jhpark.ocr.service.MainService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Controller
@AllArgsConstructor
public class MainController {

    private final MainService mainService;

    @GetMapping("/")
    private String mainPage() {

        return "index";
    }

    /**
     * OCR Async
     */
    @PostMapping(value = "/ocr.do")
    @ResponseBody
    public Object ocrImage(@RequestParam("image") MultipartFile image, HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {

        Map<String, MultipartFile> multipartFileMap = new HashMap<>();
        multipartFileMap.put("image", image);

        mainService.imageOCR(multipartFileMap);

        return "";

    }
}
