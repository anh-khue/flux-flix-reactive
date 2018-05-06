package com.khuetla.spring.reactive.fluxflixservice;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MovieServiceTest {
    
    @Autowired
    private MovieService movieService;
    
    @Test
    public void getEventsTake10() {
        String movieId = movieService.getAllMovies().blockFirst().getId();
        
        StepVerifier.withVirtualTime(() -> movieService.getEvents(movieId).take(10))
                .thenAwait(Duration.ofHours(10))
                .expectNextCount(10)
                .verifyComplete();
    }
}