package com.example;

import java.util.List;
import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Course record to store select course information
 * @param id course id
 * @param subject the course subject
 * @param courseNumber the course number
 * @param meetingsFaculty a list of meetingFaculty objects (see @link com.example.MeetingFaculty)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Course(String id, String subject, String courseNumber, List<MeetingFaculty> meetingsFaculty) {

    /**
     * 
     * MeetingFaculty record to store select meetingFaculty information
     * @param meetingTime a meetingTime object (see @link com.example.MeetingTime)
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MeetingFaculty(MeetingTime meetingTime) {

       /**  
        * Returns a string representation of the meeting faculty objet     
        * @return the formatted meeting faculty string
        */
        @Override 
        public String toString() {
            return meetingTime.toString();
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
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MeetingTime(
        boolean monday, boolean tuesday, boolean wednesday,
        boolean thursday, boolean friday, boolean saturday, boolean sunday
    ){
        /**  
        * Returns a string representation of the meeting time     
        * @return the formatted meeting time string
        */
        @Override 
        public String toString() {
           
            // TO DO: this should return a reasonable representation of 
            // a meeting time (e.g., 12:00 - 12:50 PM, MWF)
            HashMap<String, Boolean> daysMap = new HashMap<String, Boolean>();
            daysMap.put("M", monday);
            return daysMap.toString();            

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
    

   

