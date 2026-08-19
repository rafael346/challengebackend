package com.verzel.challengebackend.service.tmdb;

import java.util.List;

record TmdbNowPlayingResponse(List<TmdbMovieDto> results) {
    TmdbNowPlayingResponse {
        results = results == null ? List.of() : results;
    }
}
