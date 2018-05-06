package com.example.fluxflixclient

import org.reactivestreams.Publisher
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.cloud.gateway.route.builder.filters
import org.springframework.cloud.gateway.route.builder.routes
import org.springframework.context.support.beans
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router

@SpringBootApplication
class FluxFlixClientApplication

fun main(args: Array<String>) {

    SpringApplicationBuilder()
            .sources(FluxFlixClientApplication::class.java)
            .initializers(beans {

                bean {
                    WebClient.builder()
                            .baseUrl("http://localhost:8080/movies")
                            .build()
                }

                bean {
                    val client = ref<WebClient>()
                    router {
                        GET("/titles") {
                            val names: Publisher<String> =
                                    client
                                            .get()
                                            .retrieve()
                                            .bodyToFlux<Movie>()
                                            .map { it.title }

                            ServerResponse.ok().body(names)
                        }
                    }
                }

                bean {
                    MapReactiveUserDetailsService(
                            User.withDefaultPasswordEncoder()
                                    .username("user")
                                    .password("password")
                                    .roles("USER")
                                    .build())
                }

                bean {
                    val http = ref<ServerHttpSecurity>()
                    http
                            .httpBasic()
                            .and()
                            .authorizeExchange()
                            .pathMatchers("/rl").authenticated()
                            .anyExchange().permitAll()
                            .and()
                            .build()
                }

                bean {
                    val builder = ref<RouteLocatorBuilder>()
                    builder.routes {

                        route {
                            path("/proxy")
                            uri("http://localhost:8080/movies")
                        }

                        route {
                            val rl = ref<RequestRateLimiterGatewayFilterFactory>()
                            val redisRL = rl.apply(RedisRateLimiter.args(5, 10))
                            path("/rl")
                            filters {
                                filter(redisRL)
                            }
                            uri("http://localhost:8080/movies")
                        }

                    }
                }

            })
            .run(*args)

}

class Movie(val id: String? = null, val title: String? = null)