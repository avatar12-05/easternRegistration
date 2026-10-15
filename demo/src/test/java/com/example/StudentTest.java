package com.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.List;
import org.junit.Test;

public class StudentTest {

    @Test
    public void createsStudentWithSpecifiedIdentity() {
        Student student = new Student("  12345  ", " Ada ", " Lovelace ");

        assertEquals("12345", student.getId());
        assertEquals("Ada", student.getFirstName());
        assertEquals("Lovelace", student.getLastName());
        assertTrue(student.getCourses().isEmpty());
    }

    @Test
    public void addsAndDropsCoursesByCourseId() {
        Student student = new Student("12345", "Ada", "Lovelace");
        Course course = course("section-1", "CSC", "360");
        Course sameCourse = course("section-1", "CSC", "360");
        Course otherCourse = course("section-2", "CSC", "361");

        assertTrue(student.addCourse(course));
        assertFalse(student.addCourse(sameCourse));
        assertEquals(List.of(course), student.getCourses());

        assertTrue(student.dropCourse(sameCourse));
        assertFalse(student.dropCourse(otherCourse));
        assertTrue(student.getCourses().isEmpty());
    }

    @Test
    public void dropsMultipleSpecifiedCourses() {
        Student student = new Student("12345", "Ada", "Lovelace");
        Course first = course("section-1", "CSC", "360");
        Course second = course("section-2", 3.0, 2, meeting(false, "0900", "1000"));
        Course notEnrolled = course("section-3", "CSC", "362");
        assertTrue(student.addCourse(first));
        assertTrue(student.addCourse(second));

        assertEquals(2, student.dropCourses(first, second, notEnrolled));
        assertTrue(student.getCourses().isEmpty());
    }

    @Test
    public void dropsAllCourses() {
        Student student = new Student("12345", "Ada", "Lovelace");
        assertTrue(student.addCourse(course("section-1", "CSC", "360")));
        assertTrue(student.addCourse(
            course("section-2", 3.0, 2, meeting(false, "0900", "1000"))
        ));

        student.dropAllCourses();

        assertTrue(student.getCourses().isEmpty());
    }

    @Test
    public void displaysEnrolledCourseSchedule() {
        Student student = new Student("12345", "Ada", "Lovelace");
        Course course = course("section-1", "CSC", "360");
        assertTrue(student.addCourse(course));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;
        try {
            System.setOut(new PrintStream(output));

            student.displaySchedule();
        } finally {
            System.setOut(originalOutput);
        }

        String schedule = output.toString();
        assertTrue(schedule.contains("Schedule for Ada Lovelace (ID: 12345)"));
        assertTrue(schedule.contains("CSC 360-01, 3 credits, Course"));
        assertTrue(schedule.contains("M, 9:00 - 10:00 AM, Instructor: TBA"));
    }

    @Test
    public void displaysWhenStudentHasNoCourses() {
        Student student = new Student("12345", "Ada", "Lovelace");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;
        try {
            System.setOut(new PrintStream(output));

            student.displaySchedule();
        } finally {
            System.setOut(originalOutput);
        }

        assertTrue(output.toString().contains("No courses are currently enrolled."));
    }

    @Test
    public void mainMenuCreatesStudentFromEnteredIdentity() {
        Student student;
        try (Scanner scanner = new Scanner("12345\nAda\nLovelace\n")) {
            student = MainMenu.createStudent(scanner);
        }

        assertEquals("12345", student.getId());
        assertEquals("Ada", student.getFirstName());
        assertEquals("Lovelace", student.getLastName());
    }

    @Test
    public void mainMenuAddsSelectedCoursesToStudentSchedule() {
        Student student = new Student("12345", "Ada", "Lovelace");
        Course first = courseWithoutMeeting("first", 3.0);
        Course second = new Course(
            "second", "CSC", "361", "01", "Second course", 3.0, 3.0, 3.0,
            20, 19, 1, List.of(), List.of()
        );
        try (Scanner scanner = new Scanner("2\n")) {
            MainMenu.addCoursesToSchedule(scanner, student, List.of(first, second));
        }

        assertEquals(List.of(second), student.getCourses());
    }

    @Test
    public void mainMenuKeepsNonConflictingCoursesAndPromptsForReplacements() {
        Student student = new Student("12345", "Ada", "Lovelace");
        Course mondayFirst = timedCourse("monday-first", meeting(true, false, false, false,
            "0900", "1000"));
        Course tuesday = timedCourse("tuesday", meeting(false, true, false, false,
            "0900", "1000"));
        Course wednesday = timedCourse("wednesday", meeting(false, false, true, false,
            "0900", "1000"));
        Course thursday = timedCourse("thursday", meeting(false, false, false, true,
            "0900", "1000"));
        Course conflictingCourse = timedCourse(
            "monday-second", "361", meeting(true, false, false, false, "0930", "1030")
        );
        List<Course> available = List.of(
            mondayFirst, conflictingCourse, tuesday, wednesday, thursday
        );

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;
        try (Scanner scanner = new Scanner("1,2,3\n4,5\n")) {
            System.setOut(new PrintStream(output));
            MainMenu.addCoursesToSchedule(scanner, student, available);
        } finally {
            System.setOut(originalOutput);
        }

        assertEquals(List.of(tuesday, wednesday, thursday), student.getCourses());
        assertTrue(output.toString().contains(
            "Cannot add CSC 361-01: conflicts with CSC 360-01."
        ));
    }

    @Test
    public void mainMenuPromptsForReplacementWhenSelectedCourseIsFull() {
        Student student = new Student("12345", "Ada", "Lovelace");
        Course full = course("full", 3.0, 0, meeting(true, "0900", "1000"));
        Course available = courseWithoutMeeting("available", 3.0);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;
        try (Scanner scanner = new Scanner("1\n2\n")) {
            System.setOut(new PrintStream(output));
            MainMenu.addCoursesToSchedule(scanner, student, List.of(full, available));
        } finally {
            System.setOut(originalOutput);
        }

        assertEquals(List.of(available), student.getCourses());
        assertTrue(output.toString().contains(
            "Cannot add CSC 360-01: Course is full. Select another course."
        ));
    }

    @Test
    public void rejectsFullCourse() {
        Student student = new Student("12345", "Ada", "Lovelace");

        assertAddRejected(student, course("full", 3.0, 0, meeting(true, "0900", "1000")));
    }

    @Test
    public void allowsMultipleCoursesInOneOperation() {
        Student student = new Student("12345", "Ada", "Lovelace");
        Course first = courseWithoutMeeting("one", 8.0);
        Course second = courseWithoutMeeting("two", 8.0);

        assertEquals(2, student.addCourses(first, second));
        assertEquals(List.of(first, second), student.getCourses());
    }

    @Test
    public void enforcesMaximumCreditHours() {
        Student student = new Student("12345", "Ada", "Lovelace");
        Course[] courses = {
            courseWithoutMeeting("course-1", 4.0),
            courseWithoutMeeting("course-2", 4.0),
            courseWithoutMeeting("course-3", 4.0),
            courseWithoutMeeting("course-4", 4.0)
        };

        assertEquals(4, student.addCourses(courses));
        assertEquals(16.0, student.getTotalCredits(), 0.0);
        assertAddRejected(student, courseWithoutMeeting("course-too-many", 0.5));
    }

    /** 
     * @throws Exception
     */
    @Test
    public void scheduleMenuAllowsDroppingCourseAndFinishing() throws Exception {
        Student student = new Student("12345", "Ada", "Lovelace");
        Course course = courseWithoutMeeting("course-1", 4.0);
        assertTrue(student.addCourse(course));

        try (Scanner scanner = new Scanner("3\n1\n0\n")) {
            MainMenu.manageSchedule(scanner, student, List.of(), new EasternSchedule());
        }

        assertTrue(student.getCourses().isEmpty());
        assertEquals(0.0, student.getTotalCredits(), 0.0);
    }

    @Test
    public void batchRegistrationIsAtomicWhenCourseCannotBeAdded() {
        Student student = new Student("12345", "Ada", "Lovelace");
        Course first = courseWithoutMeeting("one", 3.0);
        Course full = course("full", 3.0, 0, meeting(false, "0900", "1000"));
        assertTrue(student.addCourse(first));

        assertAddCoursesRejected(student, courseWithoutMeeting("two", 3.0), full);

        assertEquals(List.of(first), student.getCourses());
    }

    @Test
    public void rejectsOverlappingMeetingsOnSharedDay() {
        Student student = new Student("12345", "Ada", "Lovelace");
        assertTrue(student.addCourse(
            course("one", 3.0, 2, meeting(true, "0900", "1015"))
        ));

        assertAddRejected(
            student,
            course("two", 3.0, 2, meeting(true, "1000", "1100"))
        );
    }

    @Test
    public void allowsAdjacentMeetingsOnSharedDay() {
        Student student = new Student("12345", "Ada", "Lovelace");
        assertTrue(student.addCourse(
            course("one", 3.0, 2, meeting(true, "0900", "1000"))
        ));

        assertTrue(student.addCourse(
            course("two", 3.0, 2, meeting(true, "1000", "1100"))
        ));
    }

    @Test
    public void allowsOverlappingMeetingsOnDifferentDays() {
        Student student = new Student("12345", "Ada", "Lovelace");
        assertTrue(student.addCourse(
            course("one", 3.0, 2, meeting(true, "0900", "1000"))
        ));

        assertTrue(student.addCourse(
            course("two", 3.0, 2, meeting(false, "0900", "1000"))
        ));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankStudentIdentity() {
        new Student("12345", " ", "Lovelace");
    }

    /** 
     * @param id
     * @param subject
     * @param courseNumber
     * @return Course
     */
    private Course course(String id, String subject, String courseNumber) {
        return new Course(
            id, subject, courseNumber, "01", "Course", 3.0, 3.0, 3.0,
            20, 18, 2, List.of(),
            List.of(new Course.MeetingFaculty(List.of(), meeting(true, "0900", "1000")))
        );
    }

    /** 
     * @param id
     * @param credits
     * @param seatsAvailable
     * @param meetingTime
     * @return Course
     */
    private Course course(
            String id,
            double credits,
            int seatsAvailable,
            Course.MeetingTime meetingTime
    ) {
        return new Course(
            id, "CSC", "360", "01", "Course", credits, credits, credits,
            20, 20 - seatsAvailable, seatsAvailable,
            List.of(), List.of(new Course.MeetingFaculty(List.of(), meetingTime))
        );
    }

    /** 
     * @param id
     * @param credits
     * @return Course
     */
    private Course courseWithoutMeeting(String id, double credits) {
        return new Course(
            id, "CSC", "360", "01", "Course", credits, credits, credits,
            20, 19, 1, List.of(), List.of()
        );
    }

    /** 
     * @param id
     * @param meetingTime
     * @return Course
     */
    private Course timedCourse(String id, Course.MeetingTime meetingTime) {
        return timedCourse(id, "360", meetingTime);
    }

    private Course timedCourse(
            String id,
            String courseNumber,
            Course.MeetingTime meetingTime
    ) {
        return new Course(
            id, "CSC", courseNumber, "01", "Course", 3.0, 3.0, 3.0,
            20, 19, 1, List.of(),
            List.of(new Course.MeetingFaculty(List.of(), meetingTime))
        );
    }

    /** 
     * @param monday
     * @param start
     * @param end
     * @return MeetingTime
     */
    private Course.MeetingTime meeting(boolean monday, String start, String end) {
        return new Course.MeetingTime(
            monday, !monday, false, false, false, false, false, start, end
        );
    }

    /** 
     * @param monday
     * @param tuesday
     * @param wednesday
     * @param thursday
     * @param start
     * @param end
     * @return MeetingTime
     */
    private Course.MeetingTime meeting(
            boolean monday,
            boolean tuesday,
            boolean wednesday,
            boolean thursday,
            String start,
            String end
    ) {
        return new Course.MeetingTime(
            monday, tuesday, wednesday, thursday, false, false, false, start, end
        );
    }

    /** 
     * @param student
     * @param course
     */
    private void assertAddRejected(Student student, Course course) {
        try {
            student.addCourse(course);
        } catch (IllegalStateException expected) {
            return;
        }
        throw new AssertionError("Expected course registration to be rejected.");
    }

    /** 
     * @param student
     * @param courses
     */
    private void assertAddCoursesRejected(Student student, Course... courses) {
        try {
            student.addCourses(courses);
        } catch (IllegalStateException expected) {
            return;
        }
        throw new AssertionError("Expected course registration to be rejected.");
    }
}
