package application;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SupabaseService {

    private static final String URL = "https://cklslhrmmrbqfrgvkcxd.supabase.co";
    private static final String KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNrbHNsaHJtbXJicWZyZ3ZrY3hkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTEyNzgzMDYsImV4cCI6MjEwNjg1NDMwNn0.J62tlrAD9wF0mNaKcOQcnyjwIVMAaGxZrm2_5lvsJXQ";

    private static final HttpClient client = HttpClient.newHttpClient();

    private static String sendRequest(String endpoint, String method, String body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(URL + endpoint))
                    .header("apikey", KEY)
                    .header("Authorization", "Bearer " + KEY);

            if (body != null) {
                builder.header("Content-Type", "application/json");

                if (!"GET".equalsIgnoreCase(method)) {
                    builder.header("Prefer", "return=representation");
                }

                builder.method(method, HttpRequest.BodyPublishers.ofString(body));
            } else {
                builder.GET();
            }

            HttpResponse<String> response = client.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            } else {
                System.out.println("\n[Supabase API Error Code: " + response.statusCode() + "]: " + response.body());
                return null;
            }
        } catch (Exception e) {
            System.out.println("\n[Connection Error]: " + e.getMessage());
            return null;
        }
    }

    public static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static List<String> extractJsonObjects(String json) {
        List<String> objects = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) {
            return objects;
        }

        Pattern pattern = Pattern.compile("\\{[^{}]*\\}");
        Matcher matcher = pattern.matcher(json);

        while (matcher.find()) {
            objects.add(matcher.group());
        }
        return objects;
    }

    public static String getString(String json, String key) {
        if (json == null) {
            return "";
        }
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
        Matcher matcher = Pattern.compile(pattern).matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }

        pattern = "\"" + key + "\"\\s*:\\s*([^,}\\\"]+)";
        matcher = Pattern.compile(pattern).matcher(json);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return "";
    }

    public static int getInt(String json, String key) {
        try {
            return Integer.parseInt(getString(json, key));
        } catch (Exception e) {
            return 0;
        }
    }

    public static double getDouble(String json, String key) {
        try {
            return Double.parseDouble(getString(json, key));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static String getOrDefault(String endpoint) {
        String response = sendRequest(endpoint, "GET", null);
        return response != null ? response : "[]";
    }

    // ==========================================
    // AUTHENTICATION & LOGIN
    // ==========================================
    public static boolean signUp(String email, String password, String name, String department) {
        String authJson = String.format(
                "{\"email\":\"%s\",\"password\":\"%s\",\"data\":{\"name\":\"%s\",\"role\":\"Student\",\"department\":\"%s\"}}",
                escapeJson(email), escapeJson(password), escapeJson(name), escapeJson(department)
        );

        String response = sendRequest("/auth/v1/signup", "POST", authJson);
        if (response == null) {
            return false;
        }

        String uid = getString(response, "id");
        if (uid.isEmpty()) {
            return false;
        }

        return addStudentToCloud(uid, name, email, department, 0.0);
    }

    public static boolean teacherSignUp(String email, String password, String name, String department) {
        String authJson = String.format(
                "{\"email\":\"%s\",\"password\":\"%s\",\"data\":{\"name\":\"%s\",\"role\":\"Teacher\",\"department\":\"%s\"}}",
                escapeJson(email), escapeJson(password), escapeJson(name), escapeJson(department)
        );

        String response = sendRequest("/auth/v1/signup", "POST", authJson);
        if (response == null) {
            return false;
        }

        String uid = getString(response, "id");
        if (uid.isEmpty()) {
            return false;
        }

        String teacherJson = String.format(
                "{\"id\":\"%s\",\"name\":\"%s\",\"email\":\"%s\",\"department\":\"%s\"}",
                escapeJson(uid), escapeJson(name), escapeJson(email), escapeJson(department)
        );

        return sendRequest("/rest/v1/teachers", "POST", teacherJson) != null;
    }

    public static boolean login(String email, String password) {
        String json = String.format(
                "{\"email\":\"%s\",\"password\":\"%s\"}",
                escapeJson(email), escapeJson(password)
        );
        return sendRequest("/auth/v1/token?grant_type=password", "POST", json) != null;
    }

    public static String getEmailByUidFromCloud(String inputIdentifier) {
        if (inputIdentifier.contains("@")) {
            return inputIdentifier;
        }

        String studentJson = searchStudentByIdFromCloud(inputIdentifier);
        List<String> sObjs = extractJsonObjects(studentJson);
        if (!sObjs.isEmpty()) {
            return getString(sObjs.get(0), "email");
        }

        String teacherJson = searchTeacherByIdFromCloud(inputIdentifier);
        List<String> tObjs = extractJsonObjects(teacherJson);
        if (!tObjs.isEmpty()) {
            return getString(tObjs.get(0), "email");
        }

        return inputIdentifier;
    }

    // ==========================================
    // 1. STUDENTS (Table: students)
    // ==========================================
    public static boolean addStudentToCloud(String id, String name, String email, String department, double cgpa) {
        String json = String.format(
                "{\"id\":\"%s\",\"name\":\"%s\",\"email\":\"%s\",\"department\":\"%s\",\"cgpa\":%.2f}",
                escapeJson(id), escapeJson(name), escapeJson(email), escapeJson(department), cgpa
        );
        return sendRequest("/rest/v1/students", "POST", json) != null;
    }

    public static String searchStudentByIdFromCloud(String id) {
        return getOrDefault("/rest/v1/students?id=eq." + encode(id) + "&select=*");
    }

    public static String searchStudentByEmail(String email) {
        return getOrDefault("/rest/v1/students?email=eq." + encode(email) + "&select=*");
    }

    public static String getStudentProfile(String email) {
        return searchStudentByEmail(email);
    }

    public static String getAllStudentsFromCloud() {
        return getOrDefault("/rest/v1/students?select=*&order=id.asc");
    }

    public static boolean updateStudent(String id, String name, String department, double cgpa) {
        String json = String.format(
                "{\"name\":\"%s\",\"department\":\"%s\",\"cgpa\":%.2f}",
                escapeJson(name), escapeJson(department), cgpa
        );
        return sendRequest("/rest/v1/students?id=eq." + encode(id), "PATCH", json) != null;
    }

    public static boolean deleteStudent(String id) {
        return sendRequest("/rest/v1/students?id=eq." + encode(id), "DELETE", "") != null;
    }

    // ==========================================
    // 2. TEACHERS (Table: teachers)
    // ==========================================
    public static String searchTeacherByIdFromCloud(String id) {
        return getOrDefault("/rest/v1/teachers?id=eq." + encode(id) + "&select=*");
    }

    public static String getTeacherProfile(String email) {
        return getOrDefault("/rest/v1/teachers?email=eq." + encode(email) + "&select=*");
    }

    public static String getAllTeachersFromCloud() {
        return getOrDefault("/rest/v1/teachers?select=*&order=id.asc");
    }

    // ==========================================
    // 3. COURSES MANAGE (Table: courses_manage)
    // ==========================================
    public static boolean addCourseToCloud(String courseCode, String courseName, int semester, String department) {
        String json = String.format(
                "{\"course_code\":\"%s\",\"course_name\":\"%s\",\"semester\":%d,\"department\":\"%s\"}",
                escapeJson(courseCode), escapeJson(courseName), semester, escapeJson(department)
        );
        return sendRequest("/rest/v1/courses_manage", "POST", json) != null;
    }

    public static String getCoursesBySemesterFromCloud(int semester) {
        return getOrDefault("/rest/v1/courses_manage?semester=eq." + semester + "&student_email=is.null&select=*&order=id.asc");
    }

    public static String getAllCoursesFromCloud() {
        return getOrDefault("/rest/v1/courses_manage?student_email=is.null&select=*&order=semester.asc,id.asc");
    }

    public static boolean deleteCourseFromCloud(int courseId) {
        return sendRequest("/rest/v1/courses_manage?id=eq." + courseId, "DELETE", "") != null;
    }

    public static boolean assignCourseToTeacherInCloud(String teacherId, String teacherEmail, String courseCode, String courseName) {
        String json = String.format(
                "{\"teacher_email\":\"%s\",\"teacher_name\":\"%s\"}",
                escapeJson(teacherEmail), escapeJson(courseName)
        );
        return sendRequest("/rest/v1/courses_manage?course_code=eq." + encode(courseCode) + "&student_email=is.null", "PATCH", json) != null;
    }

    public static String getTeacherAssignedCoursesFromCloud(String teacherEmail) {
        return getOrDefault("/rest/v1/courses_manage?teacher_email=eq." + encode(teacherEmail) + "&student_email=is.null&select=*&order=id.asc");
    }

    public static boolean enrollStudentCourseInCloud(String studentId, String studentEmail, int semester, String courseCode, String courseName) {
        String json = String.format(
                "{\"student_email\":\"%s\",\"semester\":%d,\"course_code\":\"%s\",\"course_name\":\"%s\",\"department\":\"CS\"}",
                escapeJson(studentEmail), semester, escapeJson(courseCode), escapeJson(courseName)
        );
        return sendRequest("/rest/v1/courses_manage", "POST", json) != null;
    }

    public static String getStudentEnrolledCoursesFromCloud(String studentEmail) {
        return getOrDefault("/rest/v1/courses_manage?student_email=eq." + encode(studentEmail) + "&select=*&order=semester.asc,id.asc");
    }

    public static boolean deleteStudentEnrolledCoursesInCloud(String studentEmail, int semester) {
        return sendRequest("/rest/v1/courses_manage?student_email=eq." + encode(studentEmail) + "&semester=eq." + semester, "DELETE", "") != null;
    }

    public static int getEnrolledStudentCountForCourse(String courseCode) {
        String json = getOrDefault("/rest/v1/courses_manage?course_code=eq." + encode(courseCode) + "&student_email=not.is.null&select=id");
        return extractJsonObjects(json).size();
    }

    // ==========================================
    // 4. ANNOUNCEMENTS / NOTICES (Table: announcements)
    // ==========================================
    public static boolean postAnnouncementToCloud(String title, String message, String targetRole) {
        String json = String.format(
                "{\"title\":\"%s\",\"message\":\"%s\",\"target_role\":\"%s\"}",
                escapeJson(title), escapeJson(message), escapeJson(targetRole)
        );
        return sendRequest("/rest/v1/announcements", "POST", json) != null;
    }

    public static String getAllAnnouncementsFromCloud() {
        return getOrDefault("/rest/v1/announcements?select=*&order=id.desc");
    }

    public static boolean deleteAnnouncementFromCloud(int id) {
        return sendRequest("/rest/v1/announcements?id=eq." + id, "DELETE", "") != null;
    }

    public static void displayAnnouncements(String role) {
        String json = getAllAnnouncementsFromCloud();
        List<String> records = extractJsonObjects(json);

        System.out.println("\n=================================");
        System.out.println("     NOTICE BOARD / ANNOUNCEMENTS");
        System.out.println("=================================");

        int count = 0;
        for (String rec : records) {
            String targetRole = getString(rec, "target_role");
            if (role.equalsIgnoreCase("ADMIN") || targetRole.equalsIgnoreCase("ALL") || targetRole.equalsIgnoreCase(role)) {
                int id = getInt(rec, "id");
                String title = getString(rec, "title");
                String message = getString(rec, "message");
                System.out.println("[" + id + "] " + title.toUpperCase() + " (For: " + targetRole + ")");
                System.out.println("Message: " + message);
                System.out.println("---------------------------------");
                count++;
            }
        }

        if (count == 0) {
            System.out.println("No announcements available right now.");
        }
        System.out.println("=================================");
    }

    // ==========================================
    // DISPLAY HELPER METHODS
    // ==========================================
    public static void displayFormattedProfile(String json, String type) {
        List<String> objects = extractJsonObjects(json);
        if (objects.isEmpty()) {
            System.out.println(type + " profile not found.");
            return;
        }

        String profile = objects.get(0);
        System.out.println("\n=================================");
        System.out.println(type.toUpperCase() + " PROFILE");
        System.out.println("=================================");
        System.out.println("UID         : " + getString(profile, "id"));
        System.out.println("Name        : " + getString(profile, "name"));
        System.out.println("Email       : " + getString(profile, "email"));
        System.out.println("Department  : " + getString(profile, "department"));

        if (type.equalsIgnoreCase("Student")) {
            double cgpaVal = getDouble(profile, "cgpa");
            if (cgpaVal > 0) {
                System.out.println("CGPA        : " + cgpaVal);
            }
        }
        System.out.println("=================================");
    }

    public static void displayFormattedTeacherCourses(String json) {
        List<String> objects = extractJsonObjects(json);
        if (objects.isEmpty()) {
            System.out.println("\nNo courses assigned to you by Admin yet.");
            return;
        }

        System.out.println("\n=================================");
        System.out.println("     YOUR ASSIGNED COURSES");
        System.out.println("=================================");
        for (String record : objects) {
            String code = getString(record, "course_code");
            String name = getString(record, "course_name");
            int sem = getInt(record, "semester");
            System.out.println(" - [Semester " + sem + "] [" + code + "] " + name);
        }
        System.out.println("=================================");
    }

    public static void displayFormattedStudentCourses(String json) {
        List<String> objects = extractJsonObjects(json);
        if (objects.isEmpty()) {
            System.out.println("\nNo registered courses found for your account.");
            return;
        }

        System.out.println("\n=================================");
        System.out.println("       YOUR ENROLLED COURSES");
        System.out.println("=================================");
        int currentSem = -1;
        for (String record : objects) {
            int sem = getInt(record, "semester");
            if (sem != currentSem) {
                currentSem = sem;
                System.out.println("\n--- SEMESTER " + currentSem + " ---");
            }
            String code = getString(record, "course_code");
            String name = getString(record, "course_name");
            System.out.println(" • [" + code + "] " + name);
        }
        System.out.println("=================================");
    }
}
