package com.example;
import java.util.ArrayList;

/**
 * 
 * The main application for the Eastern Registration system
 */
public class EasternRegistration {
    
    private EasternRegistration() {
        // Prevent instantiation
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Main entry point of application
     * @param args not used
     * @throws Exception if cannot retreive courses
     */
    public static void main(String[] args) throws Exception {
        
        // Create EasternSchedule object
        EasternSchedule easternSchedule = new EasternSchedule();
    
        System.out.println("Current Term Code: " + easternSchedule.getTerm());
        System.out.println();
          
        ArrayList<Term> terms = easternSchedule.getAvailableTerms();

        System.out.println("\n");
        int i = 0;
        for (Term term : terms) {
            System.out.println("Term Code: " + term.code());
            System.out.println("Term Description: " + term.description());
            System.out.println("");
            i++;
            if (i >= 1) {
                break;
            }
        }        

        //retreive all CSC courses
        ArrayList<Course> courses = easternSchedule.queryCourseBySubject("CSC");

        System.out.println();

        // Display info for each course (up to 3)
        System.out.println("List of courses (max = 3)");
        int number = 0;        
        for (Course c : courses) {
            System.out.println(c);
            number++;
            if (number >= 3) break;
        }

        System.out.println();

        // Demonstrate course getter methods
        System.out.println("Subject, number, and meetingFaculty for first course:");
        for (Course c : courses) {

            // Demonstrate subject and courseNumber getter methods
            System.out.println("Subject: " + c.subject());
            System.out.println("Course Number: " + c.courseNumber());

            // Demonstrate meetingsFaculty getter method. Since
            // meetingsFaculty is a list, we use the get method for this.
            System.out.println(c.meetingsFaculty().get(0));
            break;
        }        
    }



}