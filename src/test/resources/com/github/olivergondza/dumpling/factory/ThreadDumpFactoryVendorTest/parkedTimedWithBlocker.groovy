import java.util.concurrent.locks.LockSupport;

def lock = new Object();

for (;;) {
    LockSupport.parkNanos(lock, 1000000);
}
