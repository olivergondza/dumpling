import java.util.concurrent.locks.LockSupport;

for (;;) {
    LockSupport.parkNanos(1000000);
}
