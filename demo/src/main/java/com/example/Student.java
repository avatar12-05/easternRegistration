package com.example;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A student and the courses in which they are enrolled.
 */
public class Student {

    public static final int MAX_CREDITS = 16;
    private static final DateTimeFormatter COURSE_TIME_FORMAT =
        DateTimeFormatter.ofPattern("HHmm");
    private static final DateTimeFormatter DISPLAY_TIME_FORMAT =
        DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    private final String id;
    private final String firstName;
    private final String lastName;
    private final ArrayList<Course> courses = new ArrayList<>();

    public static class CourseConflictException extends IllegalStateException {
        private static final long serialVersionUID = 1L;

        private final Course conflictingCourse;

        CourseConflictException(Course conflictingCourse) {
            super("Course conflicts with " + courseName(conflictingCourse) + ".");
            this.conflictingCourse = conflictingCourse;
        }

        public Course getConflictingCourse() {
            return conflictingCourse;
        }

        private static String courseName(Course course) {
            return course.subject() + " " + course.courseNumber()
                + "-" + course.sequenceNumber();
        }
    }

    /**
     * Creates a student with the specified identity.
     *
     * @param id the student's ID
     * @param firstName the student's first name
     * @param lastName the student's last name
     */
    public Student(String id, String firstName, String lastName) {
        this.id = requireValue(id, "ID");
        this.firstName = requireValue(firstName, "First name");
        this.lastName = requireValue(lastName, "Last name");
    }

    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    /**
     * Returns the courses currently associated with this student.
     *
     * @return an immutable snapshot of the student's courses
     */
    public List<Course> getCourses() {
        return List.copyOf(courses);
    }

    public double getTotalCredits() {
        return courses.stream().mapToDouble(Student::courseCredits).sum();
    }

    /**
     * Adds a course if it has seats available, fits within the credit limit,
     * and does not conflict with an enrolled course.
     *
     * @param course the course to add
     * @return true if the course was added; false if it was already present
     * @throws IllegalStateException if the course is full, exceeds the credit
     *         limit, or conflicts with an enrolled course
     */
    public boolean addCourse(Course course) {
        Objects.requireNonNull(course, "Course cannot be null.");
        if (containsCourse(course)) {
            return false;
        }
        validateCourseForRegistration(course, courses);
        return courses.add(course);
    }

    /**
     * Adds multiple courses if all can be registered. The operation is atomic:
     * if any new course cannot be added, none of the requested courses are added.
     * Courses already enrolled or duplicated in the request are ignored.
     *
     * @param requestedCourses courses to add
     * @return the number of courses added
     * @throws NullPointerException if the array or any course is null
     * @throws IllegalStateException if a requested course is full, exceeds the
     *         credit limit, or conflicts with an enrolled or requested course
     */
    public int addCourses(Course... requestedCourses) {
        Objects.requireNonNull(requestedCourses, "Courses cannot be null.");
        ArrayList<Course> additions = new ArrayList<>();
        ArrayList<Course> proposedCourses = new ArrayList<>(courses);
        for (Course course : requestedCourses) {
            Objects.requireNonNull(course, "Course cannot be null.");
            if (containsCourse(proposedCourses, course)) {
                continue;
            }
            validateCourseForRegistration(course, proposedCourses);
            proposedCourses.add(course);
            additions.add(course);
        }
        courses.addAll(additions);
        return additions.size();
    }

    /**
     * Drops a course if the student is currently enrolled in it.
     *
     * @param course the course to drop
     * @return true if the course was dropped; false if it was not present
     */
    public boolean dropCourse(Course course) {
        Objects.requireNonNull(course, "Course cannot be null.");
        for (int i = 0; i < courses.size(); i++) {
            if (isSameCourse(courses.get(i), course)) {
                courses.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Drops each specified course that the student is currently taking.
     *
     * @param courses the courses to drop
     * @return the number of courses dropped
     * @throws NullPointerException if the array or any course is null
     */
    public int dropCourses(Course... courses) {
        Objects.requireNonNull(courses, "Courses cannot be null.");
        int droppedCount = 0;
        for (Course course : courses) {
            if (dropCourse(course)) {
                droppedCount++;
            }
        }
        return droppedCount;
    }

    /**
     * Drops all courses currently taken by this student.
     */
    public void dropAllCourses() {
        courses.clear();
    }

    /**
     * Displays this student's current class schedule.
     */
    public void displaySchedule() {
        System.out.printf(
            "Schedule for %s %s (ID: %s):%n",
            firstName,
            lastName,
            id
        );
        if (courses.isEmpty()) {
            System.out.println("No courses are currently enrolled.");
            return;
        }

        for (Course course : courses) {
            System.out.printf(
                "%s %s-%s, %s credits, %s%n",
                valueOr(course.subject(), "Subject TBA"),
                valueOr(course.courseNumber(), "Number TBA"),
                valueOr(course.sequenceNumber(), "Section TBA"),
                formatCourseCredits(course),
                valueOr(course.courseTitle(), "Title TBA").replace("&amp;", "&")
            );
            if (course.meetingsFaculty() == null || course.meetingsFaculty().isEmpty()) {
                System.out.println("  Days TBA, Time TBA, Instructor: TBA");
                continue;
            }
            for (Course.MeetingFaculty meeting : course.meetingsFaculty()) {
                Course.MeetingTime time = meeting == null ? null : meeting.meetingTime();
                List<Course.Faculty> faculty =
                    meeting != null && meeting.faculty() != null
                        && !meeting.faculty().isEmpty()
                        ? meeting.faculty()
                        : course.faculty();
                System.out.printf(
                    "  %s, Instructor: %s%n",
                    formatMeeting(time),
                    formatInstructors(faculty)
                );
            }
        }
    }

    private void validateCourseForRegistration(Course course, List<Course> registeredCourses) {
        if (isFull(course)) {
            throw new IllegalStateException("Course is full.");
        }
        double courseCredits = courseCredits(course);
        double registeredCredits = registeredCourses.stream()
            .mapToDouble(Student::courseCredits)
            .sum();
        if (registeredCredits + courseCredits > MAX_CREDITS) {
            throw new IllegalStateException(
                "Adding this course would exceed the " + MAX_CREDITS + "-credit limit."
            );
        }
        for (Course existing : registeredCourses) {
            if (hasTimeConflict(existing, course)) {
                throw new CourseConflictException(existing);
            }
        }
    }

    private boolean containsCourse(Course course) {
        return containsCourse(courses, course);
    }

    private boolean containsCourse(List<Course> registeredCourses, Course course) {
        return registeredCourses.stream().anyMatch(
            existing -> isSameCourse(existing, course)
        );
    }

    private boolean isFull(Course course) {
        if (course.seatsAvailable() != null) {
            return course.seatsAvailable() <= 0;
        }
        return course.maximumEnrollment() != null && course.enrollment() != null
            && course.enrollment() >= course.maximumEnrollment();
    }

    private static double courseCredits(Course course) {
        Double credits = course.creditHours();
        if (credits == null) {
            credits = course.creditHourHigh() != null
                ? course.creditHourHigh()
                : course.creditHourLow();
        }
        if (credits == null || !Double.isFinite(credits) || credits <= 0) {
            throw new IllegalArgumentException(
                "Course credit hours must be available and greater than zero."
            );
        }
        return credits;
    }

    private boolean hasTimeConflict(Course first, Course second) {
        if (first.meetingsFaculty() == null || second.meetingsFaculty() == null) {
            return false;
        }
        for (Course.MeetingFaculty firstMeeting : first.meetingsFaculty()) {
            if (firstMeeting == null || firstMeeting.meetingTime() == null) {
                continue;
            }
            for (Course.MeetingFaculty secondMeeting : second.meetingsFaculty()) {
                if (secondMeeting != null && secondMeeting.meetingTime() != null
                        && meetingsConflict(
                            firstMeeting.meetingTime(),
                            secondMeeting.meetingTime()
                        )) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean meetingsConflict(
            Course.MeetingTime first,
            Course.MeetingTime second
    ) {
        if (!shareMeetingDay(first, second)
                || isMissing(first.beginTime()) || isMissing(first.endTime())
                || isMissing(second.beginTime()) || isMissing(second.endTime())) {
            return false;
        }
        try {
            LocalTime firstStart = LocalTime.parse(first.beginTime(), COURSE_TIME_FORMAT);
            LocalTime firstEnd = LocalTime.parse(first.endTime(), COURSE_TIME_FORMAT);
            LocalTime secondStart = LocalTime.parse(second.beginTime(), COURSE_TIME_FORMAT);
            LocalTime secondEnd = LocalTime.parse(second.endTime(), COURSE_TIME_FORMAT);
            return firstStart.isBefore(secondEnd) && secondStart.isBefore(firstEnd);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Course meeting time is invalid.", exception);
        }
    }

    private boolean shareMeetingDay(Course.MeetingTime first, Course.MeetingTime second) {
        return first.monday() && second.monday()
            || first.tuesday() && second.tuesday()
            || first.wednesday() && second.wednesday()
            || first.thursday() && second.thursday()
            || first.friday() && second.friday()
            || first.saturday() && second.saturday()
            || first.sunday() && second.sunday();
    }

    private boolean isMissing(String time) {
        return time == null || time.isBlank();
    }

    private String formatCourseCredits(Course course) {
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
        String formatted = formatNumber(credits);
        return maximum != null && Double.compare(credits, maximum) != 0
            ? formatted + "-" + formatNumber(maximum)
            : formatted;
    }

    private String formatNumber(double number) {
        return java.math.BigDecimal.valueOf(number).stripTrailingZeros().toPlainString();
    }

    private String formatMeeting(Course.MeetingTime meeting) {
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
        if (isMissing(meeting.beginTime()) || isMissing(meeting.endTime())) {
            return daysText + ", Time TBA";
        }
        try {
            LocalTime start = LocalTime.parse(meeting.beginTime(), COURSE_TIME_FORMAT);
            LocalTime end = LocalTime.parse(meeting.endTime(), COURSE_TIME_FORMAT);
            String startText = start.format(DISPLAY_TIME_FORMAT);
            if (start.getHour() / 12 == end.getHour() / 12) {
                startText = startText.replaceFirst(" (AM|PM)$", "");
            }
            return daysText + ", " + startText + " - " + end.format(DISPLAY_TIME_FORMAT);
        } catch (DateTimeParseException exception) {
            return daysText + ", " + meeting.beginTime() + " - " + meeting.endTime();
        }
    }

    private String formatInstructors(List<Course.Faculty> faculty) {
        if (faculty == null || faculty.isEmpty()) {
            return "TBA";
        }
        List<String> names = faculty.stream()
            .filter(Objects::nonNull)
            .map(Course.Faculty::displayName)
            .filter(name -> name != null && !name.isBlank())
            .toList();
        return names.isEmpty() ? "TBA" : String.join(", ", names);
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean isSameCourse(Course first, Course second) {
        if (first.id() != null && !first.id().isBlank()
                && second.id() != null && !second.id().isBlank()) {
            return first.id().equals(second.id());
        }
        return first.equals(second);
    }

    private static String requireValue(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank.");
        }
        return value.trim();
    }
}