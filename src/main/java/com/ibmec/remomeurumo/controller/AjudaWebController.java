package com.ibmec.remomeurumo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AjudaWebController {

    @GetMapping("/ajuda")
    public String ajuda() {
        return "ajuda/index";
    }
}
