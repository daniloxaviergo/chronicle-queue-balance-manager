import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class BalanceManager {
  private final Map<Integer, Long> balances = new HashMap<>();
  private final Map<Integer, Lock> locks = new ConcurrentHashMap<>();
  private final ChronicleQueue queue;

  public BalanceManager(String queuePath) {
    this.queue = ChronicleQueue.single(queuePath);
  }

  public void plusFunds(int accountId, long amount) {
    Lock lock = getLockForAccount(accountId);
    lock.lock();
    try {
      balances.merge(accountId, amount, Long::sum);
      logTransaction("plus_funds", accountId, amount);
    } finally {
      lock.unlock();
    }
  }

  public void subFunds(int accountId, long amount) {
    Lock lock = getLockForAccount(accountId);
    lock.lock();
    try {
      balances.merge(accountId, -amount, Long::sum);
      logTransaction("sub_funds", accountId, amount);
    } finally {
      lock.unlock();
    }
  }

  public long getBalance(int accountId) {
    return balances.getOrDefault(accountId, 0L);
  }

  public void replayTransactions() {
    ExcerptTailer tailer = queue.createTailer();
    String transaction;
    while ((transaction = tailer.readText()) != null) {
      System.out.println("Replaying: " + transaction);
    }
  }

  private void logTransaction(String type, int accountId, long amount) {
    ExcerptAppender appender = queue.acquireAppender();
    appender.writeText(type + "(" + accountId + ", " + amount + ")");
  }

  private Lock getLockForAccount(int accountId) {
    return locks.computeIfAbsent(accountId, id -> new ReentrantLock());
  }
}
