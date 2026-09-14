package com.example.MyFirstApp.controller.QuickDrillController;

import com.example.MyFirstApp.DTO.QuickDrillDTO.*;

import com.example.MyFirstApp.Entity.QuickDrillSession.QuickDrillQuestion;
import com.example.MyFirstApp.Entity.QuickDrillSession.QuickDrillSession;
import com.example.MyFirstApp.service.QuickDrill.QuickDrillService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/quick-drill")
@RequiredArgsConstructor
public class QuickDrillController {

    private final QuickDrillService quickDrillService;


    // =========================================================
    // START DRILL
    // =========================================================

    @PostMapping("/start")
    public QuickDrillStartResponse startDrill(

            @RequestParam("userId")
            Long userId,

            @RequestBody
            QuickDrillStartRequest request

    ) {

        return quickDrillService.startDrill(
                userId,
                request
        );
    }


    // =========================================================
    // SUBMIT ANSWER
    // =========================================================

    @PostMapping("/submit")
    public QuickDrillFeedbackResponse submitAnswer(

            @RequestParam("userId")
            Long userId,

            @RequestBody
            com.example.MyFirstApp.DTO.QuickDrillDTO.QuickDrillSubmitRequest request

    ) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Request cannot be null"
            );
        }


        return quickDrillService.submitAnswer(

                userId,

                request.getDrillId(),

                request.getQuestionId(),

                request.getAnswer()
        );
    }


    // =========================================================
    // NEXT QUESTION
    // =========================================================

    @PostMapping("/next")
    public QuickDrillNextQuestionResponse nextQuestion(

            @RequestParam("userId")
            Long userId,

            @RequestParam("drillId")
            Long drillId

    ) {

        return quickDrillService.nextQuestion(
                userId,
                drillId
        );
    }


    // =========================================================
    // GET SESSION
    // =========================================================

    @GetMapping("/{drillId}")
    public QuickDrillSession getSession(

            @RequestParam("userId")
            Long userId,

            @PathVariable
            Long drillId

    ) {

        return quickDrillService.getSession(
                userId,
                drillId
        );
    }


    // =========================================================
    // GET QUESTIONS
    // =========================================================

    @GetMapping("/{drillId}/questions")
    public List<QuickDrillQuestion> getQuestions(

            @RequestParam("userId")
            Long userId,

            @PathVariable
            Long drillId

    ) {

        return quickDrillService.getQuestions(
                userId,
                drillId
        );
    }
    @GetMapping("/{drillId}/result")
    public QuickDrillResultResponse getResult(

            @RequestParam("userId")
            Long userId,

            @PathVariable
            Long drillId

    ) {

        return quickDrillService.getResult(
                userId,
                drillId
        );
    }
}

