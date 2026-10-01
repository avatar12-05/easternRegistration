package com.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 
 * Term record to store term information
 * @param code the term code
 * @param description the term description
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Term(String code, String description) {}

    

   

