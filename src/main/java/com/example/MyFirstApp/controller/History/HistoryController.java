package com.example.MyFirstApp.controller.History;

import com.example.MyFirstApp.DTO.HistoryDashboard.DetailResponse.HistoryDetailResponse;
import com.example.MyFirstApp.DTO.HistoryDashboard.HistoryDashboardResponse;
import com.example.MyFirstApp.DTO.HistoryDashboard.ReadinessResponse;
import com.example.MyFirstApp.service.HistoryDashboard.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;


    @GetMapping("/dashboard")
    public HistoryDashboardResponse getDashboard(
            @RequestParam Long userId
    ) {

        return historyService.getDashboard(
                userId
        );
    }


    @GetMapping("/readiness")
    public ReadinessResponse getReadiness(
            @RequestParam Long userId
    ) {

        return historyService.getReadiness(
                userId
        );
    }

    @GetMapping("/detail")
    public HistoryDetailResponse getDetail(

            @RequestParam String type,

            @RequestParam Long id

    ) {

        return historyService.getHistoryDetail(
                type,
                id
        );
    }
}