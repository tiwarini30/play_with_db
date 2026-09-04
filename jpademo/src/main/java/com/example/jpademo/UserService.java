package com.example.jpademo;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    public UserClassSpringboot createUser(UserClassSpringboot user){
        return  userRepository.save(user);
    }

    public List<UserClassSpringboot> getAllUser() {
        return userRepository.findAll();
    }
}
