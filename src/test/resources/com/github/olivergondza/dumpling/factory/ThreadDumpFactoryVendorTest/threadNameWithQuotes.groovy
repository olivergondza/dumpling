import java.util.concurrent.locks.LockSupport;

new Thread("a\"thread\"") {
    void run() {
        LockSupport.park();
    }
}.start();

LockSupport.park();
