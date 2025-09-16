import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

class UserData {
    String username;
    String password;
    String fullName;
    Date lastLogin;
    int quizzesTaken;
    int highScore;

    public UserData(String username, String password, String fullName) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.lastLogin = new Date();
        this.quizzesTaken = 0;
        this.highScore = 0;
    }
}

class Question {
    private final String questionText;
    private final String[] options;
    private final int correctAnswer;

    public Question(String questionText, String[] options, int correctAnswer) {
        this.questionText = questionText;
        this.options = options;
        this.correctAnswer = correctAnswer;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String[] getOptions() {
        return options;
    }

    public boolean checkAnswer(int selectedOption) {
        return selectedOption == correctAnswer;
    }

    @Override
    public String toString() {
        return questionText + ";" + 
               String.join(",", options) + ";" + 
               correctAnswer;
    }

    public static Question fromString(String str) {
        String[] parts = str.split(";");
        String questionText = parts[0];
        String[] options = parts[1].split(",");
        int correctAnswer = Integer.parseInt(parts[2]);
        return new Question(questionText, options, correctAnswer);
    }
}

class QuizData {
    String quizCode;
    String quizName;
    String subject;
    int timer;
    List<Question> questions;
    
    public QuizData(String quizCode, String quizName, String subject, int timer) {
        this.quizCode = quizCode;
        this.quizName = quizName;
        this.subject = subject;
        this.timer = timer;
        this.questions = new ArrayList<>();
    }
    
    @Override
    public String toString() {
        return quizCode + ":" + quizName + ":" + subject + ":" + timer;
    }
    
    public static QuizData fromString(String str) {
        String[] parts = str.split(":");
        if (parts.length < 4) return null;
        return new QuizData(parts[0], parts[1], parts[2], Integer.parseInt(parts[3]));
    }
}

class QuizResult {
    String username;
    String quizCode;
    int score;
    int totalQuestions;
    Date timestamp;

    public QuizResult(String username, String quizCode, int score, int totalQuestions) {
        this.username = username;
        this.quizCode = quizCode;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.timestamp = new Date();
    }
    
    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return username + ";" + quizCode + ";" + score + ";" + totalQuestions + ";" + sdf.format(timestamp);
    }
    
    public static QuizResult fromString(String str) {
        String[] parts = str.split(";");
        if (parts.length < 4) return null;
        
        QuizResult result = new QuizResult(parts[0], parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        if (parts.length > 4) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                result.timestamp = sdf.parse(parts[4]);
            } catch (Exception e) {
                result.timestamp = new Date();
            }
        }
        return result;
    }
}

class QuizSession extends JFrame {
    private List<Question> questions;
    private int currentQuestionIndex = 0;
    private int correctAnswers = 0;
    private String username;
    private String quizCode;
    private final javax.swing.Timer timer;
    private int timeRemaining;
    private JLabel questionLabel;
    private JRadioButton[] options;
    private JLabel timerLabel;
    private JButton nextButton;
    private QuizSystemGUI parent;
    
    public QuizSession(QuizSystemGUI parent, String username, String quizCode, List<Question> questions, int timeInMinutes) {
        this.parent = parent;
        this.username = username;
        this.quizCode = quizCode;
        this.questions = questions;
        this.timeRemaining = timeInMinutes * 60;
        
        setTitle("Quiz Session");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Timer panel
        JPanel timerPanel = new JPanel();
        timerLabel = new JLabel("Time Remaining: " + formatTime(timeRemaining));
        timerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        timerPanel.add(timerLabel);
        
        // Question panel
        JPanel questionPanel = new JPanel(new BorderLayout(10, 10));
        questionLabel = new JLabel("Question");
        questionLabel.setFont(new Font("Arial", Font.BOLD, 16));
        questionPanel.add(questionLabel, BorderLayout.NORTH);
        
        // Options panel
        JPanel optionsPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        ButtonGroup optionGroup = new ButtonGroup();
        options = new JRadioButton[4];
        
        for (int i = 0; i < 4; i++) {
            options[i] = new JRadioButton("Option " + (i + 1));
            optionGroup.add(options[i]);
            optionsPanel.add(options[i]);
        }
        
        questionPanel.add(optionsPanel, BorderLayout.CENTER);
        
        // Navigation panel
        JPanel navPanel = new JPanel();
        nextButton = new JButton("Next Question");
        nextButton.addActionListener(e -> nextQuestion());
        navPanel.add(nextButton);
        
        // Add panels to main panel
        mainPanel.add(timerPanel, BorderLayout.NORTH);
        mainPanel.add(questionPanel, BorderLayout.CENTER);
        mainPanel.add(navPanel, BorderLayout.SOUTH);
        
        
// Setup timer
timer = new javax.swing.Timer(1000, e -> updateTimer());
add(mainPanel);
loadQuestion(0);
timer.start();
    }
    private void loadQuestion(int index) {
        if (index < questions.size()) {
            Question question = questions.get(index);
            questionLabel.setText((index + 1) + ". " + question.getQuestionText());
            
            String[] questionOptions = question.getOptions();
            for (int i = 0; i < options.length; i++) {
                if (i < questionOptions.length) {
                    options[i].setText((i + 1) + ". " + questionOptions[i]);
                    options[i].setVisible(true);
                } else {
                    options[i].setVisible(false);
                }
                options[i].setSelected(false);
            }
            
            if (index == questions.size() - 1) {
                nextButton.setText("Finish Quiz");
            }
        }
    }
    
    private void nextQuestion() {
        // Check if answer is selected
        boolean answered = false;
        int selectedOption = -1;
        
        for (int i = 0; i < options.length; i++) {
            if (options[i].isSelected()) {
                answered = true;
                selectedOption = i + 1;
                break;
            }
        }
        
        if (!answered) {
            JOptionPane.showMessageDialog(this, "Please select an answer", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Check if answer is correct
        Question currentQuestion = questions.get(currentQuestionIndex);
        if (currentQuestion.checkAnswer(selectedOption)) {
            correctAnswers++;
        }
        
        currentQuestionIndex++;
        
        // Check if there are more questions
        if (currentQuestionIndex < questions.size()) {
            loadQuestion(currentQuestionIndex);
        } else {
            finishQuiz();
        }
    }
    
    private void finishQuiz() {
        timer.stop();
        int score = (int) ((double) correctAnswers / questions.size() * 100);
        JOptionPane.showMessageDialog(this,
            "Quiz completed!\nScore: " + score + "%\nCorrect answers: " + correctAnswers + " out of " + questions.size(),
            "Quiz Results",
            JOptionPane.INFORMATION_MESSAGE);
        
        // Save result
        parent.saveQuizResult(username, quizCode, correctAnswers, questions.size());
        
        dispose();
    }
    
    private void updateTimer() {
        timeRemaining--;
        timerLabel.setText("Time Remaining: " + formatTime(timeRemaining));
        
        if (timeRemaining <= 0) {
            timer.stop();
            JOptionPane.showMessageDialog(this, "Time's up! Quiz will now end.", "Time's Up", JOptionPane.WARNING_MESSAGE);
            finishQuiz();
        }
    }
    
    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }
}

public class QuizSystemGUI extends JFrame {
    private static final String USERS_FILE = "quiz_users.txt";
    private static final String QUIZ_DATA_FILE = "quiz_data.txt";
    private static final String QUIZ_QUESTIONS_DIR = "quiz_questions/";
    private static final String RESULTS_FILE = "results.txt";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    private List<UserData> users = new ArrayList<>();
    private List<QuizData> quizzes = new ArrayList<>();
    private List<QuizResult> results = new ArrayList<>();
    private UserData currentUser = null;
    private QuizData currentQuiz = null;

    // UI Components
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private DefaultTableModel userTableModel;
    private DefaultTableModel quizTableModel;
    private DefaultTableModel questionTableModel;

    public QuizSystemGUI() {
        // Setup the main frame
        setTitle("Quiz System");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Initialize card layout
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        add(mainPanel);
        
        // Create directory for quiz questions if it doesn't exist
        new File(QUIZ_QUESTIONS_DIR).mkdirs();
        
        // Create different screens
        createLoginPanel();
        createRegisterPanel();
        createAdminDashboardPanel();
        createUserDashboardPanel();
        createQuizCodePanel();
        
        // Load data
        loadUsers();
        loadQuizData();
        loadResults();
        
        // Show login screen
        cardLayout.show(mainPanel, "Login");
        setVisible(true);
    }

    private void createLoginPanel() {
        JPanel loginPanel = new JPanel();
        loginPanel.setName("Login");
        loginPanel.setLayout(new GridBagLayout());
        loginPanel.setBorder(new EmptyBorder(50, 100, 50, 100));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
    
        JLabel titleLabel = new JLabel("Quiz System Login");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
    
        JLabel usernameLabel = new JLabel("Username:");
        JTextField usernameField = new JTextField(20);
        usernameField.setPreferredSize(new Dimension(250, 35));
    
        JLabel passwordLabel = new JLabel("Password:");
        JPasswordField passwordField = new JPasswordField(20);
        passwordField.setPreferredSize(new Dimension(250, 35));
    
        JButton loginButton = new JButton("Login");
        loginButton.setPreferredSize(new Dimension(120, 40));
    
        JButton registerButton = new JButton("Create New Account");
        registerButton.setPreferredSize(new Dimension(180, 40));
    
        // Layout configuration
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        loginPanel.add(titleLabel, gbc);
    
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        loginPanel.add(usernameLabel, gbc);
    
        gbc.gridx = 1;
        loginPanel.add(usernameField, gbc);
    
        gbc.gridx = 0;
        gbc.gridy = 2;
        loginPanel.add(passwordLabel, gbc);
    
        gbc.gridx = 1;
        loginPanel.add(passwordField, gbc);
    
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        loginPanel.add(loginButton, gbc);
    
        gbc.gridx = 1;
        loginPanel.add(registerButton, gbc);
        
        // Login action
        loginButton.addActionListener(e -> performLogin(usernameField, passwordField));
    
        // Add enter key listener for login
        usernameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performLogin(usernameField, passwordField);
                }
            }
        });
    
        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performLogin(usernameField, passwordField);
                }
            }
        });
    
        registerButton.addActionListener(e -> {
            cardLayout.show(mainPanel, "Register");
        });
    
        mainPanel.add(loginPanel, "Login");
    }
    
    private void performLogin(JTextField usernameField, JPasswordField passwordField) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        // Admin login
        if (username.equals(ADMIN_USERNAME) && password.equals(ADMIN_PASSWORD)) {
            updateAdminDashboard();
            cardLayout.show(mainPanel, "AdminDashboard");
            return;
        }
        
        // User login
        currentUser = login(username, password);
        if (currentUser != null) {
            cardLayout.show(mainPanel, "QuizCode");
        } else {
            JOptionPane.showMessageDialog(this,
                "Invalid username or password",
                "Login Failed",
                JOptionPane.ERROR_MESSAGE);
        }
        usernameField.setText("");
        passwordField.setText("");
    }
    
    private void createRegisterPanel() {
        JPanel registerPanel = new JPanel();
        registerPanel.setName("Register");
        registerPanel.setLayout(new GridBagLayout());
        registerPanel.setBorder(new EmptyBorder(50, 100, 50, 100));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JLabel titleLabel = new JLabel("Create New Account");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        
        JLabel fullNameLabel = new JLabel("Full Name:");
        JTextField fullNameField = new JTextField(20);
        fullNameField.setPreferredSize(new Dimension(250, 35));
        
        JLabel usernameLabel = new JLabel("Username:");
        JTextField usernameField = new JTextField(20);
        usernameField.setPreferredSize(new Dimension(250, 35));
        
        JLabel passwordLabel = new JLabel("Password:");
        JPasswordField passwordField = new JPasswordField(20);
        passwordField.setPreferredSize(new Dimension(250, 35));
        
        JButton createButton = new JButton("Create Account");
        createButton.setPreferredSize(new Dimension(150, 40));
        
        JButton backButton = new JButton("Back to Login");
        backButton.setPreferredSize(new Dimension(150, 40));
        
        // Layout configuration
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        registerPanel.add(titleLabel, gbc);
        
        gbc.gridwidth = 1;
        
        gbc.gridy = 1;
        gbc.gridx = 0;
        registerPanel.add(fullNameLabel, gbc);
        
        gbc.gridx = 1;
        registerPanel.add(fullNameField, gbc);
        
        gbc.gridy = 2;
        gbc.gridx = 0;
        registerPanel.add(usernameLabel, gbc);
        
        gbc.gridx = 1;
        registerPanel.add(usernameField, gbc);
        
        gbc.gridy = 3;
        gbc.gridx = 0;
        registerPanel.add(passwordLabel, gbc);
        
        gbc.gridx = 1;
        registerPanel.add(passwordField, gbc);
        
        gbc.gridy = 4;
        gbc.gridx = 0;
        registerPanel.add(backButton, gbc);
        
        gbc.gridx = 1;
        registerPanel.add(createButton, gbc);
        
        createButton.addActionListener(e -> {
            String fullName = fullNameField.getText().trim();
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            
            if (fullName.isEmpty() || username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "All fields are required", 
                    "Registration Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            boolean success = createAccount(fullName, username, password);
            if (success) {
                JOptionPane.showMessageDialog(this, 
                    "Account created successfully", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
                clearFields(fullNameField, usernameField, passwordField);
                cardLayout.show(mainPanel, "Login");
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Username already exists", 
                    "Registration Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        backButton.addActionListener(e -> {
            clearFields(fullNameField, usernameField, passwordField);
            cardLayout.show(mainPanel, "Login");
        });
        
        mainPanel.add(registerPanel, "Register");
    }
    
    private void createQuizCodePanel() {
        JPanel quizCodePanel = new JPanel();
        quizCodePanel.setName("QuizCode");
        quizCodePanel.setLayout(new GridBagLayout());
        quizCodePanel.setBorder(new EmptyBorder(50, 100, 50, 100));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JLabel titleLabel = new JLabel("Enter Quiz Code");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        
        JLabel codeLabel = new JLabel("Quiz Code:");
        JTextField codeField = new JTextField(20);
        codeField.setPreferredSize(new Dimension(250, 35));
        
        JButton enterButton = new JButton("Enter Quiz");
        enterButton.setPreferredSize(new Dimension(150, 40));
        
        JButton viewDashboardButton = new JButton("View Dashboard");
        viewDashboardButton.setPreferredSize(new Dimension(150, 40));
        
        JButton logoutButton = new JButton("Logout");
        logoutButton.setPreferredSize(new Dimension(150, 40));
        
        // Layout configuration
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        quizCodePanel.add(titleLabel, gbc);
        
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        JLabel welcomeLabel = new JLabel("Welcome! Enter a quiz code to begin.");
        welcomeLabel.setHorizontalAlignment(JLabel.CENTER);
        quizCodePanel.add(welcomeLabel, gbc);
        
        gbc.gridwidth = 1;
        gbc.gridy = 2;
        gbc.gridx = 0;
        quizCodePanel.add(codeLabel, gbc);
        
        gbc.gridx = 1;
        quizCodePanel.add(codeField, gbc);
        
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(enterButton);
        buttonPanel.add(viewDashboardButton);
        quizCodePanel.add(buttonPanel, gbc);
        
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JPanel logoutPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        logoutPanel.add(logoutButton);
        quizCodePanel.add(logoutPanel, gbc);
        
        // Add action listeners
        enterButton.addActionListener(e -> {
            String code = codeField.getText().trim();
            if (code.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a quiz code", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Find quiz with the given code
            currentQuiz = findQuizByCode(code);
            if (currentQuiz != null) {
                List<Question> quizQuestions = loadQuizQuestions(currentQuiz.quizCode);
                if (quizQuestions.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "This quiz has no questions", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    // Create and show quiz session
                    QuizSession session = new QuizSession(this, currentUser.username, currentQuiz.quizCode, quizQuestions, currentQuiz.timer);
                    session.setVisible(true);
                    codeField.setText("");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Invalid quiz code", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        viewDashboardButton.addActionListener(e -> {
            updateUserDashboard();
            cardLayout.show(mainPanel, "UserDashboard");
        });
        
        logoutButton.addActionListener(e -> {
            currentUser = null;
            currentQuiz = null;
            codeField.setText("");
            cardLayout.show(mainPanel, "Login");
        });
        
        mainPanel.add(quizCodePanel, "QuizCode");
    }
    
    private void clearFields(JTextField... fields) {
        for (JTextField field : fields) {
            field.setText("");
        }
    }
    
    private void createAdminDashboardPanel() {
        JPanel dashboardPanel = new JPanel(new BorderLayout(10, 10));
        dashboardPanel.setName("AdminDashboard");
        dashboardPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Admin Dashboard");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        dashboardPanel.add(titleLabel, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setPreferredSize(new Dimension(700, 400));

        // Quiz Management Tab
        JPanel quizPanel = new JPanel(new BorderLayout());
        quizTableModel = new DefaultTableModel(
            new Object[] {"Quiz Code", "Quiz Name", "Subject", "Timer (min)"}, 0);
        JTable quizTable = new JTable(quizTableModel);
        quizTable.setPreferredScrollableViewportSize(new Dimension(680, 300));
        JScrollPane quizScrollPane = new JScrollPane(quizTable);
        
        JPanel quizButtonPanel = new JPanel();
        JButton addQuizButton = new JButton("Add New Quiz");
        JButton editQuizButton = new JButton("Edit Selected Quiz");
        JButton removeQuizButton = new JButton("Remove Selected Quiz");
        
        quizButtonPanel.add(addQuizButton);
        quizButtonPanel.add(editQuizButton);
        quizButtonPanel.add(removeQuizButton);
        
        quizPanel.add(quizScrollPane, BorderLayout.CENTER);
        quizPanel.add(quizButtonPanel, BorderLayout.SOUTH);
        
        // Questions Management Tab
        JPanel questionsPanel = new JPanel(new BorderLayout());
        questionTableModel = new DefaultTableModel(
            new Object[] {"Quiz Code", "Question", "Options", "Correct Answer"}, 0);
        JTable questionTable = new JTable(questionTableModel);
        questionTable.setPreferredScrollableViewportSize(new Dimension(680, 300));
        JScrollPane questionScrollPane = new JScrollPane(questionTable);
        
        JPanel questionButtonPanel = new JPanel();
        JButton selectQuizButton = new JButton("Select Quiz");
        JButton addQuestionButton = new JButton("Add Question");
        JButton removeQuestionButton = new JButton("Remove Selected Question");
        
        questionButtonPanel.add(selectQuizButton);
        questionButtonPanel.add(addQuestionButton);
        questionButtonPanel.add(removeQuestionButton);
        
        questionsPanel.add(questionScrollPane, BorderLayout.CENTER);
        questionsPanel.add(questionButtonPanel, BorderLayout.SOUTH);
        
        // User Information Tab
        JPanel userInfoPanel = new JPanel(new BorderLayout());
        userTableModel = new DefaultTableModel(
            new Object[] {"Username", "Full Name", "Last Login", "Quizzes Taken", "High Score"}, 0);
        JTable userTable = new JTable(userTableModel);
        userTable.setPreferredScrollableViewportSize(new Dimension(680, 350));
        JScrollPane userScrollPane = new JScrollPane(userTable);
        userInfoPanel.add(userScrollPane, BorderLayout.CENTER);
        
        tabbedPane.addTab("Quiz Management", quizPanel);
        tabbedPane.addTab("Questions Management", questionsPanel);
        tabbedPane.addTab("User Information", userInfoPanel);
        
        dashboardPanel.add(tabbedPane, BorderLayout.CENTER);
        
        JButton logoutButton = new JButton("Logout");
        logoutButton.setPreferredSize(new Dimension(200, 40));
        dashboardPanel.add(logoutButton, BorderLayout.SOUTH);
        
        // Add action listeners
        addQuizButton.addActionListener(e -> addQuiz());
        editQuizButton.addActionListener(e -> editQuiz(quizTable.getSelectedRow()));
        removeQuizButton.addActionListener(e -> removeQuiz(quizTable.getSelectedRow()));
        
        selectQuizButton.addActionListener(e -> selectQuizForQuestions());
        addQuestionButton.addActionListener(e -> addQuestion());
        removeQuestionButton.addActionListener(e -> removeQuestion(questionTable.getSelectedRow()));
        
        logoutButton.addActionListener(e -> {
            currentQuiz = null;
            cardLayout.show(mainPanel, "Login");
        });
        
        mainPanel.add(dashboardPanel, "AdminDashboard");
    }
    
    private void createUserDashboardPanel() {
        JPanel dashboardPanel = new JPanel(new BorderLayout(10, 10));
        dashboardPanel.setName("UserDashboard");
        dashboardPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel userInfoPanel = new JPanel(new GridLayout(0, 1));
        Font accountDetailsFont = new Font("Arial", Font.BOLD, 20);

        JLabel welcomeLabel = new JLabel("Welcome, User");
        welcomeLabel.setFont(accountDetailsFont);
        
        JLabel statisticsLabel = new JLabel("Quizzes Taken: 0");
        statisticsLabel.setFont(accountDetailsFont);
        
        JLabel highScoreLabel = new JLabel("High Score: 0%");
        highScoreLabel.setFont(accountDetailsFont);

        userInfoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("User Information"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        userInfoPanel.add(welcomeLabel);
        userInfoPanel.add(statisticsLabel);
        userInfoPanel.add(highScoreLabel);
        
        // Quiz control panel
        JPanel controlPanel = new JPanel();
        JButton enterQuizCodeButton = new JButton("Enter Quiz Code");
        enterQuizCodeButton.setPreferredSize(new Dimension(200, 50));
        JButton viewResultsButton = new JButton("View My Results");
        viewResultsButton.setPreferredSize(new Dimension(200, 50));
        
        controlPanel.add(enterQuizCodeButton);
        controlPanel.add(viewResultsButton);
        
        // Logout button
        JButton logoutButton = new JButton("Logout");
        logoutButton.setPreferredSize(new Dimension(200, 40));
        
        // Add everything to dashboard
        // Add everything to dashboard
        dashboardPanel.add(userInfoPanel, BorderLayout.NORTH);
        
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(BorderFactory.createTitledBorder("My Quiz Results"));
        
        DefaultTableModel resultsTableModel = new DefaultTableModel(
            new Object[] {"Quiz Name", "Score", "Date"}, 0);
        JTable resultsTable = new JTable(resultsTableModel);
        JScrollPane resultsScrollPane = new JScrollPane(resultsTable);
        resultsScrollPane.setPreferredSize(new Dimension(700, 300));
        
        resultsPanel.add(resultsScrollPane, BorderLayout.CENTER);
        dashboardPanel.add(resultsPanel, BorderLayout.CENTER);
        dashboardPanel.add(controlPanel, BorderLayout.SOUTH);
        
        // Add action listeners
        enterQuizCodeButton.addActionListener(e -> {
            cardLayout.show(mainPanel, "QuizCode");
        });
        
        viewResultsButton.addActionListener(e -> {
            updateUserResults(resultsTableModel);
        });
        
        logoutButton.addActionListener(e -> {
            currentUser = null;
            cardLayout.show(mainPanel, "Login");
        });
        
        mainPanel.add(dashboardPanel, "UserDashboard");
    }
    
    private void updateUserResults(DefaultTableModel model) {
        // Clear table
        model.setRowCount(0);
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (QuizResult result : results) {
            if (result.username.equals(currentUser.username)) {
                // Get quiz name
                String quizName = "Unknown";
                for (QuizData quiz : quizzes) {
                    if (quiz.quizCode.equals(result.quizCode)) {
                        quizName = quiz.quizName;
                        break;
                    }
                }
                
                model.addRow(new Object[] {
                    quizName,
                    result.score + " / " + result.totalQuestions + " (" + (int)((double)result.score / result.totalQuestions * 100) + "%)",
                    sdf.format(result.timestamp)
                });
            }
        }
    }
    
    private void updateUserDashboard() {
        if (currentUser == null) return;
        
        for (Component component : mainPanel.getComponents()) {
            if (component.getName() != null && component.getName().equals("UserDashboard")) {
                JPanel dashboardPanel = (JPanel) component;
                
                // Update welcome label
                JPanel userInfoPanel = (JPanel) dashboardPanel.getComponent(0);
                JLabel welcomeLabel = (JLabel) userInfoPanel.getComponent(0);
                welcomeLabel.setText("Welcome, " + currentUser.fullName);
                
                JLabel statisticsLabel = (JLabel) userInfoPanel.getComponent(1);
                statisticsLabel.setText("Quizzes Taken: " + currentUser.quizzesTaken);
                
                JLabel highScoreLabel = (JLabel) userInfoPanel.getComponent(2);
                highScoreLabel.setText("High Score: " + currentUser.highScore + "%");
                
                // Get results table model
                JPanel resultsPanel = (JPanel) dashboardPanel.getComponent(1);
                JScrollPane scrollPane = (JScrollPane) resultsPanel.getComponent(0);
                JTable table = (JTable) scrollPane.getViewport().getView();
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                
                updateUserResults(model);
                break;
            }
        }
    }
    
    private void updateAdminDashboard() {
        // Update quiz table
        quizTableModel.setRowCount(0);
        for (QuizData quiz : quizzes) {
            quizTableModel.addRow(new Object[] {
                quiz.quizCode,
                quiz.quizName,
                quiz.subject,
                quiz.timer
            });
        }
        
        // Update user table
        userTableModel.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (UserData user : users) {
            userTableModel.addRow(new Object[] {
                user.username,
                user.fullName,
                sdf.format(user.lastLogin),
                user.quizzesTaken,
                user.highScore + "%"
            });
        }
    }
    
    private void addQuiz() {
        JTextField codeField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField subjectField = new JTextField();
        JTextField timerField = new JTextField("5");
        
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Quiz Code:"));
        panel.add(codeField);
        panel.add(new JLabel("Quiz Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Subject:"));
        panel.add(subjectField);
        panel.add(new JLabel("Timer (minutes):"));
        panel.add(timerField);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "Add New Quiz", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String code = codeField.getText().trim();
            String name = nameField.getText().trim();
            String subject = subjectField.getText().trim();
            int timer;
            
            try {
                timer = Integer.parseInt(timerField.getText().trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Timer must be a number", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (code.isEmpty() || name.isEmpty() || subject.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Check if quiz code already exists
            for (QuizData quiz : quizzes) {
                if (quiz.quizCode.equals(code)) {
                    JOptionPane.showMessageDialog(this, "Quiz code already exists", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            
            QuizData newQuiz = new QuizData(code, name, subject, timer);
            quizzes.add(newQuiz);
            saveQuizData();
            updateAdminDashboard();
            
            JOptionPane.showMessageDialog(this, "Quiz added successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void editQuiz(int row) {
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a quiz to edit", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String code = (String) quizTableModel.getValueAt(row, 0);
        QuizData quiz = null;
        
        for (QuizData q : quizzes) {
            if (q.quizCode.equals(code)) {
                quiz = q;
                break;
            }
        }
        
        if (quiz == null) return;
        
        JTextField nameField = new JTextField(quiz.quizName);
        JTextField subjectField = new JTextField(quiz.subject);
        JTextField timerField = new JTextField(String.valueOf(quiz.timer));
        
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Quiz Code: " + quiz.quizCode + " (cannot be changed)"));
        panel.add(new JLabel("Quiz Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Subject:"));
        panel.add(subjectField);
        panel.add(new JLabel("Timer (minutes):"));
        panel.add(timerField);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Quiz", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String subject = subjectField.getText().trim();
            int timer;
            
            try {
                timer = Integer.parseInt(timerField.getText().trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Timer must be a number", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (name.isEmpty() || subject.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            quiz.quizName = name;
            quiz.subject = subject;
            quiz.timer = timer;
            
            saveQuizData();
            updateAdminDashboard();
            
            JOptionPane.showMessageDialog(this, "Quiz updated successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void removeQuiz(int row) {
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a quiz to remove", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String code = (String) quizTableModel.getValueAt(row, 0);
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to remove quiz " + code + "?\nThis will also remove all questions.",
            "Confirm Removal",
            JOptionPane.YES_NO_OPTION);
            
        if (confirm == JOptionPane.YES_OPTION) {
            // Remove quiz from list
            for (int i = 0; i < quizzes.size(); i++) {
                if (quizzes.get(i).quizCode.equals(code)) {
                    quizzes.remove(i);
                    break;
                }
            }
            
            // Remove quiz questions file
            File questionsFile = new File(QUIZ_QUESTIONS_DIR + code + ".txt");
            if (questionsFile.exists()) {
                questionsFile.delete();
            }
            
            saveQuizData();
            updateAdminDashboard();
            
            JOptionPane.showMessageDialog(this, "Quiz removed successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void selectQuizForQuestions() {
        if (quizzes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No quizzes available", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String[] quizCodes = new String[quizzes.size()];
        String[] quizNames = new String[quizzes.size()];
        
        for (int i = 0; i < quizzes.size(); i++) {
            quizCodes[i] = quizzes.get(i).quizCode;
            quizNames[i] = quizzes.get(i).quizName;
        }
        
        JComboBox<String> comboBox = new JComboBox<>(quizNames);
        
        int result = JOptionPane.showConfirmDialog(this,
            comboBox,
            "Select Quiz for Questions",
            JOptionPane.OK_CANCEL_OPTION);
            
        if (result == JOptionPane.OK_OPTION) {
            int selectedIndex = comboBox.getSelectedIndex();
            if (selectedIndex >= 0) {
                String selectedCode = quizCodes[selectedIndex];
                for (QuizData quiz : quizzes) {
                    if (quiz.quizCode.equals(selectedCode)) {
                        currentQuiz = quiz;
                        break;
                    }
                }
                
                // Update questions table
                updateQuestionsTable();
            }
        }
    }
    
    private void updateQuestionsTable() {
        if (currentQuiz == null) return;
        
        questionTableModel.setRowCount(0);
        List<Question> questions = loadQuizQuestions(currentQuiz.quizCode);
        
        for (Question question : questions) {
            String options = String.join(", ", question.getOptions());
            questionTableModel.addRow(new Object[] {
                currentQuiz.quizCode,
                question.getQuestionText(),
                options,
                question.getOptions()[0] // This is a placeholder; we don't show the correct answer in the table
            });
        }
    }
    
    private void addQuestion() {
        if (currentQuiz == null) {
            JOptionPane.showMessageDialog(this, "Please select a quiz first", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        JTextField questionField = new JTextField();
        JTextField option1Field = new JTextField();
        JTextField option2Field = new JTextField();
        JTextField option3Field = new JTextField();
        JTextField option4Field = new JTextField();
        
        String[] options = {"Option 1", "Option 2", "Option 3", "Option 4"};
        JComboBox<String> correctAnswerBox = new JComboBox<>(options);
        
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Question:"));
        panel.add(questionField);
        panel.add(new JLabel("Option 1:"));
        panel.add(option1Field);
        panel.add(new JLabel("Option 2:"));
        panel.add(option2Field);
        panel.add(new JLabel("Option 3:"));
        panel.add(option3Field);
        panel.add(new JLabel("Option 4:"));
        panel.add(option4Field);
        panel.add(new JLabel("Correct Answer:"));
        panel.add(correctAnswerBox);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "Add Question", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String questionText = questionField.getText().trim();
            String option1 = option1Field.getText().trim();
            String option2 = option2Field.getText().trim();
            String option3 = option3Field.getText().trim();
            String option4 = option4Field.getText().trim();
            
            if (questionText.isEmpty() || option1.isEmpty() || option2.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Question, Option 1, and Option 2 are required", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Create options array, filtering out empty options
            ArrayList<String> optionsList = new ArrayList<>();
            if (!option1.isEmpty()) optionsList.add(option1);
            if (!option2.isEmpty()) optionsList.add(option2);
            if (!option3.isEmpty()) optionsList.add(option3);
            if (!option4.isEmpty()) optionsList.add(option4);
            
            String[] optionsArray = optionsList.toArray(new String[0]);
            
            // Get correct answer index (1-based in the UI, so convert to 0-based for internal use)
            int correctAnswer = correctAnswerBox.getSelectedIndex() + 1;
            
            // Create and save question
            Question question = new Question(questionText, optionsArray, correctAnswer);
            saveQuizQuestion(currentQuiz.quizCode, question);
            
            updateQuestionsTable();
            JOptionPane.showMessageDialog(this, "Question added successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void removeQuestion(int row) {
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a question to remove", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (currentQuiz == null) {
            JOptionPane.showMessageDialog(this, "No quiz selected", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String questionText = (String) questionTableModel.getValueAt(row, 1);
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to remove this question?",
            "Confirm Removal",
            JOptionPane.YES_NO_OPTION);
            
        if (confirm == JOptionPane.YES_OPTION) {
            // Load all questions
            List<Question> questions = loadQuizQuestions(currentQuiz.quizCode);
            // Remove the selected question
            for (int i = 0; i < questions.size(); i++) {
                if (questions.get(i).getQuestionText().equals(questionText)) {
                    questions.remove(i);
                    break;
                }
            }
            
            // Save questions back to file
            saveQuizQuestions(currentQuiz.quizCode, questions);
            
            updateQuestionsTable();
            JOptionPane.showMessageDialog(this, "Question removed successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void saveQuizQuestion(String quizCode, Question question) {
        List<Question> questions = loadQuizQuestions(quizCode);
        questions.add(question);
        saveQuizQuestions(quizCode, questions);
    }
    
    private void saveQuizQuestions(String quizCode, List<Question> questions) {
        try {
            File file = new File(QUIZ_QUESTIONS_DIR + quizCode + ".txt");
            PrintWriter writer = new PrintWriter(new FileWriter(file));
            
            for (Question question : questions) {
                writer.println(question.toString());
            }
            
            writer.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving questions: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private List<Question> loadQuizQuestions(String quizCode) {
        List<Question> questions = new ArrayList<>();
        
        try {
            File file = new File(QUIZ_QUESTIONS_DIR + quizCode + ".txt");
            if (!file.exists()) return questions;
            
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.isEmpty()) {
                    Question question = Question.fromString(line);
                    if (question != null) {
                        questions.add(question);
                    }
                }
            }
            scanner.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading questions: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        
        return questions;
    }
    
    private UserData login(String username, String password) {
        for (UserData user : users) {
            if (user.username.equals(username) && user.password.equals(password)) {
                user.lastLogin = new Date();
                saveUsers();
                return user;
            }
        }
        return null;
    }
    
    private boolean createAccount(String fullName, String username, String password) {
        // Check if username already exists
        for (UserData user : users) {
            if (user.username.equals(username)) {
                return false;
            }
        }
        
        UserData newUser = new UserData(username, password, fullName);
        users.add(newUser);
        saveUsers();
        return true;
    }
    
    private QuizData findQuizByCode(String code) {
        for (QuizData quiz : quizzes) {
            if (quiz.quizCode.equals(code)) {
                return quiz;
            }
        }
        return null;
    }
    
    public void saveQuizResult(String username, String quizCode, int score, int totalQuestions) {
        // Create and save result
        QuizResult result = new QuizResult(username, quizCode, score, totalQuestions);
        results.add(result);
        saveResults();
        
        // Update user stats
        for (UserData user : users) {
            if (user.username.equals(username)) {
                user.quizzesTaken++;
                int percent = (int) ((double) score / totalQuestions * 100);
                if (percent > user.highScore) {
                    user.highScore = percent;
                }
                saveUsers();
                break;
            }
        }
    }
    
    private void loadUsers() {
        users.clear();
        try {
            File file = new File(USERS_FILE);
            if (!file.exists()) return;
            
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.isEmpty()) {
                    String[] parts = line.split(";");
                    if (parts.length >= 6) {
                        try {
                            UserData user = new UserData(parts[0], parts[1], parts[2]);
                            
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            user.lastLogin = sdf.parse(parts[3]);
                            
                            user.quizzesTaken = Integer.parseInt(parts[4]);
                            user.highScore = Integer.parseInt(parts[5]);
                            
                            users.add(user);
                        } catch (Exception e) {
                            // Skip invalid user data
                        }
                    }
                }
            }
            scanner.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading users: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void saveUsers() {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(USERS_FILE));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            for (UserData user : users) {
                writer.println(user.username + ";" + 
                               user.password + ";" + 
                               user.fullName + ";" + 
                               sdf.format(user.lastLogin) + ";" + 
                               user.quizzesTaken + ";" + 
                               user.highScore);
            }
            
            writer.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving users: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadQuizData() {
        quizzes.clear();
        try {
            File file = new File(QUIZ_DATA_FILE);
            if (!file.exists()) return;
            
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.isEmpty()) {
                    QuizData quiz = QuizData.fromString(line);
                    if (quiz != null) {
                        quizzes.add(quiz);
                    }
                }
            }
            scanner.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading quiz data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void saveQuizData() {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(QUIZ_DATA_FILE));
            
            for (QuizData quiz : quizzes) {
                writer.println(quiz.toString());
            }
            
            writer.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving quiz data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadResults() {
        results.clear();
        try {
            File file = new File(RESULTS_FILE);
            if (!file.exists()) return;
            
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.isEmpty()) {
                    QuizResult result = QuizResult.fromString(line);
                    if (result != null) {
                        results.add(result);
                    }
                }
            }
            scanner.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading results: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void saveResults() {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(RESULTS_FILE));
            
            for (QuizResult result : results) {
                writer.println(result.toString());
            }
            
            writer.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving results: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            new QuizSystemGUI();
        });
    }
}