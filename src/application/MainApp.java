package application;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

public class MainApp extends Application {

    private Admin admin = new Admin();
    private Stage stage;
    private Scene loginScene;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        primaryStage.setTitle("Student/Teacher/Admin Management System");

        buildLoginScene();
        primaryStage.setScene(loginScene);
        primaryStage.show();
    }

    private void buildLoginScene() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tabLogin = new Tab("Login");
        VBox boxLogin = new VBox(12);
        boxLogin.setAlignment(Pos.CENTER);
        boxLogin.setPadding(new Insets(25));

        Label lblTitle = new Label("LOGIN SYSTEM");

        TextField txtUser = new TextField();
        txtUser.setPromptText("Enter Email or User UID");
        txtUser.setMaxWidth(300);

        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Enter Password");
        txtPass.setMaxWidth(300);

        Button btnLogin = new Button("Login");
        btnLogin.setPrefWidth(300);

        Label lblMsg = new Label();

        btnLogin.setOnAction(e -> {
            String inputId = txtUser.getText().trim();
            String pass = txtPass.getText();

            if (inputId.isEmpty() || pass.isEmpty()) {
                lblMsg.setText("Enter both UID/Email and Password.");
                return;
            }

            lblMsg.setText("Verifying credentials with Supabase...");

            String email = SupabaseService.getEmailByUidFromCloud(inputId);

            if (admin.login(email, pass)) {
                lblMsg.setText("");
                txtUser.clear();
                txtPass.clear();
                showAdminMenu();
                return;
            }

            if (SupabaseService.login(email, pass)) {
                String sProfile = SupabaseService.getStudentProfile(email);
                List<String> sObjs = SupabaseService.extractJsonObjects(sProfile);

                if (!sObjs.isEmpty()) {
                    lblMsg.setText("");
                    txtUser.clear();
                    txtPass.clear();
                    String studentId = SupabaseService.getString(sObjs.get(0), "id");
                    showStudentMenu(email, studentId);
                } else {
                    String tProfile = SupabaseService.getTeacherProfile(email);
                    List<String> tObjs = SupabaseService.extractJsonObjects(tProfile);
                    if (!tObjs.isEmpty()) {
                        lblMsg.setText("");
                        txtUser.clear();
                        txtPass.clear();
                        showTeacherMenu(email);
                    } else {
                        lblMsg.setText("Profile record not found.");
                    }
                }
            } else {
                lblMsg.setText("Invalid Email or Password.");
            }
        });

        boxLogin.getChildren().addAll(lblTitle, txtUser, txtPass, btnLogin, lblMsg);
        tabLogin.setContent(boxLogin);

        Tab tabSign = new Tab("Sign Up");
        VBox boxSign = new VBox(10);
        boxSign.setAlignment(Pos.CENTER);
        boxSign.setPadding(new Insets(20));

        ComboBox<String> cmbRole = new ComboBox<>();
        cmbRole.getItems().addAll("Student", "Teacher");
        cmbRole.setValue("Student");
        cmbRole.setPrefWidth(300);

        TextField txtName = new TextField();
        txtName.setPromptText("Enter Name");
        txtName.setMaxWidth(300);

        TextField txtDept = new TextField();
        txtDept.setPromptText("Enter Department");
        txtDept.setMaxWidth(300);

        TextField txtEmail = new TextField();
        txtEmail.setPromptText("Enter Email");
        txtEmail.setMaxWidth(300);

        PasswordField txtNewPass = new PasswordField();
        txtNewPass.setPromptText("Create Password at least 6 characters");
        txtNewPass.setMaxWidth(300);

        Button btnSign = new Button("Sign Up");
        btnSign.setPrefWidth(300);

        Label lblSignMsg = new Label();

        btnSign.setOnAction(e -> {
            String role = cmbRole.getValue();
            String name = txtName.getText().trim();
            String dept = txtDept.getText().trim();
            String email = txtEmail.getText().trim();
            String pass = txtNewPass.getText();

            if (name.isEmpty() || dept.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                lblSignMsg.setText("Please fill all fields.");
                return;
            }

            if (pass.length() < 6) {
                lblSignMsg.setText("Password must be at least 6 characters.");
                return;
            }

            lblSignMsg.setText("Registering to Supabase...");

            boolean reg = role.equals("Student")
                    ? SupabaseService.signUp(email, pass, name, dept)
                    : SupabaseService.teacherSignUp(email, pass, name, dept);

            if (reg) {
                showAlert(Alert.AlertType.INFORMATION, "Success", role + " account created successfully with Supabase UID.");
                txtName.clear();
                txtDept.clear();
                txtEmail.clear();
                txtNewPass.clear();
                lblSignMsg.setText("");
                tabPane.getSelectionModel().select(tabLogin);
            } else {
                lblSignMsg.setText("Sign Up failed.");
            }
        });

        boxSign.getChildren().addAll(new Label("SELECT ROLE FOR SIGN UP"), cmbRole, txtName, txtDept, txtEmail, txtNewPass, btnSign, lblSignMsg);
        tabSign.setContent(boxSign);

        tabPane.getTabs().addAll(tabLogin, tabSign);
        loginScene = new Scene(tabPane, 450, 480);
    }

    private void showAdminMenu() {
        BorderPane root = new BorderPane();
        root.setTop(makeTopBar("ADMIN DASHBOARD"));

        VBox menuBox = new VBox(7);
        menuBox.setPadding(new Insets(12));
        menuBox.setPrefWidth(220);

        TextArea display = new TextArea();
        display.setEditable(false);

        Button b1 = new Button("1. View All Students");
        b1.setMaxWidth(Double.MAX_VALUE);
        b1.setOnAction(e -> {
            display.setText("Loading students from Supabase...\n\n");
            String json = SupabaseService.getAllStudentsFromCloud();
            List<String> list = SupabaseService.extractJsonObjects(json);
            if (list.isEmpty()) {
                display.appendText("No students found.\n");
            } else {
                display.appendText("========== ALL STUDENTS ==========\n");
                for (String rec : list) {
                    display.appendText("UID: " + SupabaseService.getString(rec, "id") + " | " +
                            SupabaseService.getString(rec, "name") + " | " +
                            SupabaseService.getString(rec, "email") + " | " +
                            SupabaseService.getString(rec, "department") + " | CGPA: " +
                            SupabaseService.getDouble(rec, "cgpa") + "\n");
                }
            }
        });

        Button b2 = new Button("2. Search Student by UID");
        b2.setMaxWidth(Double.MAX_VALUE);
        b2.setOnAction(e -> {
            TextInputDialog d = new TextInputDialog();
            d.setTitle("Search Student");
            d.setHeaderText(null);
            d.setContentText("Enter Student UID:");
            d.showAndWait().ifPresent(uid -> {
                String json = SupabaseService.searchStudentByIdFromCloud(uid.trim());
                List<String> list = SupabaseService.extractJsonObjects(json);
                if (list.isEmpty()) {
                    display.setText("\nStudent with UID " + uid + " not found.\n");
                } else {
                    String rec = list.get(0);
                    display.setText("========== STUDENT FOUND ==========\n" +
                            "UID: " + SupabaseService.getString(rec, "id") + "\n" +
                            "Name: " + SupabaseService.getString(rec, "name") + "\n" +
                            "Email: " + SupabaseService.getString(rec, "email") + "\n" +
                            "Dept: " + SupabaseService.getString(rec, "department") + "\n" +
                            "CGPA: " + SupabaseService.getDouble(rec, "cgpa") + "\n");
                }
            });
        });

        Button b3 = new Button("3. Update Student");
        b3.setMaxWidth(Double.MAX_VALUE);
        b3.setOnAction(e -> {
            TextInputDialog d = new TextInputDialog();
            d.setHeaderText("Enter Student UID to Update:");
            d.showAndWait().ifPresent(uid -> {
                TextInputDialog dName = new TextInputDialog();
                dName.setHeaderText("Enter New Name:");
                Optional<String> rName = dName.showAndWait();

                TextInputDialog dDept = new TextInputDialog();
                dDept.setHeaderText("Enter New Department:");
                Optional<String> rDept = dDept.showAndWait();

                TextInputDialog dCgpa = new TextInputDialog();
                dCgpa.setHeaderText("Enter New CGPA:");
                Optional<String> rCgpa = dCgpa.showAndWait();

                if (rName.isPresent() && rDept.isPresent() && rCgpa.isPresent()) {
                    try {
                        double cgpa = Double.parseDouble(rCgpa.get());
                        boolean ok = SupabaseService.updateStudent(uid, rName.get(), rDept.get(), cgpa);
                        display.setText(ok ? "Student updated successfully!\n" : "Student update failed.\n");
                    } catch (Exception ex) {
                        display.setText("Invalid CGPA.\n");
                    }
                }
            });
        });

        Button b4 = new Button("4. Delete Student");
        b4.setMaxWidth(Double.MAX_VALUE);
        b4.setOnAction(e -> {
            TextInputDialog d = new TextInputDialog();
            d.setHeaderText("Enter Student UID to Delete:");
            d.showAndWait().ifPresent(uid -> {
                Alert c = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete student " + uid + "?", ButtonType.YES, ButtonType.NO);
                c.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.YES) {
                        boolean ok = SupabaseService.deleteStudent(uid);
                        display.setText(ok ? "Student deleted successfully!\n" : "Student deletion failed.\n");
                    }
                });
            });
        });

        Button b5 = new Button("5. View All Teachers");
        b5.setMaxWidth(Double.MAX_VALUE);
        b5.setOnAction(e -> {
            display.setText("Loading teachers from Supabase...\n\n");
            String json = SupabaseService.getAllTeachersFromCloud();
            List<String> list = SupabaseService.extractJsonObjects(json);
            if (list.isEmpty()) {
                display.appendText("No teachers found.\n");
            } else {
                display.appendText("========== ALL TEACHERS ==========\n");
                for (String rec : list) {
                    display.appendText("UID: " + SupabaseService.getString(rec, "id") + " | " +
                            SupabaseService.getString(rec, "name") + " | " +
                            SupabaseService.getString(rec, "department") + " | " +
                            SupabaseService.getString(rec, "email") + "\n");
                }
            }
        });

        Button b6 = new Button("6. Search Teacher by UID");
        b6.setMaxWidth(Double.MAX_VALUE);
        b6.setOnAction(e -> {
            TextInputDialog d = new TextInputDialog();
            d.setHeaderText("Enter Teacher UID:");
            d.showAndWait().ifPresent(uid -> {
                String json = SupabaseService.searchTeacherByIdFromCloud(uid.trim());
                List<String> list = SupabaseService.extractJsonObjects(json);
                if (list.isEmpty()) {
                    display.setText("\nTeacher with UID " + uid + " not found.\n");
                } else {
                    String rec = list.get(0);
                    display.setText("========== TEACHER FOUND ==========\n" +
                            "UID: " + SupabaseService.getString(rec, "id") + "\n" +
                            "Name: " + SupabaseService.getString(rec, "name") + "\n" +
                            "Dept: " + SupabaseService.getString(rec, "department") + "\n" +
                            "Email: " + SupabaseService.getString(rec, "email") + "\n");
                }
            });
        });

        Button b7 = new Button("7. Assign Course to Teacher");
        b7.setMaxWidth(Double.MAX_VALUE);
        b7.setOnAction(e -> {
            TextInputDialog dEmail = new TextInputDialog();
            dEmail.setHeaderText("Enter Teacher Email:");
            Optional<String> rEmail = dEmail.showAndWait();

            TextInputDialog dCode = new TextInputDialog();
            dCode.setHeaderText("Enter Course Code (e.g. CS101):");
            Optional<String> rCode = dCode.showAndWait();

            TextInputDialog dName = new TextInputDialog();
            dName.setHeaderText("Enter Course Name:");
            Optional<String> rName = dName.showAndWait();

            if (rEmail.isPresent() && rCode.isPresent() && rName.isPresent()) {
                boolean ok = SupabaseService.assignCourseToTeacherInCloud("", rEmail.get(), rCode.get(), rName.get());
                display.setText(ok ? "Course assigned successfully!\n" : "Failed to assign course.\n");
            }
        });

        Button b8ViewCourses = new Button("8. View All Courses");
        b8ViewCourses.setMaxWidth(Double.MAX_VALUE);
        b8ViewCourses.setOnAction(e -> {
            display.setText("========== ALL AVAILABLE COURSES ==========\n");
            String json = SupabaseService.getAllCoursesFromCloud();
            List<String> list = SupabaseService.extractJsonObjects(json);
            if (list.isEmpty()) {
                display.appendText("No courses found.\n");
            } else {
                for (String rec : list) {
                    display.appendText("ID: " + SupabaseService.getInt(rec, "id") +
                            " | Semester: " + SupabaseService.getInt(rec, "semester") +
                            " | [" + SupabaseService.getString(rec, "course_code") + "] " +
                            SupabaseService.getString(rec, "course_name") + " (" +
                            SupabaseService.getString(rec, "department") + ")\n");
                }
            }
        });

        Button b8AddCourse = new Button("Add New Course");
        b8AddCourse.setMaxWidth(Double.MAX_VALUE);
        b8AddCourse.setOnAction(e -> {
            Dialog<Boolean> dialog = new Dialog<>();
            dialog.setTitle("Add Course to Semester");
            dialog.setHeaderText("Enter Course Details:");
            ButtonType btnAdd = new ButtonType("Add Course", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(btnAdd, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            TextField txtSem = new TextField();
            txtSem.setPromptText("1 to 8");
            TextField txtCode = new TextField();
            txtCode.setPromptText("e.g. CS101");
            TextField txtCName = new TextField();
            txtCName.setPromptText("Course Name");
            TextField txtDept = new TextField();
            txtDept.setPromptText("Department");

            grid.add(new Label("Semester (1-8):"), 0, 0);
            grid.add(txtSem, 1, 0);
            grid.add(new Label("Course Code:"), 0, 1);
            grid.add(txtCode, 1, 1);
            grid.add(new Label("Course Name:"), 0, 2);
            grid.add(txtCName, 1, 2);
            grid.add(new Label("Department:"), 0, 3);
            grid.add(txtDept, 1, 3);

            dialog.getDialogPane().setContent(grid);
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == btnAdd) {
                    try {
                        int sem = Integer.parseInt(txtSem.getText().trim());
                        if (sem < 1 || sem > 8) {
                            display.setText("Invalid Semester Number (Must be 1-8).\n");
                            return false;
                        }
                        boolean ok = SupabaseService.addCourseToCloud(txtCode.getText().trim(), txtCName.getText().trim(), sem, txtDept.getText().trim());
                        display.setText(ok ? "Course added successfully for Semester " + sem + "!\n" : "Failed to add course.\n");
                        return ok;
                    } catch (Exception ex) {
                        display.setText("Invalid semester input.\n");
                        return false;
                    }
                }
                return false;
            });
            dialog.showAndWait();
        });

        Button b8DeleteCourse = new Button("Delete Course");
        b8DeleteCourse.setMaxWidth(Double.MAX_VALUE);
        b8DeleteCourse.setOnAction(e -> {
            TextInputDialog d = new TextInputDialog();
            d.setHeaderText("Enter Course ID to delete:");
            d.showAndWait().ifPresent(idStr -> {
                try {
                    int cId = Integer.parseInt(idStr.trim());
                    boolean ok = SupabaseService.deleteCourseFromCloud(cId);
                    display.setText(ok ? "Course deleted successfully!\n" : "Failed to delete course.\n");
                } catch (Exception ex) {
                    display.setText("Invalid Course ID.\n");
                }
            });
        });

        Button b9 = new Button("9. Announcements");
        b9.setMaxWidth(Double.MAX_VALUE);
        b9.setOnAction(e -> {
            display.setText("========== ANNOUNCEMENTS ==========\n");
            String json = SupabaseService.getAllAnnouncementsFromCloud();
            List<String> list = SupabaseService.extractJsonObjects(json);
            for (String rec : list) {
                display.appendText("[" + SupabaseService.getInt(rec, "id") + "] " +
                        SupabaseService.getString(rec, "title") + " (For: " +
                        SupabaseService.getString(rec, "target_role") + ")\n" +
                        "Message: " + SupabaseService.getString(rec, "message") + "\n---------------------------------\n");
            }
        });

        menuBox.getChildren().addAll(b1, b2, b3, b4, b5, b6, b7, b8ViewCourses, b8AddCourse, b8DeleteCourse, b9);
        root.setLeft(menuBox);
        root.setCenter(display);

        stage.setScene(new Scene(root, 850, 580));
    }

    private void showStudentMenu(String email, String studentId) {
        BorderPane root = new BorderPane();
        root.setTop(makeTopBar("STUDENT DASHBOARD (" + email + ")"));

        VBox menuBox = new VBox(8);
        menuBox.setPadding(new Insets(15));
        menuBox.setPrefWidth(240);

        TextArea display = new TextArea();
        display.setEditable(false);

        Button b1 = new Button("1. My Enrolled Courses");
        b1.setMaxWidth(Double.MAX_VALUE);
        b1.setOnAction(e -> {
            display.setText("========== YOUR ENROLLED COURSES ==========\n");
            String json = SupabaseService.getStudentEnrolledCoursesFromCloud(email);
            List<String> list = SupabaseService.extractJsonObjects(json);
            if (list.isEmpty()) {
                display.appendText("No registered courses found.\n");
            } else {
                for (String rec : list) {
                    display.appendText("Sem " + SupabaseService.getInt(rec, "semester") + " • [" +
                            SupabaseService.getString(rec, "course_code") + "] " +
                            SupabaseService.getString(rec, "course_name") + "\n");
                }
            }
        });

        Button b2Register = new Button("2. Register Courses (by Sem)");
        b2Register.setMaxWidth(Double.MAX_VALUE);
        b2Register.setOnAction(e -> {
            TextInputDialog dSem = new TextInputDialog("1");
            dSem.setTitle("Register Courses");
            dSem.setHeaderText("Enter Semester Number (1 to 8):");
            dSem.showAndWait().ifPresent(semStr -> {
                try {
                    int sem = Integer.parseInt(semStr.trim());
                    if (sem < 1 || sem > 8) {
                        display.setText("Invalid Semester Number (1 to 8).\n");
                        return;
                    }

                    String json = SupabaseService.getCoursesBySemesterFromCloud(sem);
                    List<String> list = SupabaseService.extractJsonObjects(json);

                    if (list.isEmpty()) {
                        display.setText("No courses available for Semester " + sem + " yet. Ask Admin to add courses.\n");
                        return;
                    }

                    display.setText("=== AVAILABLE COURSES FOR SEMESTER " + sem + " ===\n");
                    for (int i = 0; i < list.size(); i++) {
                        String rec = list.get(i);
                        String cCode = SupabaseService.getString(rec, "course_code");
                        String cName = SupabaseService.getString(rec, "course_name");
                        int count = SupabaseService.getEnrolledStudentCountForCourse(cCode);
                        String status = count >= 50 ? "[FULL - 50/50]" : "(" + count + "/50 Enrolled)";
                        display.appendText((i + 1) + ". [" + cCode + "] " + cName + " " + status + "\n");
                    }

                    TextInputDialog dCourse = new TextInputDialog();
                    dCourse.setTitle("Enroll in Course");
                    dCourse.setHeaderText("Enter Course Code to Register (e.g. " + SupabaseService.getString(list.get(0), "course_code") + "):");
                    dCourse.showAndWait().ifPresent(selectedCode -> {
                        String codeTrim = selectedCode.trim();
                        String matchedName = null;
                        for (String rec : list) {
                            if (SupabaseService.getString(rec, "course_code").equalsIgnoreCase(codeTrim)) {
                                matchedName = SupabaseService.getString(rec, "course_name");
                                break;
                            }
                        }

                        if (matchedName == null) {
                            display.appendText("\nError: Course code '" + codeTrim + "' not found in Semester " + sem + ".\n");
                            return;
                        }

                        int enrolledCount = SupabaseService.getEnrolledStudentCountForCourse(codeTrim);
                        if (enrolledCount >= 50) {
                            display.appendText("\nCourse [" + codeTrim + "] is FULL! (Limit of 50 students reached).\n");
                            return;
                        }

                        boolean ok = SupabaseService.enrollStudentCourseInCloud(studentId, email, sem, codeTrim, matchedName);
                        if (ok) {
                            display.appendText("\n[SUCCESS] Course [" + codeTrim + "] " + matchedName + " successfully registered in Supabase!\n");
                            showAlert(Alert.AlertType.INFORMATION, "Success", "Course registered successfully!");
                        } else {
                            display.appendText("\n[FAILED] Could not register course.\n");
                        }
                    });

                } catch (Exception ex) {
                    display.setText("Invalid semester input.\n");
                }
            });
        });

        Button b3ReRegister = new Button("3. Re-register (Clear & Reset)");
        b3ReRegister.setMaxWidth(Double.MAX_VALUE);
        b3ReRegister.setOnAction(e -> {
            TextInputDialog d = new TextInputDialog("1");
            d.setHeaderText("Enter Semester Number to clear previous selections:");
            d.showAndWait().ifPresent(sStr -> {
                try {
                    int sem = Integer.parseInt(sStr.trim());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Clear all enrolled courses for Semester " + sem + "?", ButtonType.YES, ButtonType.NO);
                    confirm.showAndWait().ifPresent(ans -> {
                        if (ans == ButtonType.YES) {
                            SupabaseService.deleteStudentEnrolledCoursesInCloud(email, sem);
                            display.setText("Previous course selections for Semester " + sem + " cleared. Now you can use option 2 to register again.\n");
                        }
                    });
                } catch (Exception ex) {
                    display.setText("Invalid semester.\n");
                }
            });
        });

        Button b4 = new Button("4. Announcements");
        b4.setMaxWidth(Double.MAX_VALUE);
        b4.setOnAction(e -> {
            display.setText("========== NOTICE BOARD ==========\n");
            String json = SupabaseService.getAllAnnouncementsFromCloud();
            List<String> list = SupabaseService.extractJsonObjects(json);
            for (String rec : list) {
                String role = SupabaseService.getString(rec, "target_role");
                if (role.equalsIgnoreCase("ALL") || role.equalsIgnoreCase("STUDENT")) {
                    display.appendText("[" + SupabaseService.getString(rec, "title") + "]\n" +
                            SupabaseService.getString(rec, "message") + "\n---------------------------------\n");
                }
            }
        });

        menuBox.getChildren().addAll(b1, b2Register, b3ReRegister, b4);
        root.setLeft(menuBox);
        root.setCenter(display);

        stage.setScene(new Scene(root, 800, 500));
    }

    private void showTeacherMenu(String email) {
        BorderPane root = new BorderPane();
        root.setTop(makeTopBar("TEACHER DASHBOARD (" + email + ")"));

        VBox menuBox = new VBox(8);
        menuBox.setPadding(new Insets(15));
        menuBox.setPrefWidth(220);

        TextArea display = new TextArea();
        display.setEditable(false);

        Button b1 = new Button("1. My Assigned Courses");
        b1.setMaxWidth(Double.MAX_VALUE);
        b1.setOnAction(e -> {
            display.setText("========== ASSIGNED COURSES ==========\n");
            String json = SupabaseService.getTeacherAssignedCoursesFromCloud(email);
            List<String> list = SupabaseService.extractJsonObjects(json);
            if (list.isEmpty()) {
                display.appendText("No courses assigned to you by Admin yet.\n");
            } else {
                for (String rec : list) {
                    display.appendText("Sem " + SupabaseService.getInt(rec, "semester") + " • [" +
                            SupabaseService.getString(rec, "course_code") + "] " +
                            SupabaseService.getString(rec, "course_name") + "\n");
                }
            }
        });

        Button b2 = new Button("2. Announcements");
        b2.setMaxWidth(Double.MAX_VALUE);
        b2.setOnAction(e -> {
            display.setText("========== NOTICE BOARD ==========\n");
            String json = SupabaseService.getAllAnnouncementsFromCloud();
            List<String> list = SupabaseService.extractJsonObjects(json);
            for (String rec : list) {
                String role = SupabaseService.getString(rec, "target_role");
                if (role.equalsIgnoreCase("ALL") || role.equalsIgnoreCase("TEACHER")) {
                    display.appendText("[" + SupabaseService.getString(rec, "title") + "]\n" +
                            SupabaseService.getString(rec, "message") + "\n---------------------------------\n");
                }
            }
        });

        menuBox.getChildren().addAll(b1, b2);
        root.setLeft(menuBox);
        root.setCenter(display);

        stage.setScene(new Scene(root, 800, 800));
    }

    private HBox makeTopBar(String title) {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 15, 10, 15));

        Label lbl = new Label(title);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnLogout = new Button("Logout");
        btnLogout.setOnAction(e -> stage.setScene(loginScene));

        bar.getChildren().addAll(lbl, spacer, btnLogout);
        return bar;
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}