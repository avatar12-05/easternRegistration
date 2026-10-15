package com.example;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class MainMenu {

    private static final DateTimeFormatter INPUT_TIME_FORMAT =
        DateTimeFormatter.ofPattern("HHmm");
    private static final DateTimeFormatter DISPLAY_TIME_FORMAT =
        DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    private MainMenu() {
    }

    public static void main(String[] args) throws Exception {
        try (Scanner scanner = new Scanner(System.in)) {
            EasternSchedule schedule = new EasternSchedule();
            ArrayList<Term> terms = schedule.getAvailableTerms();
            if (terms.isEmpty()) {
                System.out.println("No semesters are available.");
                return;
            }

            Term selectedTerm = selectTerm(scanner, terms);
            schedule = new EasternSchedule(selectedTerm.code());

            System.out.println("Selected semester: " + selectedTerm.description());
            System.out.print("Enter a subject code (for example, CSC): ");
            String subject = scanner.nextLine().trim().toUpperCase(Locale.ROOT);
            System.out.print("Enter a course number (leave blank for all): ");
            String courseNumber = scanner.nextLine().trim();
            System.out.print("Enter an instructor name (leave blank for all): ");
            String instructor = scanner.nextLine().trim();
            displayCourses(subject, courseNumber, instructor, schedule);
        }
    }

    private static Term selectTerm(Scanner scanner, List<Term> terms) {
        System.out.println("Available semesters:");
        int semesterCount = Math.min(5, terms.size());
        for (int i = 0; i < semesterCount; i++) {
            System.out.printf("%d. %s%n", i + 1, terms.get(i).description());
        }

        while (true) {
            System.out.print("Select a semester by number: ");
            String selection = scanner.nextLine().trim();
            try {
                int index = Integer.parseInt(selection) - 1;
                if (index >= 0 && index < semesterCount) {
                    return terms.get(index);
                }
            } catch (NumberFormatException exception) {
                // Prompt again when the selection is not a number.
            }
            System.out.println("Enter a number from 1 to " + semesterCount + ".");
        }
    }

    public static void displayCourses(String subject, EasternSchedule schedule) throws Exception {
        displayCourses(subject, "", "", schedule);
    }

    public static void displayCourses(
            String subject,
            String courseNumber,
            EasternSchedule schedule
    ) throws Exception {
        displayCourses(subject, courseNumber, "", schedule);
    }

    public static void displayCourses(
            String subject,
            String courseNumber,
            String instructor,
            EasternSchedule schedule
    ) throws Exception {
        if (subject == null || !subject.matches("[A-Za-z][A-Za-z0-9]*")) {
            System.out.println("Enter a valid subject code using letters and numbers.");
            return;
        }
        if (courseNumber == null
                || (!courseNumber.isBlank() && !courseNumber.matches("[A-Za-z0-9]+"))) {
            System.out.println("Enter a valid course number using letters and numbers.");
            return;
        }
        if (instructor == null) {
            System.out.println("Enter a valid instructor name.");
            return;
        }

        String subjectCode = subject.toUpperCase(Locale.ROOT);
        String numberFilter = courseNumber.trim();
        String instructorFilter = instructor.trim();
        ArrayList<Course> courses = schedule.queryCourseBySubject(subjectCode);
        if (!numberFilter.isEmpty()) {
            courses.removeIf(course -> course.courseNumber() == null
                || !course.courseNumber().equalsIgnoreCase(numberFilter));
        }
        if (!instructorFilter.isEmpty()) {
            courses.removeIf(course -> !matchesInstructor(course, instructorFilter));
        }
        if (courses.isEmpty()) {
            String search = formatSearch(subjectCode, numberFilter, instructorFilter);
            System.out.println("No courses found for " + search + ".");
            return;
        }

        String search = formatSearch(subjectCode, numberFilter, instructorFilter);
        System.out.println("Courses for " + search + ":");
        for (Course course : courses) {
            String section = course.sequenceNumber() == null
                ? "TBA"
                : course.sequenceNumber();
            String title = course.courseTitle() == null
                ? "Title TBA"
                : course.courseTitle().replace("&amp;", "&");
            String courseDetails = String.format(
                "%s %s-%s, %s credits, %s",
                course.subject(),
                course.courseNumber(),
                section,
                formatCredits(course),
                title
            );
            String courseInstructor = formatInstructor(course.faculty());

            if (course.meetingsFaculty() == null || course.meetingsFaculty().isEmpty()) {
                System.out.println(
                    courseDetails + ", Instructor: " + courseInstructor +
                    ", Days TBA, Time TBA"
                );
                continue;
            }

            for (Course.MeetingFaculty meetingFaculty : course.meetingsFaculty()) {
                Course.MeetingTime meeting = meetingFaculty == null
                    ? null
                    : meetingFaculty.meetingTime();
                List<Course.Faculty> instructors =
                    meetingFaculty != null && meetingFaculty.faculty() != null
                    && !meetingFaculty.faculty().isEmpty()
                        ? meetingFaculty.faculty()
                        : course.faculty();
                System.out.println(
                    courseDetails + ", Instructor: " + formatInstructor(instructors) +
                    ", " + formatMeeting(meeting)
                );
            }
        }
    }

    private static String formatSearch(
            String subject,
            String courseNumber,
            String instructor
    ) {
        StringBuilder search = new StringBuilder(subject);
        if (!courseNumber.isEmpty()) {
            search.append(' ').append(courseNumber);
        }
        if (!instructor.isEmpty()) {
            search.append(", instructor ").append(instructor);
        }
        return search.toString();
    }

    private static boolean matchesInstructor(Course course, String search) {
        if (matchesInstructor(course.faculty(), search)) {
            return true;
        }
        if (course.meetingsFaculty() == null) {
            return false;
        }
        for (Course.MeetingFaculty meeting : course.meetingsFaculty()) {
            if (meeting != null && matchesInstructor(meeting.faculty(), search)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesInstructor(List<Course.Faculty> faculty, String search) {
        if (faculty == null) {
            return false;
        }
        for (Course.Faculty member : faculty) {
            if (member != null && member.displayName() != null
                    && member.displayName().toLowerCase(Locale.ROOT)
                        .contains(search.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String formatInstructor(List<Course.Faculty> faculty) {
        if (faculty == null || faculty.isEmpty()) {
            return "TBA";
        }

        ArrayList<String> names = new ArrayList<>();
        for (Course.Faculty instructor : faculty) {
            if (instructor != null && instructor.displayName() != null
                    && !instructor.displayName().isBlank()) {
                names.add(instructor.displayName());
            }
        }
        return names.isEmpty() ? "TBA" : String.join(", ", names);
    }

    private static String formatCredits(Course course) {
        Double credits = course.creditHours() != null
            ? course.creditHours()
            : course.creditHourLow();
        Double maximum = course.creditHourHigh();

        if (credits == null) {
            credits = maximum;
        }
        if (credits == null) {
            return "TBA";
        }

        String formattedCredits = formatNumber(credits);
        if (maximum != null && Double.compare(credits, maximum) != 0) {
            return formattedCredits + "-" + formatNumber(maximum);
        }
        return formattedCredits;
    }

    private static String formatNumber(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static String formatMeeting(Course.MeetingTime meeting) {
        if (meeting == null) {
            return "Days TBA, Time TBA";
        }

        StringBuilder days = new StringBuilder();
        if (meeting.monday()) days.append("M");
        if (meeting.tuesday()) days.append("T");
        if (meeting.wednesday()) days.append("W");
        if (meeting.thursday()) days.append("R");
        if (meeting.friday()) days.append("F");
        if (meeting.saturday()) days.append("Sa");
        if (meeting.sunday()) days.append("Su");

        String daysText = days.length() == 0 ? "Days TBA" : days.toString();
        String timeText = formatTimeRange(meeting.beginTime(), meeting.endTime());
        return daysText + ", " + timeText;
    }

    private static String formatTimeRange(String start, String end) {
        if (start == null || start.isBlank() || end == null || end.isBlank()) {
            return "Time TBA";
        }

        try {
            LocalTime startTime = LocalTime.parse(start, INPUT_TIME_FORMAT);
            LocalTime endTime = LocalTime.parse(end, INPUT_TIME_FORMAT);
            String formattedStart = startTime.format(DISPLAY_TIME_FORMAT);
            String formattedEnd = endTime.format(DISPLAY_TIME_FORMAT);
            if (startTime.getHour() / 12 == endTime.getHour() / 12) {
                formattedStart = formattedStart.replaceFirst(" (AM|PM)$", "");
            }
            return formattedStart + " - " + formattedEnd;
        } catch (DateTimeParseException exception) {
            return start + " - " + end;
        }
    }

    private static String formatTime(String time) {
        if (time == null || time.isBlank()) {
            return "TBA";
        }

        try {
            return LocalTime.parse(time, INPUT_TIME_FORMAT).format(DISPLAY_TIME_FORMAT);
        } catch (DateTimeParseException exception) {
            return time;
        }
    }
}