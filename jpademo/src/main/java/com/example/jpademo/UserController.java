package com.example.jpademo;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    private UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping 
    public UserClassSpringboot createUser(@RequestBody UserClassSpringboot user){
        return userService.createUser(user);
    }
    @GetMapping
    public List<UserClassSpringboot> getUsers(){
        return userService.getAllUser();
    }

}
