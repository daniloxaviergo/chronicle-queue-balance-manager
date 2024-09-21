import java.io.*;
import java.net.*;
import java.util.Date;

public class TCPServer {
  public static void main(String[] args) {
    BalanceManager balanceManager = new BalanceManager("balance-queue");

    BalanceService balanceService = new BalanceService(balanceManager);
    try {
      balanceService.start(12345);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
