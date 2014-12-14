import java.util.concurrent.locks.LockSupport;

def lock = new Object();

for (;;) {
    LockSupport.park(lock);
}
