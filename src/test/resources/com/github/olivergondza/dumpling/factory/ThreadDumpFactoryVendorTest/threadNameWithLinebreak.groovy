import java.util.concurrent.locks.LockSupport;

new Thread("thread" + System.getProperty("line.separator", "\n") + "name") {
    void run() {
        LockSupport.park();
    }
}.start();

LockSupport.park();
