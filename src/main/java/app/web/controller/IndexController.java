package app.web.controller;

import app.security.UserData;
import app.user.model.User;
import app.user.service.UserService;
import app.wallet.model.Wallet;
import app.web.dto.LoginRequest;
import app.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class IndexController {

    private final UserService userService;

    @Autowired
    public IndexController(UserService userService) {
        this.userService = userService;
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
    public ModelAndView getLoginPage(@RequestParam(name = "loginAttemptMessage", required = false) String message, @RequestParam(name = "error", required = false) String errorMessage) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("login");
        modelAndView.addObject("loginRequest", new LoginRequest());
        modelAndView.addObject("loginAttemptMessage", message);
        if(errorMessage != null) {
            modelAndView.addObject("errorMessage", "Invalid username or password");
        }

        return modelAndView;
    }

    /////////////////////////// We don't need this anymore because Spring Security is doing it /////////////////////////
//    @PostMapping("/login")
//    public ModelAndView login(@Valid LoginRequest loginRequest, BindingResult bindingResult, HttpSession session) {
//
//        if (bindingResult.hasErrors()) {
//            return new ModelAndView("login");
//        }
//
//        User user = userService.login(loginRequest);
//        session.setAttribute("userId", user.getId());
//
//        return new ModelAndView("redirect:/home");
//    }

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

    @PostMapping("/register")
    public ModelAndView register(@Valid RegisterRequest registerRequest, BindingResult bindingResult, RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return new ModelAndView("register");
        }

        userService.register(registerRequest);
        redirectAttributes.addFlashAttribute("successfulRegistration", "You have registered successfully");

        return new ModelAndView("redirect:/login");
    }

    @GetMapping("/home")
    public ModelAndView getHomePage(@AuthenticationPrincipal UserData userData) {

        //User user = userService.getByUsername(userProperties.getDefaultUser().getUsername());
//        UUID userId = (UUID) session.getAttribute("userId");
//        User user = userService.getById(userId);
        User user = userService.getById(userData.getUserId());

        ModelAndView modelAndView = new ModelAndView();

        modelAndView.setViewName("home");
        modelAndView.addObject("user", user);
        Wallet primaryWallet = user.getWallets().stream()
                .filter(Wallet::isMain)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("User %s has no main wallet".formatted(user.getId())));

        modelAndView.addObject("primaryWallet", primaryWallet);

        return modelAndView;
    }

    /////////////////////////// We don't need this anymore because Spring Security is doing it /////////////////////////
//    @GetMapping("/logout")
//    public String logout(HttpSession session) {
//
//        session.invalidate();
//
//        return "redirect:/";
//    }
}