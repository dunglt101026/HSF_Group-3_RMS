package com.group3.rms.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequestMapping("/demo")
public class DemoController {

    private final DemoService demoService;

    @Autowired
    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    /**
     * Mặc định vào /demo sẽ chuyển đến màn hello
     */
    @GetMapping
    public String demoIndex() {
        return "redirect:/demo/hello";
    }

    /**
     * URL 1: Chào người dùng
     * GET /demo/hello?name=...
     */
    @GetMapping("/hello")
    public String helloUser(@RequestParam(value = "name", defaultValue = "Bạn") String name, Model model) {
        Map<String, Object> greetingData = demoService.getGreetingData(name);
        model.addAttribute("activeTab", "hello");
        model.addAttribute("greeting", greetingData);
        model.addAttribute("pageTitle", "Demo - Xin chào người dùng");
        return "demo";
    }

    /**
     * URL 2: Kiểm tra kết nối Database
     * GET /demo/db-check
     */
    @GetMapping("/db-check")
    public String checkDatabase(Model model) {
        Map<String, Object> dbStatus = demoService.checkDatabaseConnection();
        model.addAttribute("activeTab", "db");
        model.addAttribute("dbStatus", dbStatus);
        model.addAttribute("pageTitle", "Demo - Kiểm tra kết nối Database");
        return "demo";
    }
}
