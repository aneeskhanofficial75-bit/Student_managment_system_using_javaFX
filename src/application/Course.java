package application;

public class Course {

    private int id;
    private String courseCode;
    private String courseName;
    private int semester;
    private String department;

    public Course(int id, String courseCode, String courseName, int semester, String department) {
        this.id = id;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.semester = semester;
        this.department = department;
    }

    public int getId() {
        return id;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public int getSemester() {
        return semester;
    }

    public String getDepartment() {
        return department;
    }

    public void displayInfo() {
        System.out.println("================================");
        System.out.println("Course ID   : " + id);
        System.out.println("Course Code : " + courseCode);
        System.out.println("Course Name : " + courseName);
        System.out.println("Semester    : " + semester);
        System.out.println("Department  : " + department);
        System.out.println("================================");
    }
}