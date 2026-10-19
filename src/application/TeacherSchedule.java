package application;


public class TeacherSchedule {
    private String id;
    private String name;
    private String department;
    private String email;
    private String teacher_password;

    public TeacherSchedule(String id, String name, String department, String email, String teacher_password) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.email = email;
        this.teacher_password = teacher_password;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }

    public String getEmail() {
        return email;
    }

    public String getTeacherPassword() {
        return teacher_password;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void displayInfo() {
        System.out.println("================================");
        System.out.println("Teacher UID : " + id);
        System.out.println("Name        : " + name);
        System.out.println("Department  : " + department);
        System.out.println("Email       : " + email);
        System.out.println("================================");
    }
}