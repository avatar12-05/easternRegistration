package com.example;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Course record to store select course information
 * @param id course id
 * @param subject the course subject
 * @param courseNumber the course number
 * @param sequenceNumber the section number
 * @param courseTitle the course title
 * @param creditHours the course credit hours
 * @param creditHourLow the minimum course credit hours
 * @param creditHourHigh the maximum course credit hours
 * @param maximumEnrollment the course enrollment capacity
 * @param enrollment the number of students enrolled
 * @param seatsAvailable the remaining number of seats
 * @param meetingsFaculty a list of meetingFaculty objects (see @link com.example.MeetingFaculty)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Course(
    String id,
    String subject,
    String courseNumber,
    String sequenceNumber,
    String courseTitle,
    Double creditHours,
    Double creditHourLow,
    Double creditHourHigh,
    Integer maximumEnrollment,
    Integer enrollment,
    Integer seatsAvailable,
    List<Faculty> faculty,
    List<MeetingFaculty> meetingsFaculty
) {

    public Course(
            String id,
            String subject,
            String courseNumber,
            String sequenceNumber,
            String courseTitle,
            Double creditHours,
            Double creditHourLow,
            Double creditHourHigh,
            List<Faculty> faculty,
            List<MeetingFaculty> meetingsFaculty
    ) {
        this(
            id, subject, courseNumber, sequenceNumber, courseTitle,
            creditHours, creditHourLow, creditHourHigh, null, null, null,
            faculty, meetingsFaculty
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Faculty(String displayName) {
    }

    /**
     * 
     * MeetingFaculty record to store select meetingFaculty information
     * @param meetingTime a meetingTime object (see @link com.example.MeetingTime)
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MeetingFaculty(List<Faculty> faculty, MeetingTime meetingTime) {

       /**  
        * Returns a string representation of the meeting faculty objet     
        * @return the formatted meeting faculty string
        */
        @Override 
        public String toString() {
            return meetingTime == null ? "" : meetingTime.toString();
        }
    }
       
    /**
     * 
     * MeetingTime record to store select meetingTime information
     * @param monday true if meeting time is on this day; false otherwise
     * @param tuesday true if meeting time is on this day; false otherwise
     * @param wednesday true if meeting time is on this day; false otherwise
     * @param thursday true if meeting time is on this day; false otherwise
     * @param friday true if meeting time is on this day; false otherwise
     * @param saturday true if meeting time is on this day; false otherwise
     * @param sunday true if meeting time is on this day; false otherwise
     * @param beginTime the meeting start time
     * @param endTime the meeting end time
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MeetingTime(
        boolean monday, boolean tuesday, boolean wednesday,
        boolean thursday, boolean friday, boolean saturday, boolean sunday,
        String beginTime, String endTime
    ){
        /**  
        * Returns a string representation of the meeting time     
        * @return the formatted meeting time string
        */
        @Override 
        public String toString() {
            StringBuilder days = new StringBuilder();
            if (monday) days.append("M");
            if (tuesday) days.append("T");
            if (wednesday) days.append("W");
            if (thursday) days.append("R");
            if (friday) days.append("F");
            if (saturday) days.append("Sa");
            if (sunday) days.append("Su");

            String time = beginTime == null || beginTime.isBlank()
                    || endTime == null || endTime.isBlank()
                ? "Time TBA"
                : beginTime + " - " + endTime;

            return (days.length() == 0 ? "Days TBA" : days.toString())
                + ", " + time;
        }
    }; 

    /**  
     * Returns a string representation of the course     
     * @return the formatted course string
     */
    @Override 
     public String toString() {
        return subject + "-" + courseNumber;
    }
}
    

   
