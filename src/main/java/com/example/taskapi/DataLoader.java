package com.example.taskapi;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final TaskRepository repository;

    public DataLoader(TaskRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() == 0) {
            repository.save(new Task("Set up the project", true));
            repository.save(new Task("Write the REST endpoints", false));
            repository.save(new Task("Ship it", false));
        }
    }
}
