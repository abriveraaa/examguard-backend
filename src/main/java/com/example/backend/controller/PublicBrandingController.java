package com.example.backend.controller;

import com.example.backend.dto.core.BrandingResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
public class PublicBrandingController {

    @GetMapping("/branding")
    public BrandingResponse getBranding() {
        BrandingResponse response = new BrandingResponse();
        response.setSchoolName("Polytechnic University of the Philippines");
        response.setShortName("PUP");
        response.setLogoUrl("/branding/PUPLogo.png");
        response.setPictureUrl1("/branding/card-default.png");
        response.setPictureUrl2("/branding/card-overlay.png");
        response.setPrimaryColor("#800000");
        response.setSecondaryColor("#D4AF37");
        response.setProjectName("ExamGuard");
        response.setTagline("Secure Digital Examination Platform");
        return response;
    }
}