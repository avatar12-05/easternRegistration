package com.example;

import java.io.IOException;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.CookiePolicy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * Provides access to Eastern's schedule of classes for a specified term,
 * using cached values if available
 */
public class EasternSchedule {

    private static final String BASE = "https://reg-prod.ec.easternct.edu/StudentRegistrationSsb";
    private String term;

    CookieManager cookieManager;
    HttpClient client;


    /**
     * 
     * Constructs an EasternSchedule object with default term
     */
    public EasternSchedule()  {

        // set term (202710 is Fall 2026)
        this.term = "202710"; 

        // set cookieManager and build client for web retreival        
        cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        
        client = HttpClient.newBuilder()
        .cookieHandler(cookieManager)
        .build();

    }

    /**
     * Accessor for term code
     * @return The term code
     */
    public String getTerm() {
        return term;
    }

    /**
     * Retrieves available terms (max of 25) from the Eastern's schedule, or from cache
     * if available
     * @return A JSON string representing the available terms
     * @throws IOException if cache file exists but cannot be read
     * @throws InterruptedException if a web request is interrupted
     */
    private String retrieveTerms() throws IOException, InterruptedException {
        Path cacheFile = Path.of("cache/terms.txt");
        String content;

        if (Files.exists(cacheFile)) {
            System.out.println("Using cached term data from " + cacheFile + "...");
            content = Files.readString(cacheFile);
            return content;
        }

        System.out.println("Retrieving data from URL ...");        
        String url = BASE + "/ssb/classSearch/getTerms?offset=1&max=25&searchTerm=";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Unexpected response status: " + response.statusCode());
        }

        content = response.body();
        Files.writeString(cacheFile, content);

        return content; 
    }

    /**
     * Parses the JSON content of available terms into an Arrayist of Terms
     * @param content The JSON string representing the available terms
     * @return An ArrayList of Terms
     * @throws IOException if terms cannot be parsed
     */
    private ArrayList<Term> parseTerms(String content) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ArrayList<Term> terms = mapper.readValue(
            content,
            new TypeReference<ArrayList<Term>>() {}            
        );

        return terms;
    }

    /** 
     * Parses content into a list of courses
     * @param content the content to parse
     * @return ArrayList<Course> an {@link ArrayList} of {@link Course} objects
     * @throws IOException
     */
    private ArrayList<Course> parseCourses(String content) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode dataNode = mapper.readTree(content).get("data");        
        ArrayList<Course> courses = mapper.convertValue(dataNode, new TypeReference<ArrayList<Course>>() {});
        return courses;
    }

    /**
     * Retrieves and parses the available terms from Eastern's schedule
     * @return An ArrayList of Term objects
     * @throws IOException if cannot parse terms
     * @throws InterruptedException if a web request is interrupted
     */
    public ArrayList<Term>getAvailableTerms() throws IOException, InterruptedException {

        String content = retrieveTerms();
        ArrayList<Term> terms = parseTerms(content);
        return terms;
    }

    /**
     * Sets the term for the Eastern schedule
     * @return The response body from the server after setting the term
     * @throws Exception If there is an error during the HTTP request
     */

    private String postTerm() throws Exception {

        String url = BASE + "/ssb/term/search?mode=search";

        String body =
            "term=" + URLEncoder.encode(term, StandardCharsets.UTF_8) +
            "&studyPath=" +
            "&studyPathText=" +
            "&startDatepicker=" +
            "&endDatepicker=";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header(
                "Content-Type",
                "application/x-www-form-urlencoded; charset=UTF-8"
            )            
            .POST(HttpRequest.BodyPublishers.ofString(body))            
            .build();

        HttpResponse<String> response =
            client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException(
                "setTerm failed: " + response.statusCode()
            );
        }

        return response.body();
}

    /** 
     * @param subject
     * @return Path
     */
    private Path createCacheFile(String subject) {
        return Path.of("cache/term_" + term + "_" + subject + ".txt");
    }

    /**
     * Caches courses for current term and specified subject. Does nothing
     * if cache file already exists; otherwise retreives data from webserver
     * @param subject the subject to cache courses for
     * @throws Exception if there is an error connecting to the webserver
     */
    private void cacheCourses(String subject) throws Exception {
        
        Path cacheFile = createCacheFile(subject);

        // if file already exists, return
        if (Files.exists(cacheFile)) {            
           System.out.println("Using cached term data from " + cacheFile + "...");
           return;           
        }

        // post request to establish term is required
        String term_response = postTerm();

        String url = BASE + "/ssb/searchResults/searchResults?" +
        "txt_subject=" + subject + "&" +
        "txt_term=" + term + "&" +
        "startDatepicker=&endDatepicker=&pageOffset=0&pageMaxSize=500&sortColumn=subjectDescription&sortDirection=asc";
                                                    
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header(
                "Accept", "application/json"                    
            )              
            .GET()           
            .build();

        HttpResponse<String> response =
            client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException(
                "searchClasses failed: " + response.statusCode() + "\n" + response.body()
            );
        }

        // save data to cache file
        Files.writeString(cacheFile, response.body());
    }

    /** 
     * Queries courses based on subject. First, we retreive data from the 
     * server and create a cached file if the file does not exist. Then we
     * extract courses from the appropriate file.
     * @param subject the subject of courses to retrieve
     * @return an {@link ArrayList} of {@link Course} objects
     * @throws Exception if there is an error in retreiving courses
     */
    public ArrayList<Course> queryCourseBySubject(String subject) throws Exception {
        
        cacheCourses(subject);
        Path cacheFile = createCacheFile(subject);
        String content = Files.readString(cacheFile);
        
        ArrayList<Course> courses = parseCourses(content);
        return courses;
    }   
}
