package com.pro.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    /**
     * Serves the main analyzer dashboard
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * Serves the project analyzer page
     */
    @GetMapping("/analyzer")
    public String analyzer() {
        return "analyzer";
    }

    /**
     * Serves the dependency visualization page
     */
    @GetMapping("/dependencies")
    public String dependencies() {
        return "dependencies";
    }
}