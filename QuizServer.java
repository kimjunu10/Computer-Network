import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class QuizServer {
    private static List<Question> questions = new ArrayList<>();
    private static final int MAX_HINTS = 5; // Maximum number of hints allowed

    public static void main(String[] args) throws IOException {
        loadQuestions(); // Load the questions into the server
        ServerSocket serverSocket = new ServerSocket(1234); // Create a server socket on port 1234
        System.out.println("Quiz Server is running...");

        ExecutorService pool = Executors.newFixedThreadPool(20); // Thread pool to handle multiple clients

        while (true) {
            Socket socket = serverSocket.accept(); // Wait for a client to connect
            pool.execute(new ClientHandler(socket)); // Handle each client in a separaate thread
        }
    }

    private static void loadQuestions() {
        // Add questions with their respective answers, hints, and scores
        questions.add(new Question("1. What is the square root of 36?", "6", "Think of a number multiplied by itself.", 5));
        questions.add(new Question("2. What is the official language of India?", "Hindi", "It's one of the most spoken languages in India.", 10));
        questions.add(new Question("3. What is the smallest prime number?", "2", "It's an even number.", 5));
        questions.add(new Question("4. What is the name of the longest river in South America?", "Amazon", "It shares its name with a major online retailer.", 5));
        questions.add(new Question("5. What is the capital of Canada?", "Ottawa", "Not Toronto or Vancouver.", 15));
        questions.add(new Question("6. What element has the chemical symbol H?", "Hydrogen", "It's the lightest element.", 20));
        questions.add(new Question("7. What company did Steve Jobs found?", "Apple", "Think of a popular tech company with a fruit name.", 5));
        questions.add(new Question("8. Which gas makes up the largest proportion of Earth’s atmosphere?", "Nitrogen", "It's not oxygen.", 20));
        questions.add(new Question("9. Which language has the largest number of speakers in the world?", "Chinese", "It has the most speakers due to population.", 10));
        questions.add(new Question("10. What is the largest continent in the world?", "Asia", "It's where China and India are located.", 5));
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private int remainingHints = MAX_HINTS; // Number of hints remaining for the client

        public ClientHandler(Socket socket) {
            this.socket = socket; // Store the client's socket
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

                // Send a welcome message to the client
                out.write("TYPE:WELCOME\n");
                out.write("Welcome to the JUN's Quiz! You have " + questions.size() + " questions and " + remainingHints + " hints available. Good luck to you.!!\n\n");
                out.flush();

                int score = 0; // Initialize the client's score
                for (Question question : questions) {
                    // Send the question and its score to the client
                    out.write("TYPE:QUESTION\n");
                    out.write(question.getQuestionText() + "\n");
                    out.write("(Score: " + question.getScore() + ")\n");
                    out.flush();

                    String answer = in.readLine(); // Read the client's answer
                    if (answer != null && answer.equalsIgnoreCase("hint")) {
                        // Provide a hint if requested and hints are available
                        if (remainingHints > 0) {
                            remainingHints--;
                            out.write("TYPE:HINT\n");
                            out.write("HINT: " + question.getHint() + " (Hints remaining: " + remainingHints + ")\n");
                            out.flush();
                            answer = in.readLine(); // Read the client's next answer
                        } else {
                            // Notify the client if no hints remain
                            out.write("TYPE:HINT\n");
                            out.write("No hints remaining.\n");
                            out.flush();
                            answer = in.readLine();
                        }
                    }

                    if (answer != null && answer.equalsIgnoreCase(question.getAnswer())) {
                        // Correct answer
                        out.write("TYPE:FEEDBACK\n");
                        out.write("Correct!\n");
                        out.flush();
                        score += question.getScore(); // Add the question's score to the client's total
                    } else {
                        // Incorrect answer
                        out.write("TYPE:FEEDBACK\n");
                        out.write("Incorrect! The correct answer was: " + question.getAnswer() + "\n");
                        out.flush();
                    }
                }

                // Send the client's final score
                out.write("TYPE:SCORE\n");
                out.write("Thank you for your hard work. The quiz is over! Your final score is: " + score + "/100\n");
                out.flush();

            } catch (IOException e) {
                System.out.println("Error handling client: " + e.getMessage()); // Log any errors
            } finally {
                try {
                    socket.close(); // Close the client's socket
                } catch (IOException e) {
                    System.out.println("Error closing socket: " + e.getMessage()); // Log socket closing errors
                }
            }
        }
    }

    private static class Question {
        private String questionText; // The text of the question
        private String answer;       // The correct answer
        private String hint;         // The hint for the question
        private int score;           // The score for the question

        public Question(String questionText, String answer, String hint, int score) {
            this.questionText = questionText;
            this.answer = answer;
            this.hint = hint;
            this.score = score;
        }

        public String getQuestionText() {
            return questionText;
        }

        public String getAnswer() {
            return answer;
        }

        public String getHint() {
            return hint;
        }

        public int getScore() {
            return score;
        }
    }
}
