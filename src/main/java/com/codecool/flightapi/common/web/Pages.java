package com.codecool.flightapi.common.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Paging rules shared by the list endpoints ({@code ?page=0&size=20}). */
public final class Pages {

    private Pages() {}

    /**
     * The requested page number and size, in the endpoint's own order. Clients cannot choose the sort:
     * a fixed order ending in the id keeps pages stable between requests and never sorts on an
     * unindexed or internal column.
     */
    public static Pageable ordered(Pageable requested, Sort sort) {
        return PageRequest.of(requested.getPageNumber(), requested.getPageSize(), sort);
    }
}
