package application;

public class Student {

    private String id;
    private String name;
    private String email;
    private String department;
    private double cgpa;
    private String password;

    public Student(String id, String name, String email, String department, double cgpa, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.department = department;
        this.cgpa = cgpa;
        this.password = password;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getDepartment() {
        return department;
    }

    public double getCgpa() {
        return cgpa;
    }

    public String getPassword() {
        return password;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setCgpa(double cgpa) {
        this.cgpa = cgpa;
    }

    public void displayInfo() {
        System.out.println("================================");
        System.out.println("Student UID : " + id);
        System.out.println("Name        : " + name);
        System.out.println("Email       : " + email);
        System.out.println("Department  : " + department);
        System.out.println("CGPA        : " + cgpa);
        System.out.println("================================");
    }
}