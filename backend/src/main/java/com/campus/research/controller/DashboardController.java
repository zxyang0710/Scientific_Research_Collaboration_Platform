package com.campus.research.controller;

import com.campus.research.common.ApiResponse;
import com.campus.research.dto.DashboardDtos.DashboardView;
import com.campus.research.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ApiResponse<DashboardView> dashboard() {
        return ApiResponse.success(dashboardService.dashboard());
    }
}
