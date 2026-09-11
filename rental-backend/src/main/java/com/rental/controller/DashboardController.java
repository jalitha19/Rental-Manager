package com.rental.controller;

import com.rental.dto.DashboardResponse;
import com.rental.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/api/dashboard")
    public DashboardResponse getDashboard() {
        return dashboardService.getDashboard();
    }
}
