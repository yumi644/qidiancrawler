package com.example.qidian.web.login;

import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.qidian.domain.User;
import com.example.qidian.service.UserService;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public String me() {
        return "me";
    }

    @GetMapping("/findbyphone")
    public Optional<User> byPhone(@RequestParam("phone") String phone) {
        return userService.findByPhone(phone);
    }

    @GetMapping("/findbyusername")
    public Optional<User> byUsername(@RequestParam("username") String username) {
        return userService.findByUsername(username);
    }

    @PostMapping("/insert")
    public int insertUser(@RequestBody User user) {
        return userService.insertUser(user);
    }

}
