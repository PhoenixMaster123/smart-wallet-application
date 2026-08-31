package app.web.controller;

import app.analytics.model.SpendingSummary;
import app.analytics.service.AnalyticsService;
import app.security.UserData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/analytics")
public class AnalyticsController {

    private static final int DEFAULT_PERIOD_DAYS = 30;

    private final AnalyticsService analyticsService;

    @Autowired
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * @param period how far back to summarise: 30, 90, or "all". Anything else
     *               falls back to the default month rather than failing.
     */
    @GetMapping
    public ModelAndView getAnalyticsPage(@AuthenticationPrincipal UserData userData,
                                         @RequestParam(required = false) String period) {

        Integer days = daysOf(period);
        SpendingSummary summary = analyticsService.summarise(userData.getUserId(), userData.getUsername(), days);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("analytics");
        modelAndView.addObject("summary", summary);
        modelAndView.addObject("period", days == null ? "all" : String.valueOf(days));

        return modelAndView;
    }

    private Integer daysOf(String period) {
        if ("all".equals(period)) {
            return null;
        }
        if ("90".equals(period)) {
            return 90;
        }
        return DEFAULT_PERIOD_DAYS;
    }
}
