package com.khuetla.spring.reactive.fluxflixservice;

import lombok.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.RouterFunction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Date;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.*;
import static org.springframework.web.reactive.function.server.ServerResponse.*;

@SpringBootApplication
public class FluxFlixServiceApplication {
    
    @Bean
    ApplicationRunner demoData(MovieRepository movieRepository) {
        return args -> {
            movieRepository.deleteAll().thenMany(
                    Flux.just("The Silence of Lambdas", "Back to the Future",
                            "Fluxxers", "Flux Holmes", "The Fluxxinator",
                            "Fluxxies League", "Fluxxerine")
                            .map(Movie::new)
                            .flatMap(movieRepository::save))
                    .thenMany(movieRepository.findAll())
                    .subscribe(System.out::println);
        };
    }
    
    @Bean
    RouterFunction<?> routerFunction(MovieService ms) {
        return route(
                GET("/movies"),
                request -> ok().body(ms.getAllMovies(), Movie.class))
                .andRoute(GET("/movies/{id}"),
                        request -> ok().body(ms.getMovieById(request.pathVariable("id")), Movie.class))
                .andRoute(GET("/movies/{id}/events"),
                        request -> ok().contentType(MediaType.TEXT_EVENT_STREAM)
                                .body(ms.getEvents(request.pathVariable("id")), MovieEvent.class));
    }
    
    public static void main(String[] args) {
        SpringApplication.run(FluxFlixServiceApplication.class, args);
    }
}

@Service
class MovieService {
    
    private final MovieRepository movieRepository;
    
    MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }
    
    public Flux<Movie> getAllMovies() {
        return this.movieRepository.findAll();
    }
    
    public Mono<Movie> getMovieById(String id) {
        return this.movieRepository.findById(id);
    }
    
    public Flux<MovieEvent> getEvents(String movieId) {
        return Flux.<MovieEvent>generate(sink -> sink.next(new MovieEvent(movieId, new Date())))
                .delayElements(Duration.ofSeconds(1));
    }
}

interface MovieRepository extends ReactiveCrudRepository<Movie, String> {
    
    Flux<Movie> findByTitle(String title);
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class MovieEvent {
    
    private String movieId;
    private Date dateViewed;
}

@Document
@Data
@NoArgsConstructor
@RequiredArgsConstructor
class Movie {
    
    @Id
    private String id;
    @NonNull
    private String title;
}