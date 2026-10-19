package application;


import java.util.ArrayList;
import java.util.List;

public class Admin extends Student {

    private ArrayList<Student> students;
    private ArrayList<TeacherSchedule> teachers;

    public Admin() {
        super("0", "Admin", "aneeskhan@gmail.com", "Management", 4.0, "Aneeskhan1122");
        students = new ArrayList<>();
        teachers = new ArrayList<>();
        loadStudentsFromCloud();
        loadTeachersFromCloud();
    }

    public boolean login(String email, String password) {
        return email.equalsIgnoreCase("aneeskhan@gmail.com") && password.equals("Aneeskhan1122");
    }

    public void loadStudentsFromCloud() {
        students.clear();

        String json = SupabaseService.getAllStudentsFromCloud();

        List<String> records = SupabaseService.extractJsonObjects(json);

        for (String record : records) {
            try {
                String id = SupabaseService.getString(record, "id");
                String name = SupabaseService.getString(record, "name");
                String email = SupabaseService.getString(record, "email");
                String department = SupabaseService.getString(record, "department");
                double cgpa = SupabaseService.getDouble(record, "cgpa");
                students.add(new Student(id, name, email, department, cgpa, ""));
            } catch (Exception e) {
            }
        }
    }

    public void loadTeachersFromCloud() {
        teachers.clear();
        String json = SupabaseService.getAllTeachersFromCloud();
        List<String> records = SupabaseService.extractJsonObjects(json);
        for (String record : records) {
            try {
                String id = SupabaseService.getString(record, "id");
                String name = SupabaseService.getString(record, "name");
                String email = SupabaseService.getString(record, "email");
                String department = SupabaseService.getString(record, "department");
                teachers.add(new TeacherSchedule(id, name, department, email, ""));
            } catch (Exception e) {
            }
        }
    }

    public void addStudentLocal(Student student) {
        if (student != null) {
            students.add(student);
        }
    }

    public boolean addStudent(Student student) {
        if (findStudent(student.getId()) != null || findStudentByEmail(student.getEmail()) != null) {
            return false;
        }
        boolean cloudSaved = SupabaseService.addStudentToCloud(student.getId(), student.getName(), student.getEmail(), student.getDepartment(), student.getCgpa());
        if (!cloudSaved) {
            return false;
        }
        students.add(student);
        return true;
    }

    public Student findStudent(String id) {
        for (Student student : students) {
            if (student.getId().equalsIgnoreCase(id)) {
                return student;
            }
        }
        return null;
    }

    public Student findStudentByEmail(String email) {
        for (Student student : students) {
            if (student.getEmail().equalsIgnoreCase(email)) {
                return student;
            }
        }
        return null;
    }

    public void viewAllStudents() {
        loadStudentsFromCloud();
        if (students.isEmpty()) {
            System.out.println("\nNo students found.");
            return;
        }
        System.out.println("\n========== ALL STUDENTS ==========");
        for (Student student : students) {
            student.displayInfo();
        }
    }

    public void searchStudent(String id) {
        loadStudentsFromCloud();
        Student student = findStudent(id);
        if (student == null) {
            System.out.println("\nStudent with UID " + id + " not found.");
        } else {
            System.out.println("\n========== STUDENT FOUND ==========");
            student.displayInfo();
        }
    }

    public boolean updateStudent(String id, String name, String department, double cgpa) {
        Student student = findStudent(id);
        if (student == null) {
            return false;
        }
        boolean cloudUpdated = SupabaseService.updateStudent(id, name, department, cgpa);
        if (!cloudUpdated) {
            return false;
        }
        student.setName(name);
        student.setDepartment(department);
        student.setCgpa(cgpa);
        return true;
    }

    public boolean deleteStudent(String id) {
        Student student = findStudent(id);
        if (student == null) {
            return false;
        }
        boolean cloudDeleted = SupabaseService.deleteStudent(id);
        if (!cloudDeleted) {
            return false;
        }
        students.remove(student);
        return true;
    }

    public boolean addTeacher(TeacherSchedule teacher) {
        if (findTeacher(teacher.getId()) != null || findTeacherByEmail(teacher.getEmail()) != null) {
            return false;
        }
        teachers.add(teacher);
        return true;
    }

    public TeacherSchedule findTeacher(String id) {
        for (TeacherSchedule teacher : teachers) {
            if (teacher.getId().equalsIgnoreCase(id)) {
                return teacher;
            }
        }
        return null;
    }

    public TeacherSchedule findTeacherByEmail(String email) {
        for (TeacherSchedule teacher : teachers) {
            if (teacher.getEmail().equalsIgnoreCase(email)) {
                return teacher;
            }
        }
        return null;
    }

    public void viewAllTeachers() {
        loadTeachersFromCloud();
        if (teachers.isEmpty()) {
            System.out.println("\nNo teachers found.");
            return;
        }
        System.out.println("\n========== ALL TEACHERS ==========");
        for (TeacherSchedule teacher : teachers) {
            teacher.displayInfo();
        }
    }

    public void searchTeacher(String id) {
        loadTeachersFromCloud();
        TeacherSchedule teacher = findTeacher(id);
        if (teacher == null) {
            System.out.println("\nTeacher with UID " + id + " not found.");
        } else {
            System.out.println("\n========== TEACHER FOUND ==========");
            teacher.displayInfo();
        }
    }
}