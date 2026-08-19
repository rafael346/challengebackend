package com.verzel.challengebackend.service.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TmdbMovieDto(
        Integer id,
        String title,
        String overview,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("release_date") String releaseDate) {
}
