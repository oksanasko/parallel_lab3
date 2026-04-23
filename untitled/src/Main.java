
import java.util.*;
import java.lang.management.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

class Account {
    public int id;
    public int balance;
    public ReentrantLock lock = new ReentrantLock();

    public Account(int id, int balance) {
        this.id = id;
        this.balance = balance;
    }

    public void deposit(int amount) {
        balance += amount;
    }

    public void withdraw(int amount) {
        balance -= amount;
    }
}

class Bank {
    public List<Account> accounts = new ArrayList<>();

    public Bank(int numAccounts) {
        for (int i = 0; i < numAccounts; i++) {
            int balance = ThreadLocalRandom.current().nextInt(1000, 5000);
            accounts.add(new Account(i, balance));
        }
    }

    public void transfer(Account from, Account to, int amount) {

        //  lock ordering -> no deadlock
        Account first = from.id < to.id ? from : to;
        Account second = from.id < to.id ? to : from;

        // another option
        // if (from.lock.tryLock(50, TimeUnit.MILLISECONDS)) { try{} }
        //

        // DEADLOCK-PRONE: no ordering
        //from.lock.lock();
        first.lock.lock();
        try {
            //to.lock.lock();
            second.lock.lock();
            try {
                // just race condition
                if (from.balance >= amount) {
                    from.balance -= amount;
                    to.balance += amount;
                } //end of just race condition
            } finally {
                //to.lock.unlock();
                second.lock.unlock();
            }
        } finally {
            //from.lock.unlock();
            first.lock.unlock();
        }
    }

    public int getTotalBalance() {
        int total = 0;
        for (Account acc : accounts) {
            total += acc.balance;
        }
        return total;
    }
}

class DeadlockDetector implements Runnable {
    @Override
    public void run() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();

        while (true) {
            long[] deadlockedThreads = bean.findDeadlockedThreads();

            if (deadlockedThreads != null) {
                ThreadInfo[] infos = bean.getThreadInfo(deadlockedThreads);

                for (ThreadInfo info : infos) {
                    System.out.println(info);
                }
                System.out.println("\nProgram will terminate.");
                System.exit(0);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
class TransferTask implements Runnable {
    private Bank bank;

    public TransferTask(Bank bank) {
        this.bank = bank;
    }

    @Override
    public void run() {
        Random rand = new Random();

        for (int i = 0; i < 100; i++) {
            int fromIndex = rand.nextInt(bank.accounts.size());
            int toIndex = rand.nextInt(bank.accounts.size());

            if (fromIndex == toIndex) continue;

            Account from = bank.accounts.get(fromIndex);
            Account to = bank.accounts.get(toIndex);

            int amount = rand.nextInt(100);

            bank.transfer(from, to, amount);
        }
    }
}

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int numAccounts = 100;
        int numThreads = 1000;

        Bank bank = new Bank(numAccounts);

        int initialTotal = bank.getTotalBalance();
        System.out.println("Initial total: " + initialTotal);

        // deadlock detector
        Thread monitor = new Thread(new DeadlockDetector());
        monitor.setDaemon(true);
        monitor.start();

        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            Thread t = new Thread(new TransferTask(bank));
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        int finalTotal = bank.getTotalBalance();
        System.out.println("Final total: " + finalTotal);
    }
}