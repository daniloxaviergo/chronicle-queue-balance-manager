import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BalanceService {
  private final BalanceManager balanceManager;
  private final ExecutorService threadPool;

  public BalanceService(BalanceManager balanceManager) {
    this.balanceManager = balanceManager;
    this.threadPool = Executors.newFixedThreadPool(1000);
  }

  public void start(int port) throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println("Balance Service running on port " + port);
      while (true) {
        // try (Socket clientSocket = serverSocket.accept()) {
        //     handleClient(clientSocket);
        // }

        // Accept new client connections
        Socket clientSocket = serverSocket.accept();
        // Submit each connection to the thread pool for processing
        threadPool.submit(() -> {
          try {
            handleClient(clientSocket);
          } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
          }
        });
      }
    } finally {
      // Shutdown the thread pool when the server stops
      threadPool.shutdown();
    }
  }

  private void handleClient(Socket clientSocket) throws IOException {
    try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
         PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

      String inputLine;
      while ((inputLine = in.readLine()) != null) {
        // Split command into parts using regex, and trim the results
        String[] parts = inputLine.split("\\(|,|\\)");
        String command = parts[0].trim();

        // Handling the 'plus_funds' and 'sub_funds' commands
        if ("plus_funds".equals(command) || "sub_funds".equals(command)) {
          if (parts.length < 3) {
            out.println("Invalid command format. Expected format: " + command + "(account_id, amount)");
            continue;
          }

          int accountId = Integer.parseInt(parts[1].trim());
          long amount = Long.parseLong(parts[2].trim());

          if ("plus_funds".equals(command)) {
            balanceManager.plusFunds(accountId, amount);
            out.println("Added " + amount + " to account " + accountId);
          } else {
            balanceManager.subFunds(accountId, amount);
            out.println("Subtracted " + amount + " from account " + accountId);
          }
        } 
        // Handling the 'get_balance' command
        else if ("get_balance".equals(command)) {
          if (parts.length < 2) {
            out.println("Invalid command format. Expected format: get_balance(account_id)");
            continue;
          }

          int accountId = Integer.parseInt(parts[1].trim());
          long balance = balanceManager.getBalance(accountId);
          out.println("Balance for account " + accountId + ": " + balance);
        }
        // Handling the 'replay_transactions' command
        else if ("replay_transactions".equals(inputLine.trim())) {
          balanceManager.replayTransactions();
          out.println("Replaying transactions...");
        }
        // Handling unknown commands
        else {
          out.println("Unknown command: " + command);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(e);
    } finally {
      clientSocket.close();
    }
  }
}
