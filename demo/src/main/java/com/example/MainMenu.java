package com.example;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;

public class MainMenu {

    private static final DateTimeFormatter INPUT_TIME_FORMAT =
        DateTimeFormatter.ofPattern("HHmm");
    private static final DateTimeFormatter DISPLAY_TIME_FORMAT =
        DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    private MainMenu() {
    }

    public static void main(String[] args) throws Exception {
        try (Scanner scanner = new Scanner(System.in)) {
            Student student = createStudent(scanner);
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
            List<Course> courses =
                searchAndDisplayCourses(subject, courseNumber, instructor, schedule);
            addCoursesToSchedule(scanner, student, courses);
            manageSchedule(scanner, student, courses, schedule);
        }
    }

    static Student createStudent(Scanner scanner) {
        System.out.print("Enter student ID: ");
        String id = scanner.nextLine();
        System.out.print("Enter student first name: ");
        String firstName = scanner.nextLine();
        System.out.print("Enter student last name: ");
        String lastName = scanner.nextLine();
        return new Student(id, firstName, lastName);
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
        searchAndDisplayCourses(subject, courseNumber, instructor, schedule);
    }

    private static List<Course> searchAndDisplayCourses(
            String subject,
            String courseNumber,
            String instructor,
            EasternSchedule schedule
    ) throws Exception {
        if (subject == null || !subject.matches("[A-Za-z][A-Za-z0-9]*")) {
            System.out.println("Enter a valid subject code using letters and numbers.");
            return List.of();
        }
        if (courseNumber == null
                || (!courseNumber.isBlank() && !courseNumber.matches("[A-Za-z0-9]+"))) {
            System.out.println("Enter a valid course number using letters and numbers.");
            return List.of();
        }
        if (instructor == null) {
            System.out.println("Enter a valid instructor name.");
            return List.of();
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
            return courses;
        }

        String search = formatSearch(subjectCode, numberFilter, instructorFilter);
        System.out.println("Courses for " + search + ":");
        for (int courseIndex = 0; courseIndex < courses.size(); courseIndex++) {
            Course course = courses.get(courseIndex);
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
                    (courseIndex + 1) + ". " + courseDetails + ", Instructor: "
                    + courseInstructor +
                    ", Days TBA, Time TBA"
                );
                continue;
            }

            for (int meetingIndex = 0;
                    meetingIndex < course.meetingsFaculty().size();
                    meetingIndex++) {
                Course.MeetingFaculty meetingFaculty =
                    course.meetingsFaculty().get(meetingIndex);
                Course.MeetingTime meeting = meetingFaculty == null
                    ? null
                    : meetingFaculty.meetingTime();
                List<Course.Faculty> instructors =
                    meetingFaculty != null && meetingFaculty.faculty() != null
                    && !meetingFaculty.faculty().isEmpty()
                        ? meetingFaculty.faculty()
                        : course.faculty();
                System.out.println(
                    (meetingIndex == 0 ? (courseIndex + 1) + ". " : "   ")
                    + courseDetails + ", Instructor: " + formatInstructor(instructors) +
                    ", " + formatMeeting(meeting)
                );
            }
        }
        return courses;
    }

    static void addCoursesToSchedule(
            Scanner scanner,
            Student student,
            List<Course> availableCourses
    ) {
        if (availableCourses.isEmpty()) {
            return;
        }
        while (true) {
            System.out.print(
                "Enter course numbers to add, separated by commas (blank to finish): "
            );
            String selection = scanner.nextLine().trim();
            if (selection.isEmpty()) {
                return;
            }
            try {
                List<Integer> selectedIndices =
                    parseCourseNumbers(selection, availableCourses.size());
                Set<Integer> previouslySelectedIndices = new HashSet<>(selectedIndices);
                ArrayList<Course> selectedCourses = coursesAt(availableCourses, selectedIndices);
                ArrayList<Course> coursesNeedingReplacement =
                    registerSelectedCourses(student, selectedCourses);

                while (!coursesNeedingReplacement.isEmpty()) {
                    System.out.println(
                        coursesNeedingReplacement.size()
                        + " selected course(s) conflict with the schedule. "
                        + "Choose that many new, non-conflicting courses."
                    );
                    System.out.print(
                        "Enter replacement course numbers separated by commas "
                        + "(blank to finish): "
                    );
                    String replacements = scanner.nextLine().trim();
                    if (replacements.isEmpty()) {
                        break;
                    }
                    List<Integer> replacementIndices =
                        parseCourseNumbers(replacements, availableCourses.size());
                    if (replacementIndices.size() != coursesNeedingReplacement.size()
                            || new HashSet<>(replacementIndices).size()
                                != replacementIndices.size()
                            || replacementIndices.stream()
                                .anyMatch(previouslySelectedIndices::contains)) {
                        System.out.println(
                            "Choose " + coursesNeedingReplacement.size()
                            + " different course numbers that were not already selected."
                        );
                        continue;
                    }
                    previouslySelectedIndices.addAll(replacementIndices);
                    coursesNeedingReplacement = registerSelectedCourses(
                        student,
                        coursesAt(availableCourses, replacementIndices)
                    );
                }
                return;
            } catch (NumberFormatException exception) {
                System.out.println(
                    "Enter valid course numbers from 1 to " + availableCourses.size() + "."
                );
            } catch (IllegalStateException exception) {
                System.out.println("Courses were not added: " + exception.getMessage());
            }
        }
    }

    static void manageSchedule(
            Scanner scanner,
            Student student,
            List<Course> availableCourses,
            EasternSchedule schedule
    ) throws Exception {
        while (true) {
            student.displaySchedule();
            System.out.printf(
                "Credits enrolled: %s / %d%n",
                formatNumber(student.getTotalCredits()),
                Student.MAX_CREDITS
            );
            System.out.println(
                "1. Add from current course list\n"
                + "2. Search for and add courses\n"
                + "3. Drop selected courses\n"
                + "4. Drop all courses\n"
                + "0. Finish"
            );
            System.out.print("Choose a schedule option: ");
            String selection = scanner.nextLine().trim();
            switch (selection) {
                case "1" -> addCoursesToSchedule(scanner, student, availableCourses);
                case "2" -> searchAndAddCourses(scanner, student, schedule);
                case "3" -> dropSelectedCourses(scanner, student);
                case "4" -> {
                    student.dropAllCourses();
                    System.out.println("All courses dropped.");
                }
                case "0" -> {
                    student.displaySchedule();
                    return;
                }
                default -> System.out.println("Choose an option from 0 to 4.");
            }
        }
    }

    private static void searchAndAddCourses(
            Scanner scanner,
            Student student,
            EasternSchedule schedule
    ) throws Exception {
        System.out.print("Enter a subject code (for example, CSC): ");
        String subject = scanner.nextLine().trim();
        System.out.print("Enter a course number (leave blank for all): ");
        String courseNumber = scanner.nextLine().trim();
        System.out.print("Enter an instructor name (leave blank for all): ");
        String instructor = scanner.nextLine().trim();
        List<Course> courses =
            searchAndDisplayCourses(subject, courseNumber, instructor, schedule);
        addCoursesToSchedule(scanner, student, courses);
    }

    private static void dropSelectedCourses(Scanner scanner, Student student) {
        List<Course> enrolledCourses = student.getCourses();
        if (enrolledCourses.isEmpty()) {
            System.out.println("There are no courses to drop.");
            return;
        }
        System.out.println("Courses currently on the schedule:");
        for (int i = 0; i < enrolledCourses.size(); i++) {
            Course course = enrolledCourses.get(i);
            System.out.printf(
                "%d. %s %s-%s%n",
                i + 1,
                course.subject(),
                course.courseNumber(),
                course.sequenceNumber()
            );
        }
        System.out.print("Enter course numbers to drop, separated by commas: ");
        String selection = scanner.nextLine().trim();
        if (selection.isEmpty()) {
            return;
        }
        try {
            List<Integer> indices = parseCourseNumbers(selection, enrolledCourses.size());
            ArrayList<Course> selectedCourses = coursesAt(enrolledCourses, indices);
            int droppedCount = student.dropCourses(selectedCourses.toArray(Course[]::new));
            System.out.println(droppedCount + " course(s) dropped.");
        } catch (NumberFormatException exception) {
            System.out.println(
                "Enter distinct course numbers from 1 to " + enrolledCourses.size() + "."
            );
        }
    }

    private static ArrayList<Course> registerSelectedCourses(
            Student student,
            List<Course> selectedCourses
    ) {
        ArrayList<Course> addedFromSelection = new ArrayList<>();
        ArrayList<Course> coursesNeedingReplacement = new ArrayList<>();
        for (Course course : selectedCourses) {
            try {
                if (student.addCourse(course)) {
                    addedFromSelection.add(course);
                }
            } catch (Student.CourseConflictException exception) {
                System.out.println("Cannot add course: " + exception.getMessage());
                Course conflictingCourse = exception.getConflictingCourse();
                if (addedFromSelection.remove(conflictingCourse)) {
                    student.dropCourse(conflictingCourse);
                    addIfAbsent(coursesNeedingReplacement, conflictingCourse);
                }
                addIfAbsent(coursesNeedingReplacement, course);
            } catch (IllegalStateException exception) {
                System.out.println("Cannot add course: " + exception.getMessage());
            }
        }
        if (!addedFromSelection.isEmpty()) {
            System.out.println(
                addedFromSelection.size() + " non-conflicting course(s) added to the schedule."
            );
        }
        return coursesNeedingReplacement;
    }

    private static void addIfAbsent(List<Course> courses, Course course) {
        if (!courses.contains(course)) {
            courses.add(course);
        }
    }

    private static List<Integer> parseCourseNumbers(String selection, int courseCount) {
        ArrayList<Integer> indices = new ArrayList<>();
        for (String selectedNumber : selection.split(",")) {
            int index = Integer.parseInt(selectedNumber.trim()) - 1;
            if (index < 0 || index >= courseCount) {
                throw new NumberFormatException();
            }
            indices.add(index);
        }
        if (new HashSet<>(indices).size() != indices.size()) {
            throw new NumberFormatException();
        }
        return indices;
    }

    private static ArrayList<Course> coursesAt(
            List<Course> availableCourses,
            List<Integer> indices
    ) {
        ArrayList<Course> selectedCourses = new ArrayList<>();
        for (int index : indices) {
            selectedCourses.add(availableCourses.get(index));
        }
        return selectedCourses;
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