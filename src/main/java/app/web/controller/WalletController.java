package app.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/wallets")
public class WalletController {

    @GetMapping
//    @PreAuthorize( "hasRole('ADMIN')")
    //@PreAuthorize("hasAnyRole('ADMIN', 'USER', 'MODERATOR')") // one of this roles is required
    public String getWalletPage() {
        return "wallets";
    }
}
