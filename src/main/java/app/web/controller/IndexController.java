package app.web.controller;

import app.user.model.User;
import app.user.property.UserProperties;
import app.user.service.UserService;
import app.web.dto.RegisterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class IndexController {

    private final UserService userService;
    private final UserProperties userProperties;

    @Autowired
    public IndexController(UserService userService, UserProperties userProperties) {
        this.userService = userService;
        this.userProperties = userProperties;
    }

    @GetMapping("/")
    public String getIndexPage() {
        return "index";
    }

    // Variant 2:
//    @GetMapping("/")
//    public ModelAndView getIndexPage() {
//        ModelAndView modelAndView = new ModelAndView();
//        modelAndView.setViewName("index");
//        return modelAndView;
//    }

    @GetMapping("/login")
    public String getLoginPage() {
        return "login";
    }

    @GetMapping("/register")
    public ModelAndView getRegisterPage() {

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("register");

        // We send an empty DTO object to the register page because we have there a form that the user will fill in.
        // We needed it because when we go to the thymeleaf page, the form is empty. If we don't send it, we will get an error.
        // In the thymeleaf page, we will use this object to bind the form fields to the DTO object.
        // We use th:object="${registerRequest}" to bind the object to the thymeleaf page.
        // We fiel the fields of the DTO object to the form fields.
        // We use th:field="*{username}" to bind the form field to the DTO object. (input field for username)

        // th:action -> specifies the form action URL
        // Example: th:action="@{/register}" means that the form will be submitted to /register URL.
        // th:method -> specifies the form method (GET or POST)
        // Example: th:method="post" means that the form will be submitted using POST

        modelAndView.addObject("registerRequest", new RegisterRequest());


        return modelAndView;
    }

    @GetMapping("/home")
    public ModelAndView getHomePage() {

        User user = userService.getByUsername(userProperties.getDefaultUser().getUsername());

        ModelAndView modelAndView = new ModelAndView();

        modelAndView.setViewName("home");
        modelAndView.addObject("user", user);

        return modelAndView;
    }
}
